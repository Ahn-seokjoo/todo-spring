package com.seokjoo.todo.presentation.auth.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class TodoAuthLoginRequest(
    @field:NotBlank(message = "id는 필수 값입니다.")
    @field:JsonProperty("user_id")
    val userId: String,
    @field:NotBlank(message = "password는 필수 값입니다.")
    val password: String,
    @field:NotBlank(message = "todo 구매시에 확정받을 이메일을 입력하세요")
    val email: String,
)
