package com.seokjoo.todo.presentation.todo.dto.request

import com.seokjoo.todo.domain.entity.Category
import jakarta.validation.constraints.NotBlank

data class TodoRequest(
    @field:NotBlank(message = "todo는 필수입니다")
    val todo: String,
    val isDone: Boolean = false,
    val categories: List<Category> = listOf(),
)
