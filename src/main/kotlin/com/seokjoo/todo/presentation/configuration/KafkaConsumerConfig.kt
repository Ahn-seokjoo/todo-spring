package com.seokjoo.todo.presentation.configuration

import com.fasterxml.jackson.core.JsonProcessingException
import com.seokjoo.todo.domain.service.outbox.service.consumer.UnconfirmedSendException
import com.seokjoo.todo.presentation.configuration.KafkaTopics.PURCHASE_EVENTS_DLT
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.TopicPartition
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

@Configuration
@ConditionalOnProperty(prefix = "outbox", name = ["delivery-mode"], havingValue = "kafka")
class KafkaConsumerConfig {

    @Bean
    fun purchaseEventDltTopic(): NewTopic {
        return TopicBuilder.name(PURCHASE_EVENTS_DLT)
            .partitions(3)
            .replicas(1)
            .build()
    }

    @Bean
    fun kafkaErrorHandler(kafkaTemplate: KafkaTemplate<String, String>): DefaultErrorHandler {
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate) { record, _ ->
            TopicPartition(PURCHASE_EVENTS_DLT, record.partition())
        }
        return DefaultErrorHandler(
            recoverer,
            FixedBackOff(1000L, 2L), // 1초 쉬고 최대 2번 더 (총 3회)
        ).apply {
            addNotRetryableExceptions(
                UnconfirmedSendException::class.java, // 몇번 다시 시도해도 READY 라서 재시도 안함
                JsonProcessingException::class.java,                   // 메시지 자체가 깨진 경우
            )
        }
    }
}
