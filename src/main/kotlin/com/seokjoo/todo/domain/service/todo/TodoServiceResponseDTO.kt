package com.seokjoo.todo.domain.service.todo

import com.seokjoo.todo.domain.entity.todo.Todo

data class TodoServiceResponseDTO(
    val id: Long?,
    val todo: String,
    val isDone: Boolean,
    val categories: List<String>,
) {
    companion object {
        fun from(todo: Todo) = TodoServiceResponseDTO(
            id = todo.id,
            todo = todo.todo,
            isDone = todo.isDone,
            categories = todo.todoCategories.map { it.category?.name.orEmpty() },
        )
    }
}
