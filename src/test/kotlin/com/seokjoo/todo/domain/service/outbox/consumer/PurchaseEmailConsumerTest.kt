package com.seokjoo.todo.domain.service.outbox.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.config.TestLockConfig
import com.seokjoo.todo.domain.entity.email.EmailSentStatus
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.email.EmailService
import com.seokjoo.todo.domain.service.outbox.OutboxEventType
import com.seokjoo.todo.domain.service.outbox.PublishableEvent
import com.seokjoo.todo.domain.service.outbox.delivery.PurchaseEventMessage
import com.seokjoo.todo.presentation.configuration.KafkaTopics
import org.apache.kafka.common.serialization.StringDeserializer
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.mockito.kotlin.after
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit

@SpringBootTest(
    properties = [
        "outbox.delivery-mode=kafka",
        "outbox.poller.enabled=false",
        "management.health.mail.enabled=false",
        "spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}",
    ]
)
@ActiveProfiles("test", "kafka")
@Import(TestLockConfig::class)
@EmbeddedKafka(partitions = 3, topics = [KafkaTopics.PURCHASE_EVENT, KafkaTopics.PURCHASE_EVENTS_DLT])
class PurchaseEmailConsumerTest @Autowired constructor(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val todoAuthService: TodoAuthService,
    private val mailService: EmailService,
    private val embeddedKafka: EmbeddedKafkaBroker,
) {
    @MockitoBean
    private lateinit var mailSender: JavaMailSender

    @Test
    fun `같은 eventId가 두 번 와도 메일은 한번만 보낸다`() {
        val message = purchaseRequestMessage()
        val json = objectMapper.writeValueAsString(message)

        kafkaTemplate.send(KafkaTopics.PURCHASE_EVENT, "1", json).get()
        kafkaTemplate.send(KafkaTopics.PURCHASE_EVENT, "1", json).get()

        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertThat(mailService.findStatusByEmailId(message.eventId)).isEqualTo(EmailSentStatus.SENT)
        }
        verify(mailSender, after(3000).times(1)).send(any<SimpleMailMessage>())
    }

    @Test
    fun `깨진 메시지는 메일 전송 없이 DLT로 이동한다`() {
        kafkaTemplate.send(KafkaTopics.PURCHASE_EVENT, "poison", "not-a-json").get()

        val props = KafkaTestUtils.consumerProps("dlt-test-group", "false", embeddedKafka)
        DefaultKafkaConsumerFactory(props, StringDeserializer(), StringDeserializer())
            .createConsumer().use { consumer ->
                embeddedKafka.consumeFromAnEmbeddedTopic(consumer, KafkaTopics.PURCHASE_EVENTS_DLT)

                val records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10), 2)   // 최소 2건 기다림
                    .records(KafkaTopics.PURCHASE_EVENTS_DLT)
                    .toList()

                assertThat(records).hasSize(2)
                assertThat(records.map { it.value() }).containsOnly("not-a-json")
                assertThat(records.map { String(it.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_CONSUMER_GROUP).value()) })
                    .containsExactlyInAnyOrder("email-notifier", "sales-stats")
            }
        verify(mailSender, never()).send(any<SimpleMailMessage>())
    }

    private fun purchaseRequestMessage(): PurchaseEventMessage {
        val suffix = UUID.randomUUID().toString().take(8)
        val sellerId = "kc-seller-$suffix"
        val buyerId = "kc-buyer-$suffix"
        todoAuthService.signUp(sellerId, "pita", email = "$sellerId@test.com")
        todoAuthService.signUp(buyerId, "pita", email = "$buyerId@test.com")

        val payload = objectMapper.writeValueAsString(
            PublishableEvent.PurchaseRequestEvent(
                sellerId = sellerId, buyerId = buyerId, todoId = 1L, price = 100L, purchaseId = 1L,
            )
        )
        return PurchaseEventMessage(
            eventId = UUID.randomUUID().toString(),
            eventType = OutboxEventType.PURCHASE_REQUEST,
            payload = payload,
        )
    }
}
