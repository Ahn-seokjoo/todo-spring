package com.seokjoo.todo.domain.service.outbox.listener

import com.seokjoo.todo.domain.service.outbox.annotation.TodoTransactionalEventListener
import com.seokjoo.todo.domain.service.outbox.listener.event.TodoPurchaseEmailEvent
import com.seokjoo.todo.domain.service.outbox.service.OutboxEventProcessor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TodoPurchaseEmailEventListener(
    private val processor: OutboxEventProcessor,
) {
    private val logger = LoggerFactory.getLogger(TodoPurchaseEmailEventListener::class.java)

    @TodoTransactionalEventListener
    fun eventListener(event: TodoPurchaseEmailEvent) {
        processor.process(
            eventId = event.outboxEventId,
            onFailure = { throwable ->
                logger.error("TodoPurchaseEmailEventListener - Error while processing outbox", throwable)
            },
            onLog = { message ->
                logger.info("TodoPurchaseEmailEventListener - failed - $message")
            }
        )
    }
}
