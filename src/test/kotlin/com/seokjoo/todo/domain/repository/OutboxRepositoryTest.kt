package com.seokjoo.todo.domain.repository

import com.seokjoo.todo.annotation.TodoTest
import com.seokjoo.todo.domain.entity.outbox.OutboxEvent
import com.seokjoo.todo.domain.entity.outbox.OutboxStatus
import com.seokjoo.todo.domain.service.outbox.OutboxEventType
import com.seokjoo.todo.domain.service.outbox.OutboxListenerType
import com.seokjoo.todo.domain.service.outbox.repository.OutboxRepository
import com.seokjoo.todo.domain.service.outbox.repository.OutboxRepository.Companion.MAX_ATTEMPTS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@TodoTest
@Transactional
class OutboxRepositoryTest @Autowired constructor(
    private val outboxRepository: OutboxRepository,
) {
    @ParameterizedTest
    @EnumSource(value = OutboxStatus::class, names = ["PENDING", "FAILED", "RESUBMITTED"])
    fun `PENDING, FAILED, RESUBMMITED 상태인 row는 claim에 성공한다`(status: OutboxStatus) {
        val event = outboxRepository.save(
            OutboxEvent(
                listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
                eventType = OutboxEventType.PURCHASE_REQUEST,
                serializedEvent = "dummy",
                publicationDate = LocalDateTime.now(),
                status = status,
            )
        )

        val effectedCount = outboxRepository.updateProcessingOutbox(id = event.id)

        assertThat(effectedCount).isEqualTo(1)
        val updated = outboxRepository.findByIdOrNull(event.id)
        assertThat(updated?.status).isEqualTo(OutboxStatus.PROCESSING)
        assertThat(updated?.processingStartedDate).isNotNull
    }

    @Test
    fun `completionAttempts가 max 보다 작다에 매칭되지 않으면 업데이트 하지 않는다`() {
        val event = outboxRepository.save(
            OutboxEvent(
                listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
                eventType = OutboxEventType.PURCHASE_REQUEST,
                serializedEvent = "dummy",
                publicationDate = LocalDateTime.now(),
                completionAttempts = MAX_ATTEMPTS,
                status = OutboxStatus.PENDING,
            )
        )

        val effectedCount = outboxRepository.updateProcessingOutbox(id = event.id)

        assertThat(effectedCount).isEqualTo(0)
        val updated = outboxRepository.findByIdOrNull(event.id)
        assertThat(updated?.status).isEqualTo(OutboxStatus.PENDING)
        assertThat(updated?.processingStartedDate).isNull()
    }

    @Test
    fun `completionAttempts가 max 보다 1 작은 값은 성공한다 (경계값 테스트)`() {
        val event = outboxRepository.save(
            OutboxEvent(
                listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
                eventType = OutboxEventType.PURCHASE_REQUEST,
                serializedEvent = "dummy",
                publicationDate = LocalDateTime.now(),
                completionAttempts = MAX_ATTEMPTS - 1,
                status = OutboxStatus.PENDING,
            )
        )

        val effectedCount = outboxRepository.updateProcessingOutbox(id = event.id)

        assertThat(effectedCount).isEqualTo(1)
        val updated = outboxRepository.findByIdOrNull(event.id)
        assertThat(updated?.status).isEqualTo(OutboxStatus.PROCESSING)
        assertThat(updated?.processingStartedDate).isNotNull
    }

    @Test
    fun `completionAttempts가 Processing 중이라면 폴러가 재수행 했을 때 effectedCount 는 0이다`() {
        val event = outboxRepository.save(
            OutboxEvent(
                listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
                eventType = OutboxEventType.PURCHASE_REQUEST,
                serializedEvent = "dummy",
                publicationDate = LocalDateTime.now(),
                completionAttempts = MAX_ATTEMPTS - 1,
                status = OutboxStatus.PENDING,
            )
        )
        // 선제적으로 먼저 수행 하여 PROCESSING 으로 바꿔둠
        outboxRepository.updateProcessingOutbox(id = event.id)
        val first = outboxRepository.findByIdOrNull(event.id)
        assertThat(first?.status).isEqualTo(OutboxStatus.PROCESSING)

        val effectedCount = outboxRepository.updateProcessingOutbox(id = event.id)

        assertThat(effectedCount).isEqualTo(0)
        val updated = outboxRepository.findByIdOrNull(event.id)
        assertThat(updated?.status).isEqualTo(OutboxStatus.PROCESSING)
        assertThat(updated?.processingStartedDate).isNotNull
    }

    @Test
    fun `failed 를 세팅하면서 시도 횟수는 1회 증가하고, 메시지와 시간이 같이 저장된다`() {
        val event = outboxRepository.save(
            OutboxEvent(
                listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
                eventType = OutboxEventType.PURCHASE_REQUEST,
                serializedEvent = "dummy",
                publicationDate = LocalDateTime.now(),
                status = OutboxStatus.FAILED,
            )
        )
        val errorMessage = "error-message"
        val effectedCount = outboxRepository.updateFailedOutbox(id = event.id, errorMessage = errorMessage)

        assertThat(effectedCount).isEqualTo(1)
        val updated = outboxRepository.findByIdOrNull(event.id)
        assertThat(updated?.lastResubmissionDate).isNotNull
        assertThat(updated?.completionAttempts).isEqualTo(1)
        assertThat(updated?.status).isEqualTo(OutboxStatus.FAILED)
        assertThat(updated?.lastErrorMessage).isEqualTo(errorMessage)

        val secondsEffectedCount = outboxRepository.updateFailedOutbox(id = event.id, errorMessage = errorMessage)
        assertThat(secondsEffectedCount).isEqualTo(1)
        val secondsUpdated = outboxRepository.findByIdOrNull(event.id)
        assertThat(secondsUpdated?.completionAttempts).isEqualTo(2)
    }

    @Test
    fun `updateAllResubmittedOutbox 를 수행하면, PROCESSING 이던 status가 Resubmitted 으로 변경된다`() {
        val time = LocalDateTime.now()
        val event = outboxRepository.save(
            OutboxEvent(
                listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
                eventType = OutboxEventType.PURCHASE_REQUEST,
                serializedEvent = "dummy",
                publicationDate = time,
                processingStartedDate = time,
                status = OutboxStatus.PROCESSING,
            )
        )

        val result = outboxRepository.updateAllResubmittedOutbox(cutoff = time.plusMinutes(1))

        assertThat(result).isEqualTo(1)
        val updated = outboxRepository.findByIdOrNull(event.id)
        assertThat(updated?.status).isEqualTo(OutboxStatus.RESUBMITTED)
    }

    /**
     * 컷오프는 poller가 오래된 processing을 업데이트해주는 용도이기에 threshold를 넘지 않은건 처리중이라 보고 업데이트해주지 않는다
     */
    @Test
    fun `cutoff 보다 작지 않으면 업데이트는 일어나지 않는다`() {
        val time = LocalDateTime.now()
        val event = outboxRepository.save(
            OutboxEvent(
                listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
                eventType = OutboxEventType.PURCHASE_REQUEST,
                serializedEvent = "dummy",
                publicationDate = time,
                processingStartedDate = time,
                status = OutboxStatus.PROCESSING,
            )
        )

        val result = outboxRepository.updateAllResubmittedOutbox(cutoff = time)

        assertThat(result).isEqualTo(0)
        val updated = outboxRepository.findByIdOrNull(event.id)
        assertThat(updated?.status).isEqualTo(OutboxStatus.PROCESSING)
    }

    @Test
    fun `status가 PROCESSING이 아니면 cutoff를 지나도 업데이트되지 않는다`() {
        val oldTime = LocalDateTime.now().minusMinutes(10)
        val event = outboxRepository.save(
            OutboxEvent(
                listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
                eventType = OutboxEventType.PURCHASE_REQUEST,
                serializedEvent = "dummy",
                publicationDate = oldTime,
                processingStartedDate = oldTime,
                status = OutboxStatus.FAILED, // PROCESSING이 아님
            )
        )

        val result = outboxRepository.updateAllResubmittedOutbox(cutoff = LocalDateTime.now())
        // status는 안 넘김 → 기본값 PROCESSING으로 필터링됨

        assertThat(result).isEqualTo(0)
        val updated = outboxRepository.findByIdOrNull(event.id)
        assertThat(updated?.status).isEqualTo(OutboxStatus.FAILED)
    }

    @Test
    fun `findPollerEvents 를 수행했을 때 pending, failed, resubmitted만 가져온다`() {
        // given
        val pending = outboxRepository.save(outboxEventOf(status = OutboxStatus.PENDING))
        val failed = outboxRepository.save(outboxEventOf(status = OutboxStatus.FAILED))
        val resubmitted = outboxRepository.save(outboxEventOf(status = OutboxStatus.RESUBMITTED))
        val processing =
            outboxRepository.save(outboxEventOf(status = OutboxStatus.PROCESSING)) // 제외되는 것도 같이 넣어야 필터링 검증됨

        // when
        val result = outboxRepository.findPollerEvents()

        // then
        assertThat(result.map { it.id })
            .containsExactlyInAnyOrder(pending.id, failed.id, resubmitted.id)
        assertThat(result.map { it.status })
            .containsExactlyInAnyOrder(OutboxStatus.PENDING, OutboxStatus.FAILED, OutboxStatus.RESUBMITTED)
    }

    @Test
    fun `deleteOutboxEventById 테스트`() {
        val outboxEvent = outboxRepository.save(outboxEventOf(status = OutboxStatus.PENDING))

        val effectedCount = outboxRepository.deleteOutboxEventById(outboxEvent.id)

        assertThat(effectedCount).isEqualTo(1)
        assertThat(outboxRepository.findByIdOrNull(outboxEvent.id)).isNull()
    }

    fun outboxEventOf(
        completionAttempts: Int = 0,
        status: OutboxStatus = OutboxStatus.PENDING,
    ): OutboxEvent {
        return OutboxEvent(
            listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
            eventType = OutboxEventType.PURCHASE_REQUEST,
            serializedEvent = "dummy",
            publicationDate = LocalDateTime.now(),
            completionAttempts = completionAttempts,
            status = status,
        )
    }
}
