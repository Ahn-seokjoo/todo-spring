package com.seokjoo.todo.domain.service.todo

data class TodoPageServiceResponseDTO(
    val isLast: Boolean = true,
    val responseList: List<TodoServiceResponseDTO> = emptyList(),
)
