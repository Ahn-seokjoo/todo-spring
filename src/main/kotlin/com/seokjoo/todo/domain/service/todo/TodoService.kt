package com.seokjoo.todo.domain.service.todo

import com.seokjoo.todo.domain.entity.todo.Todo
import com.seokjoo.todo.domain.repository.category.CategoryRepository
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import com.seokjoo.todo.presentation.category.dto.toEntity
import com.seokjoo.todo.presentation.todo.dto.request.TodoRequest
import com.seokjoo.todo.presentation.todo.dto.response.TodoResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrElse

@Service
@Transactional(readOnly = true)
class TodoService(
    private val todoRepository: TodoRepository,
    private val categoryRepository: CategoryRepository,
) {
    fun getAllTodos(): List<TodoServiceResponseDTO> {
        val todoList = todoRepository.findAll()
        return todoList.map { todo -> TodoServiceResponseDTO.from(todo) }
    }

    fun getTodoById(id: Long): TodoServiceResponseDTO {
        val result = todoRepository.findById(id).getOrElse { throw IllegalAccessError("bad") }
        return TodoServiceResponseDTO.from(result)
    }

    @Transactional
    fun createTodo(request: TodoServiceRequestDTO) {
        // 1. 저장하여 영속화 먼저
        val todo = Todo(todo = request.todo, isDone = request.isDone)
        todoRepository.save(todo)

        checkExistAndAddCategory(request, todo)
    }

    @Transactional
    fun updateTodo(id: Long, request: TodoServiceRequestDTO): TodoServiceResponseDTO {
        val todo = todoRepository.findById(id).getOrElse { throw IllegalArgumentException("id가 없음") }

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
        todoRepository.deleteById(id)
    }

    private fun checkExistAndAddCategory(
        request: TodoServiceRequestDTO,
        todo: Todo,
    ) {
        if (request.categories.isNotEmpty()) {
            request.categories.forEach { category ->
                val matchedCategory =
                    categoryRepository.findByName(category.name) ?: categoryRepository.save(category.toEntity())
                val isAlreadyNotExists = todo.todoCategories.any { it.category?.name == category.name }.not()
                if (isAlreadyNotExists) todo.addCategory(category = matchedCategory)
            }
        }
    }
}
