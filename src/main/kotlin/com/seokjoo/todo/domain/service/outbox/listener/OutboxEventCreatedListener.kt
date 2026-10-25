package com.seokjoo.todo.domain.service.outbox.listener

import com.seokjoo.todo.common.annotation.TodoTransactionalEventListener
import com.seokjoo.todo.domain.service.outbox.event.OutboxEventCreatedEvent
import com.seokjoo.todo.domain.service.outbox.service.OutboxEventProcessor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OutboxEventCreatedListener(
    private val processor: OutboxEventProcessor,
) {
    private val logger = LoggerFactory.getLogger(OutboxEventCreatedListener::class.java)

    @TodoTransactionalEventListener
    fun eventListener(event: OutboxEventCreatedEvent) {
        processor.process(
            eventId = event.outboxEventId,
            onFailure = { throwable ->
                logger.error("OutboxEventCreatedListener - Error while processing outbox", throwable)
            },
            onLog = { message ->
                logger.info("OutboxEventCreatedListener - failed - $message")
            }
        )
    }
}
