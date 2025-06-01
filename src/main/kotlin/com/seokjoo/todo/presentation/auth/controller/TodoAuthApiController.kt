package com.seokjoo.todo.presentation.auth.controller

import com.seokjoo.todo.common.jwt.getBearerToken
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.presentation.auth.dto.TodoAuthDeleteRequest
import com.seokjoo.todo.presentation.auth.dto.TodoAuthLoginRequest
import com.seokjoo.todo.presentation.auth.dto.TodoAuthLoginResponse
import com.seokjoo.todo.presentation.auth.dto.TodoAuthRefreshTokenRequest
import com.seokjoo.todo.presentation.auth.dto.TodoAuthRefreshTokenResponse
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Auth", description = "로그인, refresh token 갱신에 사용합니다")
@RestController
@RequestMapping("/api/v1/auth")
class TodoAuthApiController(
    private val authService: TodoAuthService,
) {
    @PostMapping("/signup")
    @ApiResponse(
        responseCode = "200",
        description = "회원 가입",
    )
    fun signup(@RequestBody @Valid request: TodoAuthLoginRequest): ResponseEntity<String> {
        authService.signUp(request.userId, request.password)
        return ResponseEntity.ok("회원가입 성공!")
    }

    @PostMapping("/login")
    fun login(@RequestBody @Valid request: TodoAuthLoginRequest): ResponseEntity<TodoAuthLoginResponse> {
        val result = authService.login(request.userId, request.password)
        return ResponseEntity.ok(
            TodoAuthLoginResponse(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken
            )
        )
    }

    @PostMapping("/refresh")
    fun refreshToken(
        httpRequest: HttpServletRequest,
        @RequestBody @Valid tokenRequest: TodoAuthRefreshTokenRequest,
    ): ResponseEntity<TodoAuthRefreshTokenResponse> {
        val token = httpRequest.getBearerToken()

        authService.checkTokenSignature(tokenRequest.userId, token)

        val newRefreshToken = authService.refreshToken(tokenRequest.userId)
        return ResponseEntity.ok(
            TodoAuthRefreshTokenResponse(
                refreshToken = newRefreshToken,
            )
        )
    }

    @DeleteMapping("/delete")
    fun deleteUser(
        @RequestBody @Valid request: TodoAuthDeleteRequest,
    ): ResponseEntity<String> {
        authService.delete(request.userId, request.password)
        return ResponseEntity.ok("삭제 성공")
    }
}
