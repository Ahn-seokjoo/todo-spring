package com.seokjoo.todo.domain.service.outbox.listener.event

data class TodoPurchaseEmailEvent(
    val outboxEventId: String,
)
