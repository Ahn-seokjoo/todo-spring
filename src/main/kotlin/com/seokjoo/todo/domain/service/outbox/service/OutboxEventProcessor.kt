package com.seokjoo.todo.domain.service.outbox.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.outbox.OutboxEvent
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.email.EmailService
import com.seokjoo.todo.domain.service.outbox.OutboxEventType
import com.seokjoo.todo.domain.service.outbox.PublishableEvent
import org.springframework.stereotype.Component

@Component
class OutboxEventProcessor(
    private val authService: TodoAuthService,
    private val objectMapper: ObjectMapper,
    private val mailService: EmailService,
    private val outboxService: OutboxService,
) {
    fun process(
        outboxEvent: OutboxEvent,
        onFailure: (Throwable) -> Unit,
    ) {
        // 실제 이메일 전송
        val result = when (outboxEvent.eventType) {
            OutboxEventType.PURCHASE_REQUEST -> runCatching {
                // json event를 읽고 (approve 타입으로)
                val purchase = objectMapper.readValue(
                    outboxEvent.serializedEvent,
                    PublishableEvent.PurchaseRequestEvent::class.java
                )
                // 판매자 email을 찾아서
                val seller = authService.findUserByUserId(userId = purchase.sellerId)
                val sellerEmail = seller.email ?: throw TodoException.of(TodoExceptionType.OUTBOX_SELLER_EMAIL_EMPTY)
                // 발송
                mailService.sendEmail(
                    to = sellerEmail,
                    subject = "Todo 구매 요청",
                    content = "Todo [${purchase.todoId}] 구매 요청이 ${purchase.buyerId} 로부터 도착했습니다."
                )
            }

            OutboxEventType.PURCHASE_APPROVED -> runCatching {
                // json event를 읽고 (approve 타입으로)
                val purchase = objectMapper.readValue(
                    outboxEvent.serializedEvent,
                    PublishableEvent.PurchaseApprovedEvent::class.java
                )
                // 구매자 email을 찾아서
                val buyer = authService.findUserByUserId(userId = purchase.buyerId)
                val buyerEmail = buyer.email ?: throw TodoException.of(TodoExceptionType.OUTBOX_BUYER_EMAIL_EMPTY)
                // 발송
                mailService.sendEmail(
                    to = buyerEmail,
                    subject = "Todo 구매 승인",
                    content = "Todo [${purchase.todoId}] 구매가 ${purchase.sellerId} 로부터 승인됐습니다."
                )
            }

            OutboxEventType.PURCHASE_REJECTED -> runCatching {
                // json event를 읽고 (reject 타입으로)
                val purchase = objectMapper.readValue(
                    outboxEvent.serializedEvent,
                    PublishableEvent.PurchaseRejectedEvent::class.java
                )
                // 구매자 email을 찾아서
                val buyer = authService.findUserByUserId(userId = purchase.buyerId)
                val buyerEmail = buyer.email ?: throw TodoException.of(TodoExceptionType.OUTBOX_BUYER_EMAIL_EMPTY)
                // 발송
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
            .onFailure { throwable ->
                outboxService.updateOutboxFail(id = outboxEvent.id)
                onFailure.invoke(throwable)
            }
    }
}
