package com.seokjoo.todo.presentation.todo.controller

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TodoApiController {

    @GetMapping("/api/v1/todos")
    fun getAllTodos(): String {
        return "ok"
    }

    @GetMapping("/api/v1/todos/{id}")
    fun getTodoById(@PathVariable id: Long): String {
        return "ok"
    }

    @PostMapping("/api/v1/todos")
    fun createTodo(): String {
        return "ok"
    }

    @PatchMapping("/api/v1/todos/{id}")
    fun updateTodo(@PathVariable id: Long): String {
        return "ok"
    }

    @DeleteMapping("/api/v1/todos/{id}")
    fun deleteTodo(@PathVariable id: Long): String {
        return "ok"
    }
}
