package com.seokjoo.todo.presentation.todo.controller

import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.presentation.todo.dto.request.TodoRequest
import com.seokjoo.todo.presentation.todo.dto.request.toTodoServiceRequest
import com.seokjoo.todo.presentation.todo.dto.response.TodoResponse
import com.seokjoo.todo.presentation.todo.dto.response.toResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "Todo", description = "Todo 조회, 삭제, 수정 API")
@RestController
@RequestMapping("/api/v1")
class TodoApiController(
    private val todoService: TodoService,
) {

    @GetMapping("/todos")
    @Operation(summary = "사용자 모든 todo 조회", description = "사용자가 등록한 모든 todo 와 카테고리 정보를 가져옵니다.")
    fun getAllTodos(): ResponseEntity<List<TodoResponse>> {
        val allTodos = todoService.getAllTodos().map { it.toResponse() }
        return ResponseEntity.ok(allTodos)
    }

    @GetMapping("/todos/{id}")
    @Operation(summary = "특정 id todo 조회", description = "id를 이용해 todo 한개를 조회합니다.")
    fun getTodoById(@PathVariable id: Long): ResponseEntity<TodoResponse> {
        val todo = todoService.getTodoById(id).toResponse()
        return ResponseEntity.ok(todo)
    }

    @PostMapping("/todos")
    @Operation(summary = "todo 추가", description = "todo 한개를 추가합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "todo 추가 성공",
        content = [Content(mediaType = "text/plain", schema = Schema(example = "ok"))]
    )
    fun createTodo(@RequestBody @Validated request: TodoRequest): ResponseEntity<String> {
        todoService.createTodo(request = request.toTodoServiceRequest())
        return ResponseEntity.ok("ok")
    }

    @PatchMapping("/todos/{id}")
    @Operation(summary = "특정 id todo 업데이트", description = "id를 이용해 todo 한개를 업데이트 합니다.")
    fun updateTodo(@PathVariable id: Long, @RequestBody @Validated request: TodoRequest): ResponseEntity<TodoResponse> {
        val todo = todoService.updateTodo(id = id, request = request.toTodoServiceRequest()).toResponse()
        return ResponseEntity.ok(todo)
    }

    @DeleteMapping("/todos/{id}")
    @Operation(summary = "특정 id todo 삭제", description = "id를 이용해 todo 한개를 삭제합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "삭제 성공",
        content = [Content(mediaType = "text/plain", schema = Schema(example = "ok"))]
    )
    fun deleteTodo(@PathVariable id: Long): ResponseEntity<String> {
        todoService.deleteTodo(id)
        return ResponseEntity.ok("ok")
    }
}
