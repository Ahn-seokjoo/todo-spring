package com.seokjoo.todo.domain.service.outbox.consumer

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.seokjoo.todo.domain.service.outbox.OutboxEventType
import com.seokjoo.todo.domain.service.outbox.PublishableEvent
import com.seokjoo.todo.domain.service.outbox.delivery.PurchaseEventMessage
import com.seokjoo.todo.domain.service.outbox.service.consumer.SalesStatsConsumer
import com.seokjoo.todo.domain.service.sales.TodoSalesStatsRecordService
import com.seokjoo.todo.presentation.configuration.KafkaTopics
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.record.TimestampType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Instant
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class SalesStatsConsumerTest {
    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val recordService = mockk<TodoSalesStatsRecordService>()
    private val consumer = SalesStatsConsumer(objectMapper, recordService)   // 진짜 컨슈머 + 가짜 서비스

    @Test
    fun `occurredAt은 KST 날짜로 변환해 기록한다 - UTC 15시는 KST 다음날`() {
        every { recordService.record(any(), any(), any(), any()) } returns true
        val message = message(occurredAt = Instant.parse("2026-10-19T15:00:00Z"), OutboxEventType.PURCHASE_APPROVED)

        consumer.consume(
            ConsumerRecord(
                KafkaTopics.PURCHASE_EVENT,
                0,
                0L,
                "1",
                objectMapper.writeValueAsString(message)
            )
        )

        verify(exactly = 1) {
            recordService.record(
                eventId = message.eventId,
                amount = 100L,
                sellerId = "s",
                salesDate = LocalDate.of(2026, 10, 20)
            )
        }
    }

    @ParameterizedTest
    @EnumSource(value = OutboxEventType::class, names = ["PURCHASE_APPROVED"], mode = EnumSource.Mode.EXCLUDE)
    fun `PURCHASE_APPROVED가 아니라면 record가 호출되지 않는다`(eventType: OutboxEventType) {
        every { recordService.record(any(), any(), any(), any()) } returns true
        val message = message(occurredAt = Instant.parse("2026-10-19T15:00:00Z"), eventType)

        consumer.consume(
            ConsumerRecord(
                KafkaTopics.PURCHASE_EVENT,
                0,
                0L,
                "1",
                objectMapper.writeValueAsString(message)
            )
        )
        verify(exactly = 0) { recordService.record(any(), any(), any(), any()) }
    }

    @Test
    fun `occurredAt이 없으면 레코드 timestamp를 KST 날짜로 변환해 기록한다`() {
        every { recordService.record(any(), any(), any(), any()) } returns true
        val message = message(occurredAt = null, OutboxEventType.PURCHASE_APPROVED)
        val recordTimestamp = Instant.parse("2026-10-19T15:00:00Z").toEpochMilli() // KST 2026-10-20 00:00

        // timestamp를 지정하려면 긴 생성자를 써야 한다 (짧은 생성자는 timestamp = -1)
        consumer.consume(
            ConsumerRecord(
                KafkaTopics.PURCHASE_EVENT,
                0,
                0L,
                recordTimestamp,
                TimestampType.CREATE_TIME,
                0,
                0,
                "1",
                objectMapper.writeValueAsString(message),
                RecordHeaders(),
                Optional.empty(),
            )
        )

        verify(exactly = 1) {
            recordService.record(
                eventId = message.eventId,
                amount = 100L,
                sellerId = "s",
                salesDate = LocalDate.of(2026, 10, 20)
            )
        }
    }

    private fun message(occurredAt: Instant?, eventType: OutboxEventType) = PurchaseEventMessage(
        eventId = UUID.randomUUID().toString(),
        eventType = eventType,
        payload = objectMapper.writeValueAsString(
            PublishableEvent.PurchaseApprovedEvent(
                sellerId = "s",
                buyerId = "b",
                todoId = 1L,
                price = 100L,
                purchaseId = 1L
            )
        ),
        occurredAt = occurredAt,
    )
}
