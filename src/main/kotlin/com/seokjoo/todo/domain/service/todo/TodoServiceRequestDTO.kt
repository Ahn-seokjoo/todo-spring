package com.seokjoo.todo.domain.service.todo

import com.seokjoo.todo.domain.entity.category.Category

data class TodoServiceRequestDTO(
    val todo: String,
    val isDone: Boolean = false,
    val categories: List<Category> = listOf(),
)
