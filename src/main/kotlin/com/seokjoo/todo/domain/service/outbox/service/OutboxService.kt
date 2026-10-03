package com.seokjoo.todo.domain.service.outbox.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.outbox.OutboxEvent
import com.seokjoo.todo.domain.entity.outbox.OutboxEventArchive
import com.seokjoo.todo.domain.entity.outbox.OutboxEventDLQ
import com.seokjoo.todo.domain.entity.outbox.OutboxStatus
import com.seokjoo.todo.domain.service.outbox.ClaimFailure
import com.seokjoo.todo.domain.service.outbox.ClaimSuccess
import com.seokjoo.todo.domain.service.outbox.OutboxEventClaimType
import com.seokjoo.todo.domain.service.outbox.OutboxListenerType
import com.seokjoo.todo.domain.service.outbox.PublishableEvent
import com.seokjoo.todo.domain.service.outbox.repository.OutboxArchiveRepository
import com.seokjoo.todo.domain.service.outbox.repository.OutboxDLQRepository
import com.seokjoo.todo.domain.service.outbox.repository.OutboxRepository
import com.seokjoo.todo.domain.service.outbox.repository.OutboxRepository.Companion.MAX_ATTEMPTS
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

@Service
class OutboxService(
    private val outboxRepository: OutboxRepository,
    private val outboxArchiveRepository: OutboxArchiveRepository,
    private val outboxDLQRepository: OutboxDLQRepository,
    private val objectMapper: ObjectMapper,
    @param:Value("\${outbox.poller.stale-threshold}") private val staleThresholdMillis: Long,
) {

    @Transactional
    fun saveOutbox(event: OutboxEvent): OutboxEvent {
        return outboxRepository.save(event)
    }

    @Transactional
    fun claimOutbox(id: String): OutboxEventClaimType {
        val effectedCount = outboxRepository.updateProcessingOutbox(id)
        val outboxEvent =
            outboxRepository.findByIdOrNull(id) ?: return ClaimFailure.NotClaimable

        if (effectedCount == 1) return ClaimSuccess(outboxEvent)
        return when {
            outboxEvent.completionAttempts >= MAX_ATTEMPTS -> {
                ClaimFailure.CheckDLQ(outboxEvent)
            }

            outboxEvent.status == OutboxStatus.PROCESSING -> {
                ClaimFailure.Processing
            }

            else -> {
                // 현재는 올수 없지만, 추후에 누군가 리팩토링으로 이곳으로 흘러오는것을 방지하기 위해 둠
                throw TodoException.of(TodoExceptionType.OUTBOX_UPDATE_ERROR)
            }
        }
    }

    @Transactional
    fun updateOutboxFail(id: String, errorMessage: String, claimedAt: LocalDateTime) {
        outboxRepository.updateFailedOutbox(id, errorMessage, claimedAt)
    }

    @Transactional
    fun updateOutboxSuccess(outboxEvent: OutboxEvent) {
        val effectedCount = outboxRepository.deleteOutboxEventById(outboxEvent.id)
        if (effectedCount == 0) return
        outboxArchiveRepository.save(OutboxEventArchive.from(outboxEvent))
    }

    @Transactional
    fun updateOutboxDLQ(outboxEvent: OutboxEvent) {
        val effectedCount = outboxRepository.deleteOutboxEventById(outboxEvent.id)
        if (effectedCount == 0) return
        outboxDLQRepository.save(OutboxEventDLQ.from(outboxEvent))
    }

    // DLQ 전환과 success 전환이 경합 때를 위한 정합화.(4회 재시도, 5회차 때 성공 + 윈도우에 DLQ로 다른 워커가 이관해버리는 경우)
    // email_sent_record가 SENT로 확정된 DLQ 이벤트는 "이미 보냈다"는 확정된 증거이므로, 새로운 판단 없이 archive로 이관하고 DLQ에서는 제거한다.
    @Transactional
    fun reconcileConfirmedSentDlqEvents(): Int {
        val confirmedSentDlqEvents = outboxDLQRepository.findConfirmedSentDlqEvents()
        if (confirmedSentDlqEvents.isEmpty()) return 0

        outboxArchiveRepository.saveAll(confirmedSentDlqEvents.map { OutboxEventArchive.from(it) })
        outboxDLQRepository.deleteAll(confirmedSentDlqEvents)

        return confirmedSentDlqEvents.size
    }

    // deleteOutboxEventById는 파생 delete 쿼리라 자체 트랜잭션이 없어서, 서비스 메서드로 감싸서 노출
    @Transactional
    fun deleteOutboxEvent(id: String) {
        outboxRepository.deleteOutboxEventById(id)
    }

    @Transactional
    fun findPollerEvents(): List<OutboxEvent> {
        return outboxRepository.findPollerEvents()
    }

    @Transactional
    fun updateAllResubmittedOutbox() {
        val cutoff = LocalDateTime.now().minus(Duration.ofMillis(staleThresholdMillis))
        outboxRepository.updateAllResubmittedOutbox(cutoff = cutoff)
    }

    fun createOutboxEvent(event: PublishableEvent, listener: OutboxListenerType): OutboxEvent {
        return OutboxEvent(
            listenerType = listener,
            eventType = event.eventType,
            serializedEvent = objectMapper.writeValueAsString(event),
            publicationDate = LocalDateTime.now(),
        )
    }
}
