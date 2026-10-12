package com.seokjoo.todo.domain.service.outbox.service

import com.seokjoo.todo.domain.entity.email.EmailSentStatus
import com.seokjoo.todo.domain.entity.outbox.OutboxEvent
import com.seokjoo.todo.domain.service.email.EmailService
import com.seokjoo.todo.domain.service.outbox.ClaimFailure
import com.seokjoo.todo.domain.service.outbox.ClaimSuccess
import com.seokjoo.todo.domain.service.outbox.delivery.OutboxDelivery
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
class OutboxEventProcessor(
    private val mailService: EmailService,
    private val outboxService: OutboxService,
    private val delivery: OutboxDelivery,
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
        runCatching { delivery.deliver(outboxEvent, onLog) }
            .onSuccess { handleSendSuccess(outboxEvent) }
            .onFailure { throwable -> handleSendFailure(outboxEvent, throwable, onFailure, onLog) }
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
            outboxService.updateOutboxFail(
                id = outboxEvent.id,
                errorMessage = throwable.message.orEmpty().take(1000),
                claimedAt = requireNotNull(outboxEvent.processingStartedDate),
            )
            onFailure.invoke(throwable)
        }
    }
}
