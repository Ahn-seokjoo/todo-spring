package com.seokjoo.todo.domain.service.outbox

enum class OutboxListenerType {
    EMAIL_NOTIFICATION,
    CACHE_EVICTION,
}
