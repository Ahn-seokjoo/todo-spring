package com.seokjoo.todo.domain.service.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.domain.entity.outbox.OutboxEvent
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.email.EmailService
import com.seokjoo.todo.domain.service.outbox.service.OutboxEventProcessor
import com.seokjoo.todo.domain.service.outbox.service.OutboxService
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class OutboxEventProcessorTest : BehaviorSpec({
    val authService: TodoAuthService = mockk()
    val objectMapper: ObjectMapper = mockk()
    val mailService: EmailService = mockk()
    val outboxService: OutboxService = mockk()

    val processor = OutboxEventProcessor(authService, objectMapper, mailService, outboxService)

    Given("processor가 수행됐을 때") {
        val onLog: (String) -> Unit = mockk(relaxed = true)
        val onFailure: (Throwable) -> Unit = mockk(relaxed = true)

        When("ClaimFailure.Processing 가 들어오면") {
            every { outboxService.claimOutbox(any()) } returns ClaimFailure.Processing
            Then("log에 Already Processing Event failure 메시지가 있다.") {
                processor.process("99", onFailure = onFailure, onLog = onLog)
                verify(exactly = 1) { onLog("Already Processing Event failure") }
                verify(exactly = 0) { onFailure(any()) }
            }
        }

        When("ClaimFailure.CheckDLQ 가 들어오면") {
            val buyer = User(userId = "pita2", password = "pw", email = "buyer@test.com")
            val sellerWithoutEmail = User(userId = "pita1", password = "pw", email = null)
            every { objectMapper.writeValueAsString(any()) } returns PublishableEvent.PurchaseRequestEvent(
                sellerId = sellerWithoutEmail.userId,
                buyerId = buyer.userId,
                todoId = 1L,
                price = 100L,
                purchaseId = 1L
            ).toString()

            val outboxEvent = OutboxEvent(
                listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
                eventType = OutboxEventType.PURCHASE_REQUEST,
                serializedEvent = objectMapper.writeValueAsString(""),
                publicationDate = LocalDateTime.now(),
            ).apply {
                id = "99"
            }

            every { outboxService.claimOutbox(any()) } returns ClaimFailure.CheckDLQ(outboxEvent)
            Then("updateOutboxDLQ 가 1회 호출되고 onLog 가 호출되며 메시지는 Check DLQ failure 이다.") {
                processor.process("99", onFailure = onFailure, onLog = onLog)
                verify(exactly = 1) { outboxService.updateOutboxDLQ(eq(outboxEvent)) }
                verify(exactly = 1) { onLog("Check DLQ failure") }
                verify(exactly = 0) { onFailure(any()) }
            }
        }
    }
}
)
