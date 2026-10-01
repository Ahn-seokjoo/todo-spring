package com.seokjoo.todo.domain.service.outbox.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.email.EmailSentStatus
import com.seokjoo.todo.domain.entity.outbox.OutboxEvent
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.email.EmailService
import com.seokjoo.todo.domain.service.outbox.ClaimFailure
import com.seokjoo.todo.domain.service.outbox.ClaimSuccess
import com.seokjoo.todo.domain.service.outbox.OutboxEventType
import com.seokjoo.todo.domain.service.outbox.PublishableEvent
import org.springframework.dao.DataIntegrityViolationException
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
        when (val claim = outboxService.claimOutbox(eventId)) {
            is ClaimSuccess -> processClaimedEvent(claim, onFailure, onLog)

            is ClaimFailure.CheckDLQ -> {
                runCatching {
                    outboxService.updateOutboxDLQ(claim.outboxEvent)
                }.onFailure {
                    onFailure.invoke(it)
                }
                onLog.invoke("Check DLQ failure")
            }

            is ClaimFailure.Processing -> {
                onLog.invoke("Already Processing Event failure")
            }

            is ClaimFailure.NotClaimable -> {
                onLog.invoke("Not claimable — already processed elsewhere: $eventId")
            }
        }
    }

    private fun processClaimedEvent(
        claim: ClaimSuccess,
        onFailure: (Throwable) -> Unit,
        onLog: (String) -> Unit,
    ) {
        val outboxEvent = claim.outboxEvent

        val sendResult = when (outboxEvent.eventType) {
            OutboxEventType.PURCHASE_REQUEST -> runCatching {
                val purchase = objectMapper.readValue(
                    outboxEvent.serializedEvent,
                    PublishableEvent.PurchaseRequestEvent::class.java
                )
                val sellerEmail = authService.findUserByUserId(userId = purchase.sellerId).email
                    ?: throw TodoException.of(TodoExceptionType.OUTBOX_SELLER_EMAIL_EMPTY)

                sendPurchaseEmail(
                    outboxEventId = outboxEvent.id,
                    to = sellerEmail,
                    subject = "Todo 구매 요청",
                    content = "Todo [${purchase.todoId}] 구매 요청이 ${purchase.buyerId} 로부터 도착했습니다.",
                    onLog = onLog,
                )
            }

            OutboxEventType.PURCHASE_APPROVED -> runCatching {
                val purchase = objectMapper.readValue(
                    outboxEvent.serializedEvent,
                    PublishableEvent.PurchaseApprovedEvent::class.java
                )
                val buyerEmail = authService.findUserByUserId(userId = purchase.buyerId).email
                    ?: throw TodoException.of(TodoExceptionType.OUTBOX_BUYER_EMAIL_EMPTY)

                sendPurchaseEmail(
                    outboxEventId = outboxEvent.id,
                    to = buyerEmail,
                    subject = "Todo 구매 승인",
                    content = "Todo [${purchase.todoId}] 구매가 ${purchase.sellerId} 로부터 승인됐습니다.",
                    onLog = onLog,
                )
            }

            OutboxEventType.PURCHASE_REJECTED -> runCatching {
                val purchase = objectMapper.readValue(
                    outboxEvent.serializedEvent,
                    PublishableEvent.PurchaseRejectedEvent::class.java
                )
                val buyerEmail = authService.findUserByUserId(userId = purchase.buyerId).email
                    ?: throw TodoException.of(TodoExceptionType.OUTBOX_BUYER_EMAIL_EMPTY)

                sendPurchaseEmail(
                    outboxEventId = outboxEvent.id,
                    to = buyerEmail,
                    subject = "Todo 구매 거부",
                    content = "Todo [${purchase.todoId}] 구매가 ${purchase.sellerId} 로부터 거절되어 환불처리 됐습니다.",
                    onLog = onLog,
                )
            }

            else -> Result.success(Unit)
        }

        sendResult
            .onSuccess { handleSendSuccess(outboxEvent) }
            .onFailure { throwable -> handleSendFailure(outboxEvent, throwable, onFailure, onLog) }
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
        mailService.sendEmail(to = to, subject = subject, content = content)

        // sendEmail까지 끝나면 "발송 자체"는 이미 성공, 그 이후 markSentAt 기록 실패는 놔두고 후에 정합성 스케줄러가 처리
        // 실패를 롤백시키면 이미 나간 메일을 재시도 해 중복 발송으로 이어짐
        // TODO 정합성 워커 추가 예정
        runCatching { mailService.markSentAt(id = outboxEventId) }
            .onFailure { onLog.invoke("발송은 성공했지만 SENT 기록에 실패함(재시도 대상으로 돌리지 않음): $outboxEventId") }
    }

    private fun handleSendSuccess(outboxEvent: OutboxEvent) {
        outboxService.updateOutboxSuccess(outboxEvent = outboxEvent)
    }

    private fun handleSendFailure(
        outboxEvent: OutboxEvent,
        throwable: Throwable,
        onFailure: (Throwable) -> Unit,
        onLog: (String) -> Unit,
    ) {
        if (throwable is DataIntegrityViolationException) {
            when (mailService.findStatusByEmailId(outboxEvent.id)) {
                EmailSentStatus.SENT -> {
                    // 다른 워커가 이미 발송을 확정했다 -> 안전하게 중복 스킵, 성공 처리
                    outboxService.updateOutboxSuccess(outboxEvent = outboxEvent)
                    onLog.invoke("Duplicate email send skipped (confirmed already sent by another worker): ${outboxEvent.id}")
                }

                EmailSentStatus.READY -> {
                    // 다른 워커가 아직 결론을 못 낸 상태 -> 성공 단정은 안 하되, 이번 claim은 실패로 카운트해서
                    // 기존 completionAttempts/DLQ 파이프라인에 올린다. 그래야 그 다른 워커가 영영 안 끝나는
                    // 경우(크래시 등)에도 결국 DLQ로 빠져서 사람 눈에 띄게 된다.
                    outboxService.updateOutboxFail(
                        id = outboxEvent.id,
                        errorMessage = "Email send still unconfirmed by another worker (status=READY)",
                        claimedAt = requireNotNull(outboxEvent.processingStartedDate),
                    )
                    onLog.invoke("Email send still unconfirmed, counted as a retryable attempt: ${outboxEvent.id}")
                }

                null -> {
                    onLog.invoke("Conflicting record disappeared before status check, letting next attempt retry cleanly: ${outboxEvent.id}")
                }
            }
        } else {
            // 진짜 실패(sendEmail 이전/도중) -> 마킹 롤백하고 재시도 대상으로
            mailService.deleteSentRecord(id = outboxEvent.id)
            outboxService.updateOutboxFail(
                id = outboxEvent.id,
                errorMessage = throwable.message.orEmpty().take(1000),
                claimedAt = requireNotNull(outboxEvent.processingStartedDate),
            )
            onFailure.invoke(throwable)
        }
    }
}
