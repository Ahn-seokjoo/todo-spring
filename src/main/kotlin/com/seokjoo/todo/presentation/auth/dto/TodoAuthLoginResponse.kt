package com.seokjoo.todo.presentation.auth.dto

data class TodoAuthLoginResponse(
    val accessToken: String,
    val refreshToken: String,
)
