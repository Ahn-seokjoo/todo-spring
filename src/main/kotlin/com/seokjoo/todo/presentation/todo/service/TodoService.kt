package com.seokjoo.todo.presentation.todo.service

import com.seokjoo.todo.domain.entity.Todo
import com.seokjoo.todo.domain.repository.CategoryRepository
import com.seokjoo.todo.domain.repository.TodoRepository
import com.seokjoo.todo.presentation.todo.dto.request.TodoRequest
import com.seokjoo.todo.presentation.todo.dto.response.TodoResponse
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrElse

@Service
class TodoService(
    private val todoRepository: TodoRepository,
    private val categoryRepository: CategoryRepository,
) {
    @Transactional(readOnly = true)
    fun getAllTodos(): ResponseEntity<List<TodoResponse>> {
        val todoList = todoRepository.findAll()
        return ResponseEntity.ok(todoList.map { todo -> TodoResponse.from(todo) })
    }

    @Transactional(readOnly = true)
    fun getTodoById(id: Long): ResponseEntity<TodoResponse> {
        val result = todoRepository.findById(id).getOrElse { throw IllegalAccessError("bad") }
        return ResponseEntity.ok(TodoResponse.from(result))
    }

    @Transactional
    fun createTodo(request: TodoRequest): ResponseEntity<String> {
        val todo = Todo(todo = request.todo, isDone = request.isDone).apply {
            checkAndAddCategory(request)
        }
        todoRepository.save(todo)
        return ResponseEntity.ok("ok")
    }

    // 더티 체킹으로 save 할 필요 없음
    @Transactional
    fun updateTodo(id: Long, request: TodoRequest): ResponseEntity<TodoResponse> {
        val todo = todoRepository.findById(id).getOrElse { throw IllegalArgumentException("id가 없음") }

        todo.apply {
            this.todo = request.todo
            this.isDone = request.isDone

            checkAndAddCategory(request)
        }
        return ResponseEntity.ok(TodoResponse.from(todo))
    }

    @Transactional
    fun deleteTodo(id: Long): ResponseEntity<String> {
        todoRepository.deleteById(id)

        return ResponseEntity.ok("삭제 성공")
    }

    private fun Todo.checkAndAddCategory(request: TodoRequest) {
        if (request.categories.isNotEmpty()) {
            request.categories.forEach { category ->
                val matchedCategory = categoryRepository.findByName(category.name)
                val realCategory = matchedCategory ?: categoryRepository.save(category)
                addCategory(category = realCategory, isNotExist = matchedCategory == null)
            }
        }
    }
}
