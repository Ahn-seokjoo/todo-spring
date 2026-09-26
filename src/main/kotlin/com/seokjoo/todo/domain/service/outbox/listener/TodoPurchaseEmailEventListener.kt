package com.seokjoo.todo.domain.service.outbox.listener

import com.seokjoo.todo.domain.service.outbox.annotation.TodoTransactionalEventListener
import com.seokjoo.todo.domain.service.outbox.listener.event.TodoPurchaseEmailEvent
import com.seokjoo.todo.domain.service.outbox.service.OutboxEventProcessor
import com.seokjoo.todo.domain.service.outbox.service.OutboxService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TodoPurchaseEmailEventListener(
    private val outboxService: OutboxService,
    private val outboxEventProcessor: OutboxEventProcessor,
) {
    private val logger = LoggerFactory.getLogger(TodoPurchaseEmailEventListener::class.java)

    @TodoTransactionalEventListener
    fun eventListener(event: TodoPurchaseEmailEvent) {
        val outboxEvent = outboxService.claimOutbox(event.outboxEventId)

        outboxEventProcessor.process(
            outboxEvent = outboxEvent,
            onFailure = { throwable ->
                logger.error("TodoPurchaseEmailEventListener - Email Send Error: outboxId=${outboxEvent.id}", throwable)
            }
        )
    }
}
