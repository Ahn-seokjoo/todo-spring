package com.seokjoo.todo.presentation.auth.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class TodoAuthSignUpRequest(
    @field:Size(min = 4, max = 30, message = "id는 4자 이상, 30자 이하입니다.")
    @field:Pattern(regexp = "^[a-zA-Z0-9_-]{4,30}\$", message = "영문, 숫자, _,- 포함입니다.")
    @field:NotBlank(message = "id는 필수 값입니다.")
    @field:JsonProperty("user_id")
    val userId: String,
    @field:NotBlank(message = "password는 필수 값입니다.")
    val password: String,
    @field:NotBlank(message = "todo 구매시에 확정받을 이메일을 입력하세요")
    @field:Size(max = 255, message = "email은 255자 이하여야 합니다.")
    val email: String,
)
