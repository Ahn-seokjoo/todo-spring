package com.seokjoo.todo.domain.service.outbox.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.outbox.OutboxEvent
import com.seokjoo.todo.domain.entity.outbox.OutboxEventArchive
import com.seokjoo.todo.domain.service.outbox.OutboxListenerType
import com.seokjoo.todo.domain.service.outbox.PublishableEvent
import com.seokjoo.todo.domain.service.outbox.repository.OutboxArchiveRepository
import com.seokjoo.todo.domain.service.outbox.repository.OutboxRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class OutboxService(
    private val outboxRepository: OutboxRepository,
    private val outboxArchiveRepository: OutboxArchiveRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun saveOutbox(event: OutboxEvent): OutboxEvent {
        return outboxRepository.save(event)
    }

    @Transactional
    fun claimOutbox(id: String): OutboxEvent {
        val effectedCount = outboxRepository.updateProcessingOutbox(id)
        check(effectedCount == 1) {
            throw TodoException.of(TodoExceptionType.OUTBOX_UPDATE_ERROR)
        }
        return outboxRepository.findByIdOrNull(id) ?: throw TodoException.of(TodoExceptionType.OUTBOX_NOT_FOUND)
    }

    @Transactional
    fun updateOutboxSuccess(outboxEvent: OutboxEvent) {
        outboxRepository.deleteOutboxEventById(outboxEvent.id)
        outboxArchiveRepository.save(OutboxEventArchive.from(outboxEvent))
    }

    @Transactional
    fun updateOutboxFail(id: String) {
        outboxRepository.updateFailedOutbox(id)
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
