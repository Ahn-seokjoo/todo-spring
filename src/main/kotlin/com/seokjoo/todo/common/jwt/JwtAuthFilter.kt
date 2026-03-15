package com.seokjoo.todo.common.jwt

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.common.jwt.JwtTokenType.Companion.isRefreshToken
import com.seokjoo.todo.presentation.error.ApiErrorResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthFilter(
    private val jwtProvider: JwtProvider,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
    /**
     * "/api/v1/auth/refresh" 는 controller 쪽에서 검증을 따로 하기 때문에 filter 해주지 않았습니다
     */
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val excludePatterns =
            listOf(
                "/api/v1/auth/login",
                "/api/v1/auth/signup",
                "/api/v1/auth/refresh",
                "/api/v1/auth/delete",
                "/swagger-ui",
                "/v3/api-docs",
            )
        return excludePatterns.any { request.requestURI.startsWith(it) }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val token = request.getBearerToken()

            if (jwtProvider.getTokenType(token).isRefreshToken()) {
                throw TodoException.of(TodoExceptionType.AUTH_INVALID_TOKEN_TYPE)
            }
            val isNotValid = jwtProvider.validateToken(token).not()
            if (isNotValid) throw TodoException.of(TodoExceptionType.AUTH_REFRESH_TOKEN_NOT_VALID)

            filterChain.doFilter(request, response)
        } catch (e: TodoException) {
            handleException(response, e)
        }
    }

    private fun handleException(response: HttpServletResponse, exception: TodoException) {
        val errorResponse = ApiErrorResponse(
            errorCode = exception.errorCode,
            message = exception.message,
        )
        response.apply {
            status = exception.httpStatusCode
            contentType = "application/json"
            characterEncoding = "UTF-8"
            writer.write(objectMapper.writeValueAsString(errorResponse))
        }
    }
}
