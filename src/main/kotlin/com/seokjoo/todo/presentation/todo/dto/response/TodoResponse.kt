package com.seokjoo.todo.presentation.todo.dto.response

import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO

data class TodoResponse(
    val id: Long,
    val todo: String,
    val isDone: Boolean,
    val categories: List<String>,
)

fun TodoServiceResponseDTO.toResponse() = TodoResponse(
    id = id,
    todo = todo,
    isDone = isDone,
    categories = categories.map { it },
)
