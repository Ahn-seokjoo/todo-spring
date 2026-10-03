package com.seokjoo.todo.domain.service.outbox.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * OutboxEventProcessor가 sendEmail에 성공한 뒤 updateOutboxSuccess을 하기도 전에,
 * 그 사이 재할당된 다른 워커의 실패 누적으로 completionAttempts가 MAX_ATTEMPTS를 넘겨 updateOutboxDLQ가 먼저 실행돼버리면
 * 실제로는 발송에 성공한 이벤트가 outbox_event_dlq에 "실패"로 영구히 남는다.
 *
 * 해당 정합성을 맞추는 워커
 */
@Component
class OutboxDLQReconciler(
    private val outboxService: OutboxService,
) {
    private val logger = LoggerFactory.getLogger(OutboxDLQReconciler::class.java)

    @Scheduled(fixedDelayString = "\${outbox.dlq-reconciler.fixed-delay:300000}")
    fun reconcile() {
        runCatching {
            outboxService.reconcileConfirmedSentDlqEvents()
        }.onSuccess { movedCount ->
            if (movedCount > 0) {
                logger.info("OutboxDLQReconciler - moved $movedCount DLQ event(s) confirmed as sent to archive")
            }
        }.onFailure {
            logger.error("OutboxDLQReconciler - failed to reconcile DLQ events", it)
        }
    }
}
