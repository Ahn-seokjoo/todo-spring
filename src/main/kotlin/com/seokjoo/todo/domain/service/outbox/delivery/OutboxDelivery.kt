package com.seokjoo.todo.domain.service.outbox.delivery

import com.seokjoo.todo.domain.entity.outbox.OutboxEvent

interface OutboxDelivery {
    fun deliver(outboxEvent: OutboxEvent, onLog: (String) -> Unit)
}
