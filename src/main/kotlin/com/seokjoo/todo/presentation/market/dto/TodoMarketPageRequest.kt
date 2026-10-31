package com.seokjoo.todo.presentation.market.dto

import com.seokjoo.todo.domain.service.market.TodoMarketPageServiceDTO

data class TodoMarketPageRequest(
    val pageSize: Int,
    val cursor: Long,
)

fun TodoMarketPageRequest.toTodoMarketPageServiceDTO() = TodoMarketPageServiceDTO(
    pageSize = pageSize,
    cursor = cursor,
)
