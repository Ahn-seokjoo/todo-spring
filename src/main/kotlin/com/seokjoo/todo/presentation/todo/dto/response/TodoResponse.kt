package com.seokjoo.todo.presentation.todo.dto.response

import com.seokjoo.todo.domain.entity.todo.Todo
import io.swagger.v3.oas.annotations.media.Schema

data class TodoResponse(
    val id: Long?,
    @Schema(description = "todo", example = "백엔드 공부하기", required = true)
    val todo: String,
    @Schema(required = true)
    val isDone: Boolean,
    @Schema(description = "카테고리 목록", examples = ["horror", "comedy"])
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
