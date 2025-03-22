package com.seokjoo.todo.presentation.todo.dto.response

data class TodoPageResponse(
    val isLast: Boolean = true,
    val todoList: List<TodoResponse> = listOf(),
)
