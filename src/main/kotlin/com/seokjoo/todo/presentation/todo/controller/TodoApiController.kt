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

/**
 * 요구사항 다시 정해보기
 * 1. 사용자는 카테고리 없이 투두를 만들 수 있다.
 * 2. 사용자는 카테고리를 1개 이상으로 저장할 수 있다.
 * 3. 이때, 기존에 없던 카테고리라면 카테고리를 저장하고 추가한다
 * 4. 기존에 있던 카테고리라면 새로 저장하지 않고 투두에 추가한다
 * 5. 기존에 카테고리가 있고, 업데이트를 해준다면 업데이트 되는 부분만 추가해준다
 * 6. 이때 카테고리는 중복이 없어야 한다.
 */
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
