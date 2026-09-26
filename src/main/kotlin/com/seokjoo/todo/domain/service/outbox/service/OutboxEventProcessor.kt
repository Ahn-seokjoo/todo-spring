package com.seokjoo.todo.domain.service.outbox.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.email.EmailService
import com.seokjoo.todo.domain.service.outbox.ClaimFailure
import com.seokjoo.todo.domain.service.outbox.ClaimSuccess
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
        eventId: String,
        onFailure: (throwable: Throwable) -> Unit = {},
        onLog: (String) -> Unit = {},
    ) {
        // 실제 이메일 전송
        when (val claim = outboxService.claimOutbox(eventId)) {
            is ClaimSuccess -> {
                val result = when (claim.outboxEvent.eventType) {
                    OutboxEventType.PURCHASE_REQUEST -> runCatching {
                        val purchase = objectMapper.readValue(
                            claim.outboxEvent.serializedEvent,
                            PublishableEvent.PurchaseRequestEvent::class.java
                        )
                        val seller = authService.findUserByUserId(userId = purchase.sellerId)
                        val sellerEmail =
                            seller.email ?: throw TodoException.of(TodoExceptionType.OUTBOX_SELLER_EMAIL_EMPTY)

                        mailService.sendEmail(
                            to = sellerEmail,
                            subject = "Todo 구매 요청",
                            content = "Todo [${purchase.todoId}] 구매 요청이 ${purchase.buyerId} 로부터 도착했습니다."
                        )
                    }

                    OutboxEventType.PURCHASE_APPROVED -> runCatching {
                        val purchase = objectMapper.readValue(
                            claim.outboxEvent.serializedEvent,
                            PublishableEvent.PurchaseApprovedEvent::class.java
                        )
                        val buyer = authService.findUserByUserId(userId = purchase.buyerId)
                        val buyerEmail =
                            buyer.email ?: throw TodoException.of(TodoExceptionType.OUTBOX_BUYER_EMAIL_EMPTY)

                        mailService.sendEmail(
                            to = buyerEmail,
                            subject = "Todo 구매 승인",
                            content = "Todo [${purchase.todoId}] 구매가 ${purchase.sellerId} 로부터 승인됐습니다."
                        )
                    }

                    OutboxEventType.PURCHASE_REJECTED -> runCatching {
                        val purchase = objectMapper.readValue(
                            claim.outboxEvent.serializedEvent,
                            PublishableEvent.PurchaseRejectedEvent::class.java
                        )
                        val buyer = authService.findUserByUserId(userId = purchase.buyerId)
                        val buyerEmail =
                            buyer.email ?: throw TodoException.of(TodoExceptionType.OUTBOX_BUYER_EMAIL_EMPTY)

                        mailService.sendEmail(
                            to = buyerEmail,
                            subject = "Todo 구매 거부",
                            content = "Todo [${purchase.todoId}] 구매가 ${purchase.sellerId} 로부터 거절되어 환불처리 됐습니다."
                        )
                    }

                    else -> Result.success(Unit)
                }
                result
                    .onSuccess { outboxService.updateOutboxSuccess(outboxEvent = claim.outboxEvent) }
                    .onFailure { throwable ->
                        outboxService.updateOutboxFail(
                            id = claim.outboxEvent.id,
                            errorMessage = throwable.message.orEmpty(),
                        )
                        onFailure.invoke(throwable)
                    }
            }

            is ClaimFailure.CheckDLQ -> {
                runCatching {
                    outboxService.updateOutboxDLQ(claim.outboxEvent)
                }.onFailure { /** 여러 인스턴스가 dlq 이관 시도 시에 pk 중복 에러로 실패 날 수 있지만 무시 */ }
                onLog.invoke("Check DLQ failure")
            }

            is ClaimFailure.Processing -> {
                onLog.invoke("Process failure")
            }
        }
    }
}
