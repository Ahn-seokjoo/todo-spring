package com.seokjoo.todo.presentation.todo.dto.request

import com.seokjoo.todo.domain.service.todo.TodoPageServiceDTO

data class TodoPageRequest(
    val pageNumber: Int = 0,
    val pageSize: Int = 20,
)

fun TodoPageRequest.toPageServiceDTO() = TodoPageServiceDTO(
    pageNumber = pageNumber,
    pageSize = pageSize,
)
