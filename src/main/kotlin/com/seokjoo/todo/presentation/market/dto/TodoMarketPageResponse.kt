package com.seokjoo.todo.presentation.market.dto

import com.seokjoo.todo.presentation.todo.dto.response.TodoResponse

data class TodoMarketPageResponse(
    val nextCursor: Long? = null,
    val todoList: List<TodoResponse> = listOf(),
)
