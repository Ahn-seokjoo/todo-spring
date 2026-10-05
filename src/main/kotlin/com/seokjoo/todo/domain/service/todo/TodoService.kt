package com.seokjoo.todo.domain.service.todo

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.todo.Todo
import com.seokjoo.todo.domain.entity.todo.TodoStatus
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import com.seokjoo.todo.domain.service.category.CategoryService
import com.seokjoo.todo.domain.service.remove.TodoDeleteService
import com.seokjoo.todo.domain.service.todo.event.TodoCacheEvictEvent
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TodoService(
    private val todoRepository: TodoRepository,
    private val todoDeleteService: TodoDeleteService,
    private val categoryService: CategoryService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    // 현재 캐시매니저가 1개라서 안써도 되지만 공부용으로 명시함
    @Cacheable(
        cacheNames = ["todos"],
        unless = "#result.responseList.isEmpty()",
        key = "#userId + ':page:' + #pageServiceDTO.pageNumber + ':size:' + #pageServiceDTO.pageSize",
        cacheManager = "todoCacheManager"
    )
    fun getPagedTodos(userId: String, pageServiceDTO: TodoPageServiceDTO): TodoPageServiceResponseDTO {
        val pageRequest =
            PageRequest.of(pageServiceDTO.pageNumber, pageServiceDTO.pageSize, Sort.by("updatedAt").ascending())
        val todoPage = todoRepository.findAllSlicedTodoOrderByUpdatedAt(pageable = pageRequest, ownerId = userId)
        val pageResult = todoRepository.getFetchJoinedTodoList(todos = todoPage.content)
        val todoPagedList = pageResult.map { todo -> TodoServiceResponseDTO.from(todo) }

        return TodoPageServiceResponseDTO(isLast = todoPage.isLast, responseList = todoPagedList)
    }

    @Cacheable(cacheNames = ["todo"], key = "#todoId")
    fun getTodoById(todoId: Long): TodoServiceResponseDTO {
        val todo = todoRepository.findByIdOrNull(todoId) ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)

        return TodoServiceResponseDTO.from(todo)
    }

    @Transactional(readOnly = true)
    fun getTodoByIdForUpdate(todoId: Long): TodoServiceResponseDTO {
        val todo = todoRepository.findByIdOrNull(todoId) ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)

        return TodoServiceResponseDTO.from(todo)
    }

    @Transactional
    fun createTodo(request: TodoCreateServiceRequestDTO, owner: User): TodoServiceResponseDTO {
        val todo = Todo(todo = request.todo, isDone = request.isDone, owner = owner, price = request.price)
        todoRepository.save(todo)

        checkExistAndAddCategory(request, todo)
        eventPublisher.publishEvent(TodoCacheEvictEvent(isEvictAll = true))
        return TodoServiceResponseDTO.from(todo)
    }

    @Transactional
    fun updateTodo(todoId: Long, userId: String, request: TodoUpdateServiceRequestDTO): TodoServiceResponseDTO {
        val todo = todoRepository.findByIdOrNull(todoId) ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)
        check(isOwner(todo.owner.userId, userId)) {
            throw TodoException.of(TodoExceptionType.UNAUTHORIZED_TODO_ACCESS)
        }
        check(todo.status == TodoStatus.AVAILABLE) {
            throw TodoException.of(TodoExceptionType.PENDING)
        }
        todo.todoUpdateApply(request)
        // 더티 체킹으로 save 할 필요 없지만 그냥 명시적으로 해줌
        todoRepository.save(todo)

        replaceCategoryIfNotEmpty(request, todo)
        eventPublisher.publishEvent(TodoCacheEvictEvent(isEvictAll = true, todoIds = listOf(todoId)))
        return TodoServiceResponseDTO.from(todo)
    }

    @Transactional
    fun deleteTodo(todoId: Long, userId: String) {
        val todo = todoRepository.findByIdOrNull(todoId) ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)
        check(isOwner(todo.owner.userId, userId)) {
            throw TodoException.of(TodoExceptionType.UNAUTHORIZED_TODO_ACCESS)
        }
        check(todo.status == TodoStatus.AVAILABLE) {
            throw TodoException.of(TodoExceptionType.PENDING)
        }

        eventPublisher.publishEvent(TodoCacheEvictEvent(isEvictAll = true, todoIds = listOf(todoId)))
        todoDeleteService.deleteTodo(todo)
    }

    @Transactional
    fun updateOwner(todoId: Long, owner: User): TodoServiceResponseDTO {
        val todo = todoRepository.findByIdOrNull(todoId) ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)
        check(todo.status == TodoStatus.AVAILABLE) {
            throw TodoException.of(TodoExceptionType.PENDING)
        }
        todo.updateOwner(owner)
        todoRepository.save(todo)

        eventPublisher.publishEvent(TodoCacheEvictEvent(isEvictAll = true, todoIds = listOf(todoId)))
        return TodoServiceResponseDTO.from(todo)
    }

    @Transactional(readOnly = true)
    fun getTodoCounts(owner: User): Long {
        val count = todoRepository.countByOwnerId(ownerId = owner.userId)
        return count
    }

    @Transactional
    fun changeStatus(todoId: Long, status: TodoStatus) {
        val successCount = when (status) {
            TodoStatus.PENDING_APPROVAL -> todoRepository.updateTodoStatusPendingIfAvailable(todoId)
            TodoStatus.AVAILABLE -> todoRepository.updateTodoStatusAvailableIfPending(todoId)
        }
        if (successCount == 0) throw TodoException.of(TodoExceptionType.NOT_PENDING)
        eventPublisher.publishEvent(TodoCacheEvictEvent(isEvictAll = true, todoIds = listOf(todoId)))
    }

    private fun checkExistAndAddCategory(
        request: TodoServiceRequestDTO,
        todo: Todo,
    ) {
        request.categoryNames.forEach { categoryName ->
            if (todo.hasCategory(categoryName).not()) {
                val category = categoryService.getOrCreateCategory(categoryName)
                todo.addCategory(category = category)
            }
        }
    }

    private fun replaceCategoryIfNotEmpty(
        request: TodoServiceRequestDTO,
        todo: Todo,
    ) {
        if (request.categoryNames.isEmpty()) return
        todo.todoCategories.clear()
        request.categoryNames.forEach { categoryName ->
            val category = categoryService.getOrCreateCategory(categoryName)
            todo.addCategory(category = category)
        }
    }

    private fun isOwner(todoOwnerId: String, userId: String) = todoOwnerId == userId
}
