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
            outboxRepository.findByIdOrNull(id) ?: throw TodoException.of(TodoExceptionType.OUTBOX_NOT_FOUND)

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
    fun updateOutboxSuccess(outboxEvent: OutboxEvent) {
        outboxRepository.deleteOutboxEventById(outboxEvent.id)
        outboxArchiveRepository.save(OutboxEventArchive.from(outboxEvent))
    }

    @Transactional
    fun updateOutboxFail(id: String, errorMessage: String, claimedAt: LocalDateTime) {
        outboxRepository.updateFailedOutbox(id, errorMessage, claimedAt)
    }

    @Transactional
    fun updateOutboxDLQ(outboxEvent: OutboxEvent) {
        val effectedCount = outboxRepository.deleteOutboxEventById(outboxEvent.id)
        if (effectedCount == 0) return
        outboxDLQRepository.save(OutboxEventDLQ.from(outboxEvent))
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
