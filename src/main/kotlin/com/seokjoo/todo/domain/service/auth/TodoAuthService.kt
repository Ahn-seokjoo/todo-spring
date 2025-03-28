package com.seokjoo.todo.domain.service.auth

import com.seokjoo.todo.common.common.jwt.JwtProvider
import com.seokjoo.todo.common.common.jwt.JwtTokenType
import org.springframework.stereotype.Service

@Service
class TodoAuthService(
    private val jwtProvider: JwtProvider,
) {
    fun login(id: String, password: String): TodoAuthServiceLoginResponse {
        // TODO repository로 조회, 있으면

        val accessToken = jwtProvider.generateToken(id, JwtTokenType.ACCESS)
        val refreshToken = jwtProvider.generateToken(id, JwtTokenType.REFRESH)
        return TodoAuthServiceLoginResponse(accessToken, refreshToken)
    }

    fun refreshToken() {}
}
