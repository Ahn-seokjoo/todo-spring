package com.seokjoo.todo.domain.service.outbox.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.email.EmailService
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
    private val mailService: EmailService,
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
                sellerEmail?.let {
                    mailService.sendEmail(
                        to = sellerEmail,
                        subject = "Todo 구매 요청",
                        email = "Todo [${purchase.todoId}] 구매 요청이 ${purchase.buyerId} 로부터 도착했습니다."
                    )
                }
            }

            OutboxEventType.PURCHASE_APPROVED -> {
                val purchase = objectMapper.readValue(
                    outboxEvent.serializedEvent,
                    PublishableEvent.PurchaseApprovedEvent::class.java
                )
                val buyer = authService.findUserByUserId(userId = purchase.buyerId)
                val buyerEmail = buyer.email
                buyerEmail?.let {
                    mailService.sendEmail(
                        to = buyerEmail,
                        subject = "Todo 구매 승인",
                        email = "Todo [${purchase.todoId}] 구매가 ${purchase.sellerId} 로부터 승인됐습니다."
                    )
                }
            }

            OutboxEventType.PURCHASE_REJECTED -> {
                val purchase = objectMapper.readValue(
                    outboxEvent.serializedEvent,
                    PublishableEvent.PurchaseRejectedEvent::class.java
                )
                val buyer = authService.findUserByUserId(userId = purchase.buyerId)
                val buyerEmail = buyer.email
                buyerEmail?.let {
                    mailService.sendEmail(
                        to = buyerEmail,
                        subject = "Todo 구매 거부",
                        email = "Todo [${purchase.todoId}] 구매가 ${purchase.sellerId} 로부터 거절되어 환불처리 됐습니다."
                    )
                }
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
