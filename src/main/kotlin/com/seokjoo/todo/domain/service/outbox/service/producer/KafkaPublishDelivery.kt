package com.seokjoo.todo.domain.service.outbox.service.producer

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.domain.entity.outbox.OutboxEvent
import com.seokjoo.todo.domain.service.outbox.delivery.OutboxDelivery
import com.seokjoo.todo.domain.service.outbox.delivery.PurchaseEventMessage
import com.seokjoo.todo.presentation.configuration.KafkaTopics
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty(prefix = "outbox", name = ["delivery-mode"], havingValue = "kafka")
class KafkaPublishDelivery(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : OutboxDelivery {

    override fun deliver(
        outboxEvent: OutboxEvent,
        onLog: (String) -> Unit
    ) {
        val key = objectMapper.readTree(outboxEvent.serializedEvent).get("purchaseId")
            ?.takeUnless { it.isNull }
            ?.asText()
            ?: throw IllegalStateException("purchaseId가 없는 아웃박스 이벤트: ${outboxEvent.id}")

        val message = PurchaseEventMessage(
            eventId = outboxEvent.id,
            eventType = outboxEvent.eventType,
            payload = outboxEvent.serializedEvent,
        )

        kafkaTemplate.send(KafkaTopics.PURCHASE_EVENT, key, objectMapper.writeValueAsString(message))
            .get(10, TimeUnit.SECONDS)
    }
}
