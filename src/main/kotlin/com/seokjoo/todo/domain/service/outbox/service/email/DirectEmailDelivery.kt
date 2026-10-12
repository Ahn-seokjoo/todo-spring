package com.seokjoo.todo.domain.service.outbox.service.email

import com.seokjoo.todo.domain.entity.outbox.OutboxEvent
import com.seokjoo.todo.domain.service.outbox.delivery.OutboxDelivery
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "outbox", name = ["delivery-mode"], havingValue = "direct", matchIfMissing = true)
class DirectEmailDelivery(
    private val sender: PurchaseEmailSender,
) : OutboxDelivery {

    override fun deliver(
        outboxEvent: OutboxEvent,
        onLog: (String) -> Unit
    ) {
        sender.send(
            eventId = outboxEvent.id,
            eventType = outboxEvent.eventType,
            serializedEvent = outboxEvent.serializedEvent,
            onLog = onLog,
        )
    }
}
