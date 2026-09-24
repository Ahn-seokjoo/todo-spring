package com.seokjoo.todo.domain.service.outbox.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.outbox.OutboxEventType
import com.seokjoo.todo.domain.service.outbox.PublishableEvent
import com.seokjoo.todo.domain.service.outbox.annotation.TodoTransactionalEventListener
import com.seokjoo.todo.domain.service.outbox.listener.event.TodoPurchaseEmailEvent
import com.seokjoo.todo.domain.service.outbox.service.OutboxService
import org.springframework.stereotype.Component

@Component
class TodoPurchaseEmailEventListener(
    private val outboxService: OutboxService,
    private val authService: TodoAuthService,
    private val objectMapper: ObjectMapper,
) {

    @TodoTransactionalEventListener
    fun eventListener(event: TodoPurchaseEmailEvent) {
        val outboxEvent = outboxService.getOutbox(event.outboxEventId)

        // 실제 이메일 전송
        when (outboxEvent.eventType) {
            OutboxEventType.PURCHASE_REQUEST -> {
                val purchase = objectMapper.readValue(
                    outboxEvent.serializedEvent,
                    PublishableEvent.PurchaseRequestEvent::class.java
                )
                val seller = authService.findUserByUserId(userId = purchase.sellerId)
                val sellerEmail = seller.email
            }

            OutboxEventType.PURCHASE_APPROVED -> {
                val purchase = objectMapper.readValue(
                    outboxEvent.serializedEvent,
                    PublishableEvent.PurchaseApprovedEvent::class.java
                )
                val buyer = authService.findUserByUserId(userId = purchase.buyerId)
                val buyerEmail = buyer.email
            }

            OutboxEventType.PURCHASE_REJECTED -> {
                val purchase = objectMapper.readValue(
                    outboxEvent.serializedEvent,
                    PublishableEvent.PurchaseRejectedEvent::class.java
                )
                val buyer = authService.findUserByUserId(userId = purchase.buyerId)
                val buyerEmail = buyer.email
            }

            else -> {
                // no-op
            }
        }

        // 임시 true, 구매 정보로 이메일 전송 성공 여부 따라 if 문 분기
        if (true) {
            outboxService.updateOutboxSuccess(outboxEvent = outboxEvent)
        } else {
            outboxService.updateOutboxFail(id = outboxEvent.id)
        }
    }
}
