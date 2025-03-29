package com.seokjoo.todo.domain.service.auth

data class TodoAuthServiceLoginResponse(
    val accessToken: String,
    val refreshToken: String,
)
