package com.seokjoo.todo.common.common.jwt

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthFilter(
    private val jwtProvider: JwtProvider,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val excludePatterns = listOf("/api/v1/auth/login", "/api/v1/auth/signup")
        return excludePatterns.any { request.requestURI.startsWith(it) }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = getToken(request) ?: throw TodoException.of(TodoExceptionType.AUTH_REFRESH_TOKEN_BAD_REQUEST)

        val isNotValid = jwtProvider.validateToken(token).not()
        if (isNotValid) throw throw TodoException.of(TodoExceptionType.AUTH_REFRESH_TOKEN_NOT_VALID)

        filterChain.doFilter(request, response)
    }

    private fun getToken(request: HttpServletRequest) = request.getHeader("Authorization")?.takeIf {
        it.startsWith("Bearer ")
    }?.substring(7)
}
