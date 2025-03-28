package com.seokjoo.todo.domain.service.auth

import com.seokjoo.todo.common.common.jwt.JwtProvider
import com.seokjoo.todo.common.common.jwt.JwtTokenType
import org.springframework.stereotype.Service

@Service
class TodoAuthService(
    private val jwtProvider: JwtProvider,
) {
    fun login(userId: String, password: String): TodoAuthServiceLoginResponse {
        // TODO repository로 조회, 있으면

        val accessToken = jwtProvider.generateToken(userId, JwtTokenType.ACCESS)
        val refreshToken = jwtProvider.generateToken(userId, JwtTokenType.REFRESH)
        return TodoAuthServiceLoginResponse(accessToken, refreshToken)
    }

    fun refreshToken() {}
}
