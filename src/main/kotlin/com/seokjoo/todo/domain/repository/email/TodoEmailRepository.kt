package com.seokjoo.todo.domain.repository.email

import com.seokjoo.todo.domain.entity.email.EmailSentRecord
import com.seokjoo.todo.domain.entity.email.EmailSentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface TodoEmailRepository : JpaRepository<EmailSentRecord, String> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from EmailSentRecord email where email.entityId = :emailId")
    fun deleteByEmailId(emailId: String): Int

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        "update EmailSentRecord email set email.status = :sent, email.sentAt = :sentAt" +
            " where email.entityId = :emailId and email.status = :ready"
    )
    fun updateSent(
        emailId: String,
        sentAt: LocalDateTime = LocalDateTime.now(),
        ready: EmailSentStatus = EmailSentStatus.READY,
        sent: EmailSentStatus = EmailSentStatus.SENT,
    ): Int

    @Query(value = "select email.status from EmailSentRecord email where email.entityId = :emailId")
    fun findStatusByEmailId(emailId: String): EmailSentStatus?
}
