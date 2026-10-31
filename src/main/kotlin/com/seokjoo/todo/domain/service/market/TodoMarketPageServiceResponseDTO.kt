package com.seokjoo.todo.domain.service.market

import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO

data class TodoMarketPageServiceResponseDTO(
    val nextCursor: Long? = null,
    val responseList: List<TodoServiceResponseDTO> = emptyList(),
)
