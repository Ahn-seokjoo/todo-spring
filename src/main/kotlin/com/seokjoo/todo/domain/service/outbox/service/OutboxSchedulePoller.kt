package com.seokjoo.todo.domain.service.outbox.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class OutboxSchedulePoller(
    private val outboxService: OutboxService,
    private val processor: OutboxEventProcessor,
) {
    private val logger = LoggerFactory.getLogger(OutboxSchedulePoller::class.java)

    @Scheduled(fixedDelayString = "\${outbox.poller.fixed-delay:60000}")
    fun process() {
        outboxService.updateAllResubmittedOutbox()
        val events = outboxService.findPollerEvents()
        events.forEach { event ->
            runCatching {
                processor.process(
                    eventId = event.id,
                    onFailure = { throwable ->
                        logger.error("OutboxSchedulePoller - Error while processing outbox", throwable)
                    },
                    onLog = { message ->
                        logger.info("OutboxSchedulePoller - Outbox scheduled $message")
                    }
                )
            }.onFailure {
                logger.error("OutboxSchedulePoller - unexpected error. id=${event.id}", it)
            }
        }
    }
}
