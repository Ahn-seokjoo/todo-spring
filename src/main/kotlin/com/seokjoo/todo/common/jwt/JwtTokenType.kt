package com.seokjoo.todo.common.jwt

enum class JwtTokenType {
    ACCESS, REFRESH;

    companion object {
        fun JwtTokenType.isRefreshToken(): Boolean {
            return this == JwtTokenType.REFRESH
        }
    }
}
