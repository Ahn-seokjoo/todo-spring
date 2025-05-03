package com.seokjoo.todo.domain.service.todo

data class TodoUpdateServiceRequestDTO(
    val todo: String,
    val isDone: Boolean?,
    val price: Long?,
    override val categoryNames: List<String> = emptyList(),
) : TodoServiceRequestDTO
