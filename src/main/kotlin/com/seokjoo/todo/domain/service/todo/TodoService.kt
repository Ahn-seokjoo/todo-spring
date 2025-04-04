package com.seokjoo.todo.domain.service.todo

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.category.Category
import com.seokjoo.todo.domain.entity.todo.Todo
import com.seokjoo.todo.domain.repository.category.CategoryRepository
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import com.seokjoo.todo.domain.service.remove.TodoDeleteService
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TodoService(
    private val todoRepository: TodoRepository,
    private val categoryRepository: CategoryRepository,
    private val todoDeleteService: TodoDeleteService,
) {
    @Cacheable(cacheNames = ["todos"], key = "'todos'")
    fun getPagedTodos(pageServiceDTO: TodoPageServiceDTO): TodoPageServiceResponseDTO {
        val pageRequest =
            PageRequest.of(pageServiceDTO.pageNumber, pageServiceDTO.pageSize, Sort.by("updatedAt").ascending())
        val todoPage = todoRepository.findAllSlicedTodoOrderByUpdatedAt(pageRequest)
        val pageResult = todoRepository.getFetchJoinedTodoList(todos = todoPage.content)
        val todoPagedList = pageResult.map { todo -> TodoServiceResponseDTO.from(todo) }

        return TodoPageServiceResponseDTO(isLast = todoPage.isLast, responseList = todoPagedList)
    }

    @Cacheable(cacheNames = ["todos"], key = "todos")
    fun getTodoById(id: Long): TodoServiceResponseDTO {
        val result = todoRepository.findByIdOrNull(id) ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)
        return TodoServiceResponseDTO.from(result)
    }

    @Transactional
    @CacheEvict(value = ["todos"])
    fun createTodo(request: TodoServiceRequestDTO): TodoServiceResponseDTO {
        // 1. 저장하여 영속화 먼저
        val todo = Todo(todo = request.todo, isDone = request.isDone)
        todoRepository.save(todo)

        checkExistAndAddCategory(request, todo)
        return TodoServiceResponseDTO.from(todo)
    }

    @Transactional
    @CacheEvict(value = ["todos"])
    fun updateTodo(id: Long, request: TodoServiceRequestDTO): TodoServiceResponseDTO {
        val todo = todoRepository.findByIdOrNull(id) ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)

        todo.apply {
            this.todo = request.todo
            this.isDone = request.isDone
        }
        // 더티 체킹으로 save 할 필요 없지만 그냥 명시적으로 해줌
        todoRepository.save(todo)

        checkExistAndAddCategory(request, todo)
        return TodoServiceResponseDTO.from(todo)
    }

    @Transactional
    @CacheEvict(value = ["todos"])
    fun deleteTodo(id: Long) {
        val todo = todoRepository.findByIdOrNull(id) ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)

        todoDeleteService.deleteTodo(todo)
    }

    private fun checkExistAndAddCategory(
        request: TodoServiceRequestDTO,
        todo: Todo,
    ) {
        if (request.categoryNames.isNotEmpty()) {
            request.categoryNames.forEach { categoryName ->
                if (todo.hasCategory(categoryName).not()) {
                    val matchedCategory =
                        categoryRepository.findCategoryByName(categoryName)
                            ?: categoryRepository.save(Category(name = categoryName))
                    todo.addCategory(category = matchedCategory)
                }
            }
        }
    }
}
