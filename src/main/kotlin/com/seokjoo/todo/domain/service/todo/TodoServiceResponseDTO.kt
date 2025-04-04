package com.seokjoo.todo.domain.service.todo

import com.fasterxml.jackson.annotation.JsonProperty
import com.seokjoo.todo.domain.entity.todo.Todo

data class TodoServiceResponseDTO(
    val id: Long = 0L,
    val todo: String = "",
    @JsonProperty("isDone") // is가 붙으면 is를 빼버리고 내부에 저장해버림;;
    val isDone: Boolean = false,
    val categories: List<String> = emptyList(),
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
