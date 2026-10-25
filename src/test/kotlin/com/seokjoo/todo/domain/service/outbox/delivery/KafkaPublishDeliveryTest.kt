package com.seokjoo.todo.domain.service.outbox.delivery

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.seokjoo.todo.domain.entity.outbox.OutboxEvent
import com.seokjoo.todo.domain.service.outbox.OutboxEventType
import com.seokjoo.todo.domain.service.outbox.PublishableEvent
import com.seokjoo.todo.domain.service.outbox.service.producer.KafkaPublishDelivery
import com.seokjoo.todo.presentation.configuration.KafkaTopics
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

class KafkaPublishDeliveryTest {
    private val kafkaTemplate: KafkaTemplate<String, String> = mockk()
    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val delivery = KafkaPublishDelivery(kafkaTemplate, objectMapper)

    private val outboxEvent = OutboxEvent(
        eventType = OutboxEventType.PURCHASE_APPROVED,
        serializedEvent = objectMapper.writeValueAsString(
            PublishableEvent.PurchaseApprovedEvent(
                sellerId = "s", buyerId = "b", todoId = 1L, price = 100L, purchaseId = 42L,
            )
        ),
        publicationDate = LocalDateTime.of(2026, 10, 19, 0, 0), // KST 기준 승인 시각 (고정값)
    ).apply { id = "outbox-1" }

    @Test
    fun `purchaseId를 key로, outbox id를 eventId로, 아웃박스 생성 시각을 occurredAt으로 담아 발행한다`() {
        val value = slot<String>()
        every { kafkaTemplate.send(KafkaTopics.PURCHASE_EVENT, "42", capture(value)) } returns
            CompletableFuture.completedFuture(mockk<SendResult<String, String>>())

        delivery.deliver(outboxEvent) {}

        val message = objectMapper.readValue(value.captured, PurchaseEventMessage::class.java)
        assertThat(message.eventId).isEqualTo("outbox-1")
        assertThat(message.eventType).isEqualTo(OutboxEventType.PURCHASE_APPROVED)
        assertThat(message.payload).isEqualTo(outboxEvent.serializedEvent)
        // KST 2026-10-19 00:00 = UTC 2026-10-18 15:00
        assertThat(message.occurredAt).isEqualTo(Instant.parse("2026-10-18T15:00:00Z"))
    }

    @Test
    fun `브로커 ack를 받지 못하면 예외를 던져 아웃박스가 실패로 처리되게 한다`() {
        every { kafkaTemplate.send(any<String>(), any<String>(), any<String>()) } returns
            CompletableFuture.failedFuture(RuntimeException("broker down"))

        assertThrows<ExecutionException> { delivery.deliver(outboxEvent) {} }
    }
}
