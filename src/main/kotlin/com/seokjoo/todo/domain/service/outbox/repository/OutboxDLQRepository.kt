package com.seokjoo.todo.domain.service.outbox.repository

import com.seokjoo.todo.domain.entity.email.EmailSentStatus
import com.seokjoo.todo.domain.entity.outbox.OutboxEventDLQ
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface OutboxDLQRepository : JpaRepository<OutboxEventDLQ, String> {

    @Query(
        "select dlq from OutboxEventDLQ dlq" +
            " where exists (" +
            "   select 1 from EmailSentRecord email" +
            "   where email.entityId = dlq.entityId and email.status = :sent" +
            " )"
    )
    fun findConfirmedSentDlqEvents(sent: EmailSentStatus = EmailSentStatus.SENT): List<OutboxEventDLQ>
}
