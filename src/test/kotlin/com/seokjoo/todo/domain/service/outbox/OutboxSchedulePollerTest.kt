package com.seokjoo.todo.domain.service.outbox

import com.seokjoo.todo.domain.entity.outbox.OutboxEvent
import com.seokjoo.todo.domain.service.outbox.service.OutboxEventProcessor
import com.seokjoo.todo.domain.service.outbox.service.OutboxSchedulePoller
import com.seokjoo.todo.domain.service.outbox.service.OutboxService
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.time.LocalDateTime

class OutboxSchedulePollerTest : BehaviorSpec({
    val outboxService: OutboxService = mockk()
    val processor: OutboxEventProcessor = mockk()
    val poller = OutboxSchedulePoller(outboxService, processor)

    fun eventOf(id: String) = OutboxEvent(
        listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
        eventType = OutboxEventType.PURCHASE_REQUEST,
        serializedEvent = "dummy",
        publicationDate = LocalDateTime.now(),
    ).apply { this.id = id }

    Given("process가 수행될 때") {
        every { outboxService.updateAllResubmittedOutbox() } just Runs

        When("findPollerEvents 호출 순서") {
            every { outboxService.findPollerEvents() } returns emptyList()

            Then("updateAllResubmittedOutbox가 findPollerEvents보다 먼저 호출된다") {
                poller.process()
                verifyOrder {
                    outboxService.updateAllResubmittedOutbox()
                    outboxService.findPollerEvents()
                }
            }
        }

        When("이벤트가 없으면") {
            every { outboxService.findPollerEvents() } returns emptyList()

            Then("processor는 호출되지 않는다") {
                poller.process()
                verify(exactly = 0) { processor.process(any(), any(), any()) }
            }
        }

        When("이벤트가 여러 개면") {
            val event1 = eventOf("1")
            val event2 = eventOf("2")
            every { outboxService.findPollerEvents() } returns listOf(event1, event2)
            every { processor.process(eventId = any(), onFailure = any(), onLog = any()) } just Runs

            Then("각 이벤트 id로 processor.process가 1번씩 호출된다") {
                poller.process()
                verify(exactly = 1) { processor.process(eventId = "1", onFailure = any(), onLog = any()) }
                verify(exactly = 1) { processor.process(eventId = "2", onFailure = any(), onLog = any()) }
            }
        }

        When("특정 이벤트 처리 중 예외가 발생해도") {
            val event1 = eventOf("1")
            val event2 = eventOf("2")
            every { outboxService.findPollerEvents() } returns listOf(event1, event2)
            every { processor.process(eventId = "1", onFailure = any(), onLog = any()) } throws RuntimeException("boom")
            every { processor.process(eventId = "2", onFailure = any(), onLog = any()) } just Runs

            Then("나머지 이벤트는 격리되어 계속 처리된다") {
                poller.process()
                verify(exactly = 1) { processor.process(eventId = "1", onFailure = any(), onLog = any()) }
                verify(exactly = 1) { processor.process(eventId = "2", onFailure = any(), onLog = any()) }
            }
        }
    }
}) {
    override fun isolationMode(): IsolationMode = IsolationMode.InstancePerLeaf
}
