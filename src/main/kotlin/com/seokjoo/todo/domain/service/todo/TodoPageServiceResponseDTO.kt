package com.seokjoo.todo.domain.service.todo

data class TodoPageServiceResponseDTO(
    val isLast: Boolean,
    val responseList: List<TodoServiceResponseDTO>,
)
