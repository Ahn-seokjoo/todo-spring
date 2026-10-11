package com.seokjoo.todo.domain.service.outbox.service.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.domain.entity.email.EmailSentStatus
import com.seokjoo.todo.domain.service.email.EmailService
import com.seokjoo.todo.domain.service.outbox.delivery.PurchaseEventMessage
import com.seokjoo.todo.domain.service.outbox.service.email.PurchaseEmailSender
import com.seokjoo.todo.presentation.configuration.KafkaTopics
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "outbox", name = ["delivery-mode"], havingValue = "kafka")
class PurchaseEmailConsumer(
    private val objectMapper: ObjectMapper,
    private val sender: PurchaseEmailSender,
    private val mailService: EmailService,
) {

    private val logger = LoggerFactory.getLogger(PurchaseEmailConsumer::class.java)

    @KafkaListener(topics = [KafkaTopics.PURCHASE_EVENT], groupId = "email-notifier")
    fun consume(record: ConsumerRecord<String, String>, ack: Acknowledgment) {
        val message = objectMapper.readValue(record.value(), PurchaseEventMessage::class.java)
        try {
            sender.send(
                eventId = message.eventId,
                eventType = message.eventType,
                serializedEvent = message.payload,
                onLog = logger::warn,
            )
        } catch (e: DataIntegrityViolationException) {
            when (mailService.findStatusByEmailId(emailId = message.eventId)) {
                EmailSentStatus.SENT -> logger.info("이미 발송된 이벤트, 건너뜀 : ${message.eventId}")
                EmailSentStatus.READY -> {
                    throw UnconfirmedSendException(eventId = message.eventId)
                }

                null -> throw e
            }
        }
        ack.acknowledge()
    }
}

class UnconfirmedSendException(eventId: String) : RuntimeException("이전 발송 시대고 결과 미확정(READY) 상태 : $eventId")
