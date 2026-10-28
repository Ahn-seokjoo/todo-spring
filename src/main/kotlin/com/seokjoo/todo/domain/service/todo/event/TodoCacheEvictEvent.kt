package com.seokjoo.todo.domain.service.todo.event

data class TodoCacheEvictEvent(
    val todoIds: Set<Long> = emptySet(),
    val userIds: Set<String> = emptySet(),
)
