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
        "update OutboxEvent oe set oe.status = :processing, oe.processingStartedDate = :now " +
            "where oe.id = :id and oe.status in (:pending, :failed, :resubmitted) and oe.completionAttempts < :max"
    )
    fun updateProcessingOutbox(
        id: String,
        processing: OutboxStatus = OutboxStatus.PROCESSING,
        pending: OutboxStatus = OutboxStatus.PENDING,
        failed: OutboxStatus = OutboxStatus.FAILED,
        resubmitted: OutboxStatus = OutboxStatus.RESUBMITTED,
        now: LocalDateTime = LocalDateTime.now(),
        max: Int = MAX_ATTEMPTS,
    ): Int

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        "update OutboxEvent oe " +
            "set oe.completionAttempts = oe.completionAttempts + 1, oe.lastResubmissionDate = :now, oe.status = :status, oe.lastErrorMessage = :errorMessage " +
            "where oe.id = :id"
    )
    fun updateFailedOutbox(
        id: String,
        errorMessage: String,
        now: LocalDateTime = LocalDateTime.now(),
        status: OutboxStatus = OutboxStatus.FAILED,
    ): Int

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update OutboxEvent oe set oe.status = :resubmitted where oe.status = :status and oe.processingStartedDate < :cutoff")
    fun updateAllResubmittedOutbox(
        cutoff: LocalDateTime,
        status: OutboxStatus = OutboxStatus.PROCESSING,
        resubmitted: OutboxStatus = OutboxStatus.RESUBMITTED,
    ): Int

    @Query("select oe from OutboxEvent oe where oe.status in (:pending, :failed, :resubmitted) and oe.completionAttempts < :max")
    fun findPollerEvents(
        pending: OutboxStatus = OutboxStatus.PENDING,
        failed: OutboxStatus = OutboxStatus.FAILED,
        resubmitted: OutboxStatus = OutboxStatus.RESUBMITTED,
        max: Int = MAX_ATTEMPTS,
    ): List<OutboxEvent>

    fun deleteOutboxEventById(id: String): Int

    companion object {
        const val MAX_ATTEMPTS = 5
    }
}
