package com.seokjoo.todo.domain.service.todo

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.common.redisson.RedisUtils
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
    private val redisUtils: RedisUtils,
) {
    // 현재 캐시매니저가 1개라서 안써도 되지만 공부용으로 명시함
    @Cacheable(
        cacheNames = ["todos"],
        unless = "#result.responseList.isEmpty()",
        key = "'todos:page:' + #pageServiceDTO.pageNumber + ':size:' + #pageServiceDTO.pageSize",
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

    @Cacheable(cacheNames = ["todo"], key = "#id")
    fun getTodoById(id: Long, userId: String): TodoServiceResponseDTO {
        val todo = todoRepository.findByIdOrNull(id) ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)
        if (isMe(todo.owner.userId, userId)) throw TodoException.of(TodoExceptionType.UNAUTHORIZED_TODO_ACCESS)
        return TodoServiceResponseDTO.from(todo)
    }

    @Transactional
    @CacheEvict(value = ["todos"])
    fun createTodo(request: TodoCreateServiceRequestDTO, owner: User): TodoServiceResponseDTO {
        val todo = Todo(todo = request.todo, isDone = request.isDone, owner = owner, price = request.price)
        todoRepository.save(todo)

        request.categoryNames.map {
            redisUtils.tryLock(it) {
                checkExistAndAddCategory(request, todo)
            }
        }
        return TodoServiceResponseDTO.from(todo)
    }

    @Transactional
    @CacheEvict(value = ["todos"])
    @CachePut(cacheNames = ["todo"], key = "#id")
    fun updateTodo(id: Long, userId: String, request: TodoUpdateServiceRequestDTO): TodoServiceResponseDTO {
        val todo = todoRepository.findByIdOrNull(id) ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)
        if (isMe(todo.owner.userId, userId)) throw TodoException.of(TodoExceptionType.UNAUTHORIZED_TODO_ACCESS)
        todo.todoUpdateApply(request)
        // 더티 체킹으로 save 할 필요 없지만 그냥 명시적으로 해줌
        todoRepository.save(todo)

        checkExistAndAddCategory(request, todo)
        return TodoServiceResponseDTO.from(todo)
    }

    @Transactional
    @Caching(
        evict = [
            CacheEvict(value = ["todos"]),
            CacheEvict(value = ["todo"], key = "#id"),
        ]
    )
    fun deleteTodo(id: Long, userId: String) {
        val todo = todoRepository.findByIdOrNull(id) ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)
        if (isMe(todo.owner.userId, userId)) throw TodoException.of(TodoExceptionType.UNAUTHORIZED_TODO_ACCESS)

        todoDeleteService.deleteTodo(todo)
    }

    private fun checkExistAndAddCategory(
        request: TodoServiceRequestDTO,
        todo: Todo,
    ) {
        if (request.categoryNames.isNotEmpty()) {
            request.categoryNames.forEach { categoryName ->
                if (todo.hasCategory(categoryName).not()) {
                    val matchedCategory = categoryService.getOrCreateCategory(categoryName)
                    todo.addCategory(category = matchedCategory)
                }
            }
        }
    }

    private fun isMe(todoOwnerId: String, userId: String) = todoOwnerId != userId
}
