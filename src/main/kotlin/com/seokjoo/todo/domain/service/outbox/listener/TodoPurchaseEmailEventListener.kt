package com.seokjoo.todo.domain.service.outbox.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.email.EmailService
import com.seokjoo.todo.domain.service.outbox.OutboxEventType
import com.seokjoo.todo.domain.service.outbox.PublishableEvent
import com.seokjoo.todo.domain.service.outbox.annotation.TodoTransactionalEventListener
import com.seokjoo.todo.domain.service.outbox.listener.event.TodoPurchaseEmailEvent
import com.seokjoo.todo.domain.service.outbox.service.OutboxService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TodoPurchaseEmailEventListener(
    private val outboxService: OutboxService,
    private val authService: TodoAuthService,
    private val objectMapper: ObjectMapper,
    private val mailService: EmailService,
) {
    private val logger = LoggerFactory.getLogger(TodoPurchaseEmailEventListener::class.java)

    @TodoTransactionalEventListener
    fun eventListener(event: TodoPurchaseEmailEvent) {
        val outboxEvent = outboxService.getOutbox(event.outboxEventId)

        // 실제 이메일 전송
        val result = when (outboxEvent.eventType) {
            OutboxEventType.PURCHASE_REQUEST -> runCatching {
                val purchase = objectMapper.readValue(
                    outboxEvent.serializedEvent,
                    PublishableEvent.PurchaseRequestEvent::class.java
                )
                val seller = authService.findUserByUserId(userId = purchase.sellerId)
                val sellerEmail = requireNotNull(seller.email) { "판매자 이메일이 등록되지 않았습니다." }
                mailService.sendEmail(
                    to = sellerEmail,
                    subject = "Todo 구매 요청",
                    content = "Todo [${purchase.todoId}] 구매 요청이 ${purchase.buyerId} 로부터 도착했습니다."
                )
            }

            OutboxEventType.PURCHASE_APPROVED -> runCatching {
                val purchase = objectMapper.readValue(
                    outboxEvent.serializedEvent,
                    PublishableEvent.PurchaseApprovedEvent::class.java
                )
                val buyer = authService.findUserByUserId(userId = purchase.buyerId)
                val buyerEmail = requireNotNull(buyer.email) { "구매자 이메일이 등록되지 않았습니다." }
                mailService.sendEmail(
                    to = buyerEmail,
                    subject = "Todo 구매 승인",
                    content = "Todo [${purchase.todoId}] 구매가 ${purchase.sellerId} 로부터 승인됐습니다."
                )
            }

            OutboxEventType.PURCHASE_REJECTED -> runCatching {
                val purchase = objectMapper.readValue(
                    outboxEvent.serializedEvent,
                    PublishableEvent.PurchaseRejectedEvent::class.java
                )
                val buyer = authService.findUserByUserId(userId = purchase.buyerId)
                val buyerEmail = requireNotNull(buyer.email) { "구매자 이메일이 등록되지 않았습니다." }
                mailService.sendEmail(
                    to = buyerEmail,
                    subject = "Todo 구매 거부",
                    content = "Todo [${purchase.todoId}] 구매가 ${purchase.sellerId} 로부터 거절되어 환불처리 됐습니다."
                )
            }

            else -> Result.success(Unit)
        }

        result
            .onSuccess { outboxService.updateOutboxSuccess(outboxEvent = outboxEvent) }
            .onFailure {
                outboxService.updateOutboxFail(id = outboxEvent.id)
                logger.error("Email Send Error: outboxId=${outboxEvent.id}", it)
            }
    }
}
