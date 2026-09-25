package com.seokjoo.todo.domain.service.outbox.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.seokjoo.todo.domain.entity.outbox.OutboxEvent
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.email.EmailService
import com.seokjoo.todo.domain.service.outbox.OutboxEventType
import com.seokjoo.todo.domain.service.outbox.OutboxListenerType
import com.seokjoo.todo.domain.service.outbox.PublishableEvent
import com.seokjoo.todo.domain.service.outbox.listener.event.TodoPurchaseEmailEvent
import com.seokjoo.todo.domain.service.outbox.service.OutboxService
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime

class TodoPurchaseEmailEventListenerTest : BehaviorSpec({
    val outboxService: OutboxService = mockk()
    val authService: TodoAuthService = mockk()
    val mailService: EmailService = mockk()
    val objectMapper: ObjectMapper = jacksonObjectMapper()

    val listener = TodoPurchaseEmailEventListener(
        outboxService = outboxService,
        authService = authService,
        objectMapper = objectMapper,
        mailService = mailService,
    )

    val seller = User(userId = "pita1", password = "pw", email = "seller@test.com")
    val buyer = User(userId = "pita2", password = "pw", email = "buyer@test.com")

    fun outboxEventOf(eventType: OutboxEventType, event: PublishableEvent): OutboxEvent {
        return OutboxEvent(
            listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
            eventType = eventType,
            serializedEvent = objectMapper.writeValueAsString(event),
            publicationDate = LocalDateTime.now(),
        ).also { it.id = "outbox-${eventType.name}" }
    }

    data class Case(
        val description: String,
        val eventType: OutboxEventType,
        val event: PublishableEvent,
        val expectedRecipient: User,
    )

    val cases = listOf(
        Case(
            description = "구매 요청 시 판매자에게 발송된다",
            eventType = OutboxEventType.PURCHASE_REQUEST,
            event = PublishableEvent.PurchaseRequestEvent(
                sellerId = seller.userId, buyerId = buyer.userId, todoId = 1L, price = 100L, purchaseId = 1L
            ),
            expectedRecipient = seller,
        ),
        Case(
            description = "구매 승인 시 구매자에게 발송된다",
            eventType = OutboxEventType.PURCHASE_APPROVED,
            event = PublishableEvent.PurchaseApprovedEvent(
                sellerId = seller.userId, buyerId = buyer.userId, todoId = 1L, price = 100L, purchaseId = 1L
            ),
            expectedRecipient = buyer,
        ),
        Case(
            description = "구매 거절 시 구매자에게 발송된다",
            eventType = OutboxEventType.PURCHASE_REJECTED,
            event = PublishableEvent.PurchaseRejectedEvent(
                sellerId = seller.userId, buyerId = buyer.userId, todoId = 1L, price = 100L, purchaseId = 1L
            ),
            expectedRecipient = buyer,
        ),
    )

    cases.forEach { case ->
        Given(case.description) {
            val outboxEvent = outboxEventOf(case.eventType, case.event)

            every { outboxService.getOutbox(any()) } returns outboxEvent
            every { authService.findUserByUserId(seller.userId) } returns seller
            every { authService.findUserByUserId(buyer.userId) } returns buyer
            every { outboxService.updateOutboxSuccess(any()) } just Runs
            every { outboxService.updateOutboxFail(any()) } just Runs

            val toSlot = slot<String>()
            every { mailService.sendEmail(to = capture(toSlot), subject = any(), content = any()) } just Runs

            When("리스너가 이벤트를 처리하면") {
                listener.eventListener(TodoPurchaseEmailEvent(outboxEventId = "dummy-outbox-id"))

                Then("올바른 수신자에게 메일이 발송되고 outbox가 성공 처리된다") {
                    toSlot.captured shouldBe case.expectedRecipient.email
                    verify(exactly = 1) { outboxService.updateOutboxSuccess(outboxEvent) }
                    verify(exactly = 0) { outboxService.updateOutboxFail(any()) }
                }
            }
        }
    }

    Given("판매자의 이메일이 등록되어 있지 않은 경우") {
        val sellerWithoutEmail = User(userId = "pita1", password = "pw", email = null)
        val purchaseRequestEvent = PublishableEvent.PurchaseRequestEvent(
            sellerId = sellerWithoutEmail.userId, buyerId = buyer.userId, todoId = 1L, price = 100L, purchaseId = 1L
        )
        val outboxEvent = outboxEventOf(OutboxEventType.PURCHASE_REQUEST, purchaseRequestEvent)

        every { outboxService.getOutbox(any()) } returns outboxEvent
        every { authService.findUserByUserId(sellerWithoutEmail.userId) } returns sellerWithoutEmail
        every { outboxService.updateOutboxSuccess(any()) } just Runs
        every { outboxService.updateOutboxFail(any()) } just Runs

        When("리스너가 이벤트를 처리하면") {
            listener.eventListener(TodoPurchaseEmailEvent(outboxEventId = "dummy-outbox-id"))

            Then("메일 발송을 시도하지 않고 outbox가 실패 처리된다") {
                verify(exactly = 0) { mailService.sendEmail(any(), any(), any()) }
                verify(exactly = 1) { outboxService.updateOutboxFail(id = outboxEvent.id) }
                verify(exactly = 0) { outboxService.updateOutboxSuccess(any()) }
            }
        }
    }
}) {
    // Kotest BehaviorSpec은 기본적으로 스펙 전체(모든 Given/When/Then)를 하나의 인스턴스에서 실행해서,
    // 위에서 선언한 mockk()들이 모든 케이스에 걸쳐 공유되고 호출 이력도 계속 누적됨.
    // "몇 번 호출됐는지"를 검증하는 케이스(특히 verify(exactly = 0))가 있는 여기서는
    // 케이스(leaf test)마다 완전히 새 인스턴스로 스펙을 다시 실행해서 mock을 매번 초기화해야 함.
    override fun isolationMode(): IsolationMode = IsolationMode.InstancePerLeaf
}
