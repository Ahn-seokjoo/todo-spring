package com.seokjoo.todo.domain.service.todo

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.todo.Todo
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import com.seokjoo.todo.domain.service.category.CategoryService
import com.seokjoo.todo.domain.service.remove.TodoDeleteService
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
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

    @Transactional
    @CacheEvict(value = ["todos"], allEntries = true)
    fun createTodo(request: TodoCreateServiceRequestDTO, owner: User): TodoServiceResponseDTO {
        val todo = Todo(todo = request.todo, isDone = request.isDone, owner = owner, price = request.price)
        todoRepository.save(todo)

        checkExistAndAddCategory(request, todo)
        return TodoServiceResponseDTO.from(todo)
    }

    @Transactional
    @CacheEvict(value = ["todos"], allEntries = true)
    @CachePut(cacheNames = ["todo"], key = "#todoId")
    fun updateTodo(todoId: Long, userId: String, request: TodoUpdateServiceRequestDTO): TodoServiceResponseDTO {
        val todo = todoRepository.findByIdOrNull(todoId) ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)
        if (isNotOwner(todo.owner.userId, userId)) throw TodoException.of(TodoExceptionType.UNAUTHORIZED_TODO_ACCESS)
        todo.todoUpdateApply(request)
        // 더티 체킹으로 save 할 필요 없지만 그냥 명시적으로 해줌
        todoRepository.save(todo)

        replaceCategoryIfNotEmpty(request, todo)
        return TodoServiceResponseDTO.from(todo)
    }

    @Transactional
    @Caching(
        evict = [
            CacheEvict(value = ["todos"], allEntries = true),
            CacheEvict(value = ["todo"], key = "#todoId"),
        ]
    )
    fun deleteTodo(todoId: Long, userId: String) {
        val todo = todoRepository.findByIdOrNull(todoId) ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)
        if (isNotOwner(todo.owner.userId, userId)) throw TodoException.of(TodoExceptionType.UNAUTHORIZED_TODO_ACCESS)

        todoDeleteService.deleteTodo(todo)
    }

    @Transactional
    @CachePut(cacheNames = ["todo"], key = "#todoId")
    @CacheEvict(value = ["todos"], allEntries = true)
    fun updateOwner(todoId: Long, owner: User): TodoServiceResponseDTO {
        val todo = todoRepository.findByIdOrNull(todoId) ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)
        todo.updateOwner(owner)
        todoRepository.save(todo)

        return TodoServiceResponseDTO.from(todo)
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

    private fun isNotOwner(todoOwnerId: String, userId: String) = todoOwnerId != userId
}
