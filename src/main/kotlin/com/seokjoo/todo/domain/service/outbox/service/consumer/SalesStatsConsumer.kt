package com.seokjoo.todo.domain.service.outbox.service.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.domain.service.outbox.OutboxEventType
import com.seokjoo.todo.domain.service.outbox.PublishableEvent
import com.seokjoo.todo.domain.service.outbox.delivery.PurchaseEventMessage
import com.seokjoo.todo.domain.service.sales.TodoSalesStatsRecordService
import com.seokjoo.todo.presentation.configuration.KafkaTopics
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId

@Component
@ConditionalOnProperty(prefix = "outbox", name = ["delivery-mode"], havingValue = "kafka")
class SalesStatsConsumer(
    private val objectMapper: ObjectMapper,
    private val recordService: TodoSalesStatsRecordService,
) {
    private val logger = LoggerFactory.getLogger(SalesStatsConsumer::class.java)

    @KafkaListener(topics = [KafkaTopics.PURCHASE_EVENT], groupId = "sales-stats")
    fun consume(record: ConsumerRecord<String, String>) {
        val message = objectMapper.readValue(record.value(), PurchaseEventMessage::class.java)
        if (message.eventType != OutboxEventType.PURCHASE_APPROVED) return

        val purchase = objectMapper.readValue(
            message.payload,
            PublishableEvent.PurchaseApprovedEvent::class.java
        )

        val occurredAt = message.occurredAt ?: run {
            logger.warn("occurredAt 없는 메시지, 레코드 timestamp로 대체: eventId={}", message.eventId)
            Instant.ofEpochMilli(record.timestamp())
        }
        val salesDate = occurredAt.atZone(ZoneId.of("Asia/Seoul")).toLocalDate()

        val applied = recordService.record(
            eventId = message.eventId,
            amount = purchase.price,
            sellerId = purchase.sellerId,
            salesDate = salesDate,
        )
        if (!applied) logger.info("이미 반영된 이벤트, 건너뜀: eventId={}", message.eventId)
    }
}
