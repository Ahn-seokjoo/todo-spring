package com.seokjoo.todo.presentation.todo.controller

import com.seokjoo.todo.presentation.todo.dto.request.TodoRequest
import com.seokjoo.todo.presentation.todo.dto.response.TodoResponse
import com.seokjoo.todo.presentation.todo.service.TodoService
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class TodoApiController(
    private val todoService: TodoService,
) {

    @GetMapping("/todos")
    fun getAllTodos(): ResponseEntity<List<TodoResponse>> {
        return todoService.getAllTodos()
    }

    @GetMapping("/todos/{id}")
    fun getTodoById(@PathVariable id: Long): ResponseEntity<TodoResponse> {
        return todoService.getTodoById(id)
    }

    @PostMapping("/todos")
    fun createTodo(@RequestBody @Validated request: TodoRequest): ResponseEntity<String> {
        return todoService.createTodo(request = request)
    }

    @PatchMapping("/todos/{id}")
    fun updateTodo(@PathVariable id: Long, @RequestBody @Validated request: TodoRequest): ResponseEntity<TodoResponse> {
        return todoService.updateTodo(id = id, request = request)
    }

    @DeleteMapping("/todos/{id}")
    fun deleteTodo(@PathVariable id: Long): ResponseEntity<String> {
        return todoService.deleteTodo(id)
    }
}
