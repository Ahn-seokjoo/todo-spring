package com.seokjoo.todo.presentation.todo.controller

import com.seokjoo.todo.common.common.jwt.getBearerToken
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.presentation.todo.dto.request.TodoPageRequest
import com.seokjoo.todo.presentation.todo.dto.request.TodoRequest
import com.seokjoo.todo.presentation.todo.dto.request.TodoUpdateRequest
import com.seokjoo.todo.presentation.todo.dto.request.toCreateRequest
import com.seokjoo.todo.presentation.todo.dto.request.toPageServiceDTO
import com.seokjoo.todo.presentation.todo.dto.request.toUpdateRequest
import com.seokjoo.todo.presentation.todo.dto.response.TodoPageResponse
import com.seokjoo.todo.presentation.todo.dto.response.TodoResponse
import com.seokjoo.todo.presentation.todo.dto.response.toResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
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
import java.net.URI

/**
 * 1. 사용자는 카테고리 없이 투두를 만들 수 있다.
 * 2. 사용자는 카테고리를 1개 이상으로 저장할 수 있다.
 * 3. 이때, 기존에 없던 카테고리라면 카테고리를 저장하고 추가한다
 * 4. 기존에 있던 카테고리라면 새로 저장하지 않고 투두에 추가한다
 * 5. 기존에 카테고리가 있고, 업데이트를 해준다면 업데이트 되는 부분만 추가해준다
 * 6. 이때 카테고리는 중복이 없어야 한다.
 * 7. 각 Todo는 Owner 정보를 가지고 있고, 자신의 Todo만 볼 수 있다.
 */
@Tag(name = "Todo", description = "Todo 조회, 삭제, 수정 API")
@RestController
@RequestMapping("/api/v1")
class TodoApiController(
    private val todoService: TodoService,
    private val authService: TodoAuthService,
) {

    @GetMapping("/todos")
    @Operation(summary = "사용자 todo를 페이지네이션을 통해 리턴", description = "사용자가 등록한 todo 와 카테고리 정보를 n개씩 가져옵니다. (default = 20)")
    fun getPagedTodos(
        servletRequest: HttpServletRequest,
        @RequestBody todoPageRequest: TodoPageRequest = TodoPageRequest(),
    ): ResponseEntity<TodoPageResponse> {
        val user = authService.findUser(servletRequest.getBearerToken())
        val pagedDto = todoPageRequest.toPageServiceDTO()
        val pagedResponse = todoService.getPagedTodos(userId = user.userId, pageServiceDTO = pagedDto)
        return ResponseEntity.ok(
            TodoPageResponse(
                isLast = pagedResponse.isLast,
                todoList = pagedResponse.responseList.map { it.toResponse() }
            )
        )
    }

    @GetMapping("/todos/{id}")
    @Operation(summary = "특정 id todo 조회", description = "id를 이용해 todo 한개를 조회합니다.")
    fun getTodoById(
        servletRequest: HttpServletRequest,
        @PathVariable id: Long,
    ): ResponseEntity<TodoResponse> {
        val user = authService.findUser(servletRequest.getBearerToken())
        val todo = todoService.getTodoById(id, user.userId).toResponse()
        return ResponseEntity.ok(todo)
    }

    @PostMapping("/todos")
    @Operation(summary = "todo 추가", description = "todo 한개를 추가합니다.")
    @ApiResponse(
        responseCode = "201",
        description = "todo 추가 성공",
        content = [Content(mediaType = "text/plain", schema = Schema(example = "ok"))]
    )
    fun createTodo(
        servletRequest: HttpServletRequest,
        @RequestBody @Validated request: TodoRequest,
    ): ResponseEntity<TodoResponse> {
        val user = authService.findUser(servletRequest.getBearerToken())
        val todo = todoService.createTodo(request = request.toCreateRequest(), user).toResponse()
        return ResponseEntity.created(URI.create("/todos/${todo.id}")).body(todo)
    }

    @PatchMapping("/todos/{id}")
    @Operation(summary = "특정 id todo 업데이트", description = "id를 이용해 todo 한개를 업데이트 합니다.")
    fun updateTodo(
        servletRequest: HttpServletRequest,
        @PathVariable id: Long,
        @RequestBody @Validated request: TodoUpdateRequest,
    ): ResponseEntity<TodoResponse> {
        val user = authService.findUser(servletRequest.getBearerToken())
        val todo =
            todoService.updateTodo(id = id, userId = user.userId, request = request.toUpdateRequest()).toResponse()
        return ResponseEntity.ok(todo)
    }

    @DeleteMapping("/todos/{id}")
    @Operation(summary = "특정 id todo 삭제", description = "id를 이용해 todo 한개를 삭제합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "삭제 성공",
        content = [Content(mediaType = "text/plain", schema = Schema(example = "ok"))]
    )
    fun deleteTodo(servletRequest: HttpServletRequest, @PathVariable id: Long): ResponseEntity<String> {
        val user = authService.findUser(servletRequest.getBearerToken())
        todoService.deleteTodo(id = id, userId = user.userId)
        return ResponseEntity.noContent().build()
    }
}
