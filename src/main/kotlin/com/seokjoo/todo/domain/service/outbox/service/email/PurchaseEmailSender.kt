package com.seokjoo.todo.domain.service.outbox.service.email

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.email.EmailService
import com.seokjoo.todo.domain.service.outbox.OutboxEventType
import com.seokjoo.todo.domain.service.outbox.PublishableEvent
import org.springframework.stereotype.Component

@Component
class PurchaseEmailSender(
    private val authService: TodoAuthService,
    private val objectMapper: ObjectMapper,
    private val mailService: EmailService,
) {
    fun send(
        eventId: String,
        eventType: OutboxEventType,
        serializedEvent: String,
        onLog: (String) -> Unit,
    ) {
        when (eventType) {
            OutboxEventType.PURCHASE_REQUEST -> {
                val purchase = objectMapper.readValue(
                    serializedEvent,
                    PublishableEvent.PurchaseRequestEvent::class.java
                )
                val sellerEmail = authService.findUserByUserId(userId = purchase.sellerId).email
                    ?: throw TodoException.of(TodoExceptionType.OUTBOX_SELLER_EMAIL_EMPTY)

                sendPurchaseEmail(
                    outboxEventId = eventId,
                    to = sellerEmail,
                    subject = "Todo 구매 요청",
                    content = "Todo [${purchase.todoId}] 구매 요청이 ${purchase.buyerId} 로부터 도착했습니다.",
                    onLog = onLog,
                )
            }

            OutboxEventType.PURCHASE_APPROVED -> {
                val purchase = objectMapper.readValue(
                    serializedEvent,
                    PublishableEvent.PurchaseApprovedEvent::class.java
                )
                val buyerEmail = authService.findUserByUserId(userId = purchase.buyerId).email
                    ?: throw TodoException.of(TodoExceptionType.OUTBOX_BUYER_EMAIL_EMPTY)

                sendPurchaseEmail(
                    outboxEventId = eventId,
                    to = buyerEmail,
                    subject = "Todo 구매 승인",
                    content = "Todo [${purchase.todoId}] 구매가 ${purchase.sellerId} 로부터 승인됐습니다.",
                    onLog = onLog,
                )
            }

            OutboxEventType.PURCHASE_REJECTED -> {
                val purchase = objectMapper.readValue(
                    serializedEvent,
                    PublishableEvent.PurchaseRejectedEvent::class.java
                )
                val buyerEmail = authService.findUserByUserId(userId = purchase.buyerId).email
                    ?: throw TodoException.of(TodoExceptionType.OUTBOX_BUYER_EMAIL_EMPTY)

                sendPurchaseEmail(
                    outboxEventId = eventId,
                    to = buyerEmail,
                    subject = "Todo 구매 거부",
                    content = "Todo [${purchase.todoId}] 구매가 ${purchase.sellerId} 로부터 거절되어 환불처리 됐습니다.",
                    onLog = onLog,
                )
            }
        }
    }

    // saveEmailSentRecordToReady(멱등키 선점) -> sendEmail(실제 발송) -> markSentAt(발송 확정 기록) 순서로 진행한다.
    private fun sendPurchaseEmail(
        outboxEventId: String,
        to: String,
        subject: String,
        content: String,
        onLog: (String) -> Unit,
    ) {
        mailService.saveEmailSentRecordToReady(id = outboxEventId)
        runCatching {
            mailService.sendEmail(to = to, subject = subject, content = content)
        }.onFailure { throwable ->
            mailService.deleteSentRecord(id = outboxEventId)
            throw throwable
        }

        // sendEmail까지 끝나면 "발송 자체"는 이미 성공, 그 이후 markSentAt 기록 실패는 놔두고 후에 EmailSentRecordReconciler 가 처리
        runCatching { mailService.markSentAt(id = outboxEventId) }
            .onFailure { onLog.invoke("발송은 성공했지만 SENT 기록에 실패함 : $outboxEventId") }
    }
}
