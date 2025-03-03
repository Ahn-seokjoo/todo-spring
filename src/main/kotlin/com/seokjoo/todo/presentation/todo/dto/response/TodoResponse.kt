package com.seokjoo.todo.presentation.todo.dto.response

import com.seokjoo.todo.domain.entity.Todo

data class TodoResponse(
    val id: Long?,
    val todo: String,
    val isDone: Boolean,
    val categories: List<String>,
) {
    companion object {
        fun from(todo: Todo) = TodoResponse(
            id = todo.id,
            todo = todo.todo,
            isDone = todo.isDone,
            categories = todo.todoCategories.map { it.category?.name.orEmpty() },
        )
    }
}
