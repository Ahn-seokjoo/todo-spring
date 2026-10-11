package com.seokjoo.todo.domain.service.outbox.delivery

import com.seokjoo.todo.domain.service.outbox.OutboxEventType

data class PurchaseEventMessage(
    val eventId: String,            // outbox id
    val eventType: OutboxEventType, // 어떤 메일 타입인지
    val payload: String,            // outbox.serializedEvent 그대로
)
