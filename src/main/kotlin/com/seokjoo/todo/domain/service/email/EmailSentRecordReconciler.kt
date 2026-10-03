package com.seokjoo.todo.domain.service.email

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 실제 메일 발송 성공 이후 markSentAt 이 실패한 경우를 보정해주는 워커 5분마다 보정
 */
@Component
class EmailSentRecordReconciler(
    private val emailService: EmailService,
) {
    private val logger = LoggerFactory.getLogger(EmailSentRecordReconciler::class.java)

    @Scheduled(fixedDelayString = "\${email.reconciler.fixed-delay:300000}")
    fun reconcile() {
        runCatching {
            emailService.reconcileConfirmedSentRecords()
        }.onSuccess { updatedCount ->
            if (updatedCount > 0) {
                logger.info("EmailSentRecordReconciler - reconciled $updatedCount READY record(s) to SENT")
            }
        }.onFailure {
            logger.error("EmailSentRecordReconciler - failed to reconcile READY records", it)
        }
    }
}
