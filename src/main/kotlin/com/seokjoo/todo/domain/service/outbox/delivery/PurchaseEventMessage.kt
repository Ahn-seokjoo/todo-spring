package com.seokjoo.todo.domain.service.outbox.delivery

import com.seokjoo.todo.domain.service.outbox.OutboxEventType
import java.time.Instant

data class PurchaseEventMessage(
    val eventId: String,                // outbox id
    val eventType: OutboxEventType,     // 어떤 메일 타입인지
    val payload: String,                // outbox.serializedEvent 그대로
    val occurredAt: Instant? = null,    // 이벤트 발생 시각 - 하위 호환으로 nullable
)
