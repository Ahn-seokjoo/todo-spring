package com.seokjoo.todo.presentation.todo.dto.response

import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import io.swagger.v3.oas.annotations.media.Schema

data class TodoResponse(
    @Schema(description = "todo", example = "백엔드 공부하기", required = true)
    val id: Long,
    val todo: String,
    @Schema(required = true)
    val isDone: Boolean,
    @Schema(description = "카테고리 목록", examples = ["horror", "comedy"])
    val categories: List<String>,
)

fun TodoServiceResponseDTO.toResponse() = TodoResponse(
    id = id,
    todo = todo,
    isDone = isDone,
    categories = categories.map { it },
)
