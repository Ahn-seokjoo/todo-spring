package com.seokjoo.todo.domain.service.todo

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
data class TodoPageServiceResponseDTO(
    val isLast: Boolean = false,
    val responseList: List<TodoServiceResponseDTO> = emptyList(),
)
