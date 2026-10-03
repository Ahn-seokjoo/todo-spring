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

    // 발송 자체(sendEmail)는 성공했지만 그 뒤 markSentAt 기록만 실패해서 READY로 멈춘 레코드를 위한 정합화 쿼리.
    // email.entityId가 outbox_event_archive에 존재한다는 건 그 outbox_event가 이미 SUCCESS로 종결됐다는 뜻으로, sent로 변경
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        "update EmailSentRecord email set email.status = :sent, email.sentAt = :sentAt" +
            " where email.status = :ready" +
            " and email.entityId in (select archive.entityId from OutboxEventArchive archive)"
    )
    fun reconcileConfirmedSentRecords(
        sentAt: LocalDateTime = LocalDateTime.now(),
        ready: EmailSentStatus = EmailSentStatus.READY,
        sent: EmailSentStatus = EmailSentStatus.SENT,
    ): Int
}
