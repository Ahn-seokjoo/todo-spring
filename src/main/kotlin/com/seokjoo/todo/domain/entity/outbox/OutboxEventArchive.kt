package com.seokjoo.todo.domain.entity.outbox

import com.seokjoo.todo.domain.service.outbox.OutboxEventType
import com.seokjoo.todo.domain.service.outbox.OutboxListenerType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

@Entity
@Table(name = "outbox_event_archive")
class OutboxEventArchive(
    // 기존 Outbox Event의 id를 물려받음
    id: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "listener_type", nullable = false, length = 50)
    val listenerType: OutboxListenerType,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    val eventType: OutboxEventType,

    // 원본 json
    @Column(name = "serialized_event", nullable = false, columnDefinition = "json")
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
) : Persistable<String> {

    @Id
    @Column(name = "id")
    private val entityId: String = id

    @Transient
    private var isNewEntity: Boolean = true

    override fun isNew(): Boolean = isNewEntity
    override fun getId(): String = entityId

    @PostPersist
    @PostLoad
    fun markNotNew() {
        isNewEntity = false
    }

    companion object {
        fun from(outboxEvent: OutboxEvent): OutboxEventArchive {
            return OutboxEventArchive(
                id = outboxEvent.id,
                listenerType = outboxEvent.listenerType,
                eventType = outboxEvent.eventType,
                serializedEvent = outboxEvent.serializedEvent,
                publicationDate = outboxEvent.publicationDate,
                completionDate = LocalDateTime.now(),
                status = OutboxStatus.SUCCESS,
                completionAttempts = outboxEvent.completionAttempts,
                lastResubmissionDate = outboxEvent.lastResubmissionDate,
            )
        }
    }
}
