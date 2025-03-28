package com.seokjoo.todo.presentation.auth.controller

import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.presentation.auth.dto.TodoAuthLoginRequest
import com.seokjoo.todo.presentation.auth.dto.TodoAuthLoginResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Auth", description = "로그인, refresh token 갱신에 사용합니다")
@RestController("/api/v1/auth")
class TodoAuthApiController(
    private val authService: TodoAuthService,
) {

    @PostMapping("/login")
    fun login(@RequestBody @Validated request: TodoAuthLoginRequest): ResponseEntity<TodoAuthLoginResponse> {
        val result = authService.login(request.id, request.password)
        return ResponseEntity.ok(
            TodoAuthLoginResponse(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken
            )
        )
    }

    @PostMapping("/refresh")
    fun refreshToken() {
        authService.refreshToken()
    }
}
