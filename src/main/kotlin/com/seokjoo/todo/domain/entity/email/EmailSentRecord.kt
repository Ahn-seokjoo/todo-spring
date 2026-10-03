package com.seokjoo.todo.domain.entity.email

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
@Table(name = "email_sent_record")
class EmailSentRecord(
    // 기존 Outbox Event의 id를 물려받음
    id: String,

    @Column(name = "sent_at", nullable = true)
    val sentAt: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: EmailSentStatus,
) : Persistable<String> {

    // delete 쿼리(TodoEmailRepository.deleteByEmailId)에서 email.entityId로 참조하기 위해 public으로 둔다.
    @Id
    @Column(name = "id")
    val entityId: String = id

    @Transient
    private var isNewEntity: Boolean = true

    override fun isNew(): Boolean = isNewEntity
    override fun getId(): String = entityId

    @PostPersist
    @PostLoad
    fun markNotNew() {
        isNewEntity = false
    }
}
