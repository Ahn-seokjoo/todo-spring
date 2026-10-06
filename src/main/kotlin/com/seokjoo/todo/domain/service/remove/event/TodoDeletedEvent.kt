package com.seokjoo.todo.domain.service.remove.event

data class TodoDeletedEvent(
    val categoryIds: List<Long>,
)
