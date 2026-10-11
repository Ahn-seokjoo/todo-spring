package com.seokjoo.todo.presentation.configuration

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
@ConditionalOnProperty(prefix = "outbox", name = ["delivery-mode"], havingValue = "kafka")
class KafkaTopicConfig {
    @Bean
    fun purchaseEventsTopic() = TopicBuilder.name(KafkaTopics.PURCHASE_EVENT)
        .partitions(3)
        .replicas(1)
        .build()
}

object KafkaTopics {
    const val PURCHASE_EVENT = "todo.purchase.events"
}
