package com.seokjoo.todo.domain.service.outbox.listener.event

data class TodoPurchaseEvent(
    val outboxEventId: String,
)
