package com.seokjoo.todo.presentation.todo.dto.request

import com.seokjoo.todo.domain.entity.category.Category
import com.seokjoo.todo.domain.service.todo.TodoServiceRequestDTO
import jakarta.validation.constraints.NotBlank

data class TodoRequest(
    @field:NotBlank(message = "todo는 필수입니다")
    val todo: String,
    val isDone: Boolean = false,
    val categories: List<Category> = listOf(),
)

fun TodoRequest.toTodoServiceRequest() = TodoServiceRequestDTO(
    todo = todo,
    isDone = isDone,
    categories = categories,
)
