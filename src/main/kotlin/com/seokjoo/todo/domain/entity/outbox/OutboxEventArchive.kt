package com.seokjoo.todo.domain.entity.outbox

import com.seokjoo.todo.domain.service.outbox.OutboxEventType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "outbox_event_archive")
class OutboxEventArchive(
    // 기존 Outbox Event의 id를 물려받음
    @Id
    val id: String,

    @Column(name = "listener_type", nullable = false, length = 50)
    val listenerType: String,

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
    val status: OutboxStatus,

    @Column(name = "completion_attempts", nullable = false)
    val completionAttempts: Int = 0,

    @Column(name = "last_resubmission_date")
    val lastResubmissionDate: LocalDateTime? = null,
)
