package com.seokjoo.todo.domain.service.todo

data class TodoServiceRequestDTO(
    val todo: String,
    val isDone: Boolean = false,
    val categoryNames: List<String> = listOf(),
)
