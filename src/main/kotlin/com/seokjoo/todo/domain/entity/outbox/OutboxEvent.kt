package com.seokjoo.todo.domain.entity.outbox

import com.seokjoo.todo.domain.service.outbox.OutboxEventType
import com.seokjoo.todo.domain.service.outbox.OutboxListenerType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "outbox_event")
class OutboxEvent(
    @Enumerated(EnumType.STRING)
    @Column(name = "listener_type", nullable = false, length = 50)
    val listenerType: OutboxListenerType,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    val eventType: OutboxEventType,

    // 원본 json
    @Column(name = "serialized_event", nullable = false)
    val serializedEvent: String,

    // timestamp
    @Column(name = "publication_date", nullable = false)
    val publicationDate: LocalDateTime,

    @Column(name = "completion_date")
    val completionDate: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: OutboxStatus = OutboxStatus.PENDING,

    @Column(name = "completion_attempts", nullable = false)
    val completionAttempts: Int = 0,

    @Column(name = "last_resubmission_date")
    val lastResubmissionDate: LocalDateTime? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: String
}
