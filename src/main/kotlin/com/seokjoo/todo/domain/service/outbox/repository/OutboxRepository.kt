package com.seokjoo.todo.domain.service.outbox.repository

import com.seokjoo.todo.domain.entity.outbox.OutboxEvent
import com.seokjoo.todo.domain.entity.outbox.OutboxStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface OutboxRepository : JpaRepository<OutboxEvent, String> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        "update OutboxEvent oe set oe.status = :processing " +
            "where oe.id = :id and oe.status in (:pending, :failed) and oe.completionAttempts < :max"
    )
    fun updateProcessingOutbox(
        id: String,
        processing: OutboxStatus = OutboxStatus.PROCESSING,
        pending: OutboxStatus = OutboxStatus.PENDING,
        failed: OutboxStatus = OutboxStatus.FAILED,
        max: Int = MAX_ATTEMPTS,
    ): Int

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        "update OutboxEvent oe " +
            "set oe.completionAttempts = oe.completionAttempts + 1, oe.lastResubmissionDate = :now, oe.status = :status " +
            "where oe.id = :id"
    )
    fun updateFailedOutbox(
        id: String,
        now: LocalDateTime = LocalDateTime.now(),
        status: OutboxStatus = OutboxStatus.FAILED,
    ): Int

    fun deleteOutboxEventById(id: String): Int

    companion object {
        private const val MAX_ATTEMPTS = 5
    }
}
