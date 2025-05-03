package com.seokjoo.todo.domain.service.todo

data class TodoCreateServiceRequestDTO(
    val todo: String,
    val isDone: Boolean = false,
    val price: Long = 0L,
    override val categoryNames: List<String> = emptyList(),
) : TodoServiceRequestDTO
