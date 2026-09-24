package com.seokjoo.todo.domain.service.outbox.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.outbox.OutboxEventType
import com.seokjoo.todo.domain.service.outbox.PublishableEvent
import com.seokjoo.todo.domain.service.outbox.annotation.TodoTransactionalEventListener
import com.seokjoo.todo.domain.service.outbox.listener.event.TodoPurchaseEvent
import com.seokjoo.todo.domain.service.outbox.service.OutboxService
import org.springframework.stereotype.Component

@Component
class TodoPurchaseEventListener(
    private val outboxService: OutboxService,
    private val authService: TodoAuthService,
    private val objectMapper: ObjectMapper,
) {

    @TodoTransactionalEventListener
    fun eventListener(event: TodoPurchaseEvent) {
        val outboxEvent = outboxService.getOutbox(event.outboxEventId)
        when (outboxEvent.eventType) {
            OutboxEventType.PURCHASE_REQUEST -> {
                val purchase = objectMapper.readValue(
                    outboxEvent.serializedEvent,
                    PublishableEvent.PurchaseRequestEvent::class.java
                )
                val seller = authService.findUserByUserId(purchase.sellerId)
                val sellerEmail = seller.email
                // 임시 true, 구매 정보로 이메일 전송 성공 여부 따라 if 문 분기
                if (true) {
                    outboxService.updateOutboxSuccess(outboxEvent = outboxEvent)
                } else {
                    outboxService.updateOutboxFail(id = outboxEvent.id)
                }
            }

            else -> {
                // no-op
            }
        }
    }
}
