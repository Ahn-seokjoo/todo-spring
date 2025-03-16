package com.seokjoo.todo.domain.service.todo

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.todo.Todo
import com.seokjoo.todo.domain.entity.todocategory.TodoCategory
import com.seokjoo.todo.domain.repository.category.CategoryRepository
import com.seokjoo.todo.domain.repository.categorytodo.TodoCategoryRepository
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TodoService(
    private val todoRepository: TodoRepository,
    private val categoryRepository: CategoryRepository,
    private val todoCategoryRepository: TodoCategoryRepository,
) {
    fun getAllTodos(): List<TodoServiceResponseDTO> {
        val todoList = todoRepository.findAll()
        return todoList.map { todo -> TodoServiceResponseDTO.from(todo) }
    }

    fun getTodoById(id: Long): TodoServiceResponseDTO {
        val result = todoRepository.findByIdOrNull(id) ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)
        return TodoServiceResponseDTO.from(result)
    }

    @Transactional
    fun createTodo(request: TodoServiceRequestDTO): TodoServiceResponseDTO {
        // 1. 저장하여 영속화 먼저
        val todo = Todo(todo = request.todo, isDone = request.isDone)
        todoRepository.save(todo)

        checkExistAndAddCategory(request, todo)
        return TodoServiceResponseDTO.from(todo)
    }

    @Transactional
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
    fun deleteTodo(id: Long) {
        val todo = todoRepository.findByIdOrNull(id) ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)
        val todoCategory = todo.todoCategories
        todoRepository.deleteById(id)

        todoCategory
            .filter { it.category != null && isCategoryLinked(it) }
            .mapNotNull { it.category }
            .forEach { categoryRepository.delete(it) }
    }

    private fun isCategoryLinked(it: TodoCategory) =
        todoCategoryRepository.countByCategoryId(it.id) > 0

    private fun checkExistAndAddCategory(
        request: TodoServiceRequestDTO,
        todo: Todo,
    ) {
        if (request.categories.isNotEmpty()) {
            request.categories.forEach { category ->
                val matchedCategory =
                    categoryRepository.findByName(category.name) ?: categoryRepository.save(category)
                val isAlreadyNotExists = todo.todoCategories.any { it.category?.name == category.name }.not()
                if (isAlreadyNotExists) todo.addCategory(category = matchedCategory)
            }
        }
    }
}
