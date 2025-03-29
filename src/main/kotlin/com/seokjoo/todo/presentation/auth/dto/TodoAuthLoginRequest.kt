package com.seokjoo.todo.presentation.auth.dto

import jakarta.validation.constraints.NotBlank

data class TodoAuthLoginRequest(
    @field:NotBlank(message = "id는 필수 값입니다.")
    val userId: String,
    @field:NotBlank(message = "password는 필수 값입니다.")
    val password: String,
)
