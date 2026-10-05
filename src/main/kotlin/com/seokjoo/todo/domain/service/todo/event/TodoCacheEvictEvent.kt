package com.seokjoo.todo.domain.service.todo.event

data class TodoCacheEvictEvent(
    val todoIds: List<Long> = emptyList(),
    val isEvictAll: Boolean = false,
)
