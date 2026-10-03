package com.seokjoo.todo.domain.service.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.email.EmailSentRecord
import com.seokjoo.todo.domain.entity.email.EmailSentStatus
import com.seokjoo.todo.domain.entity.outbox.OutboxEvent
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.email.EmailService
import com.seokjoo.todo.domain.service.outbox.service.OutboxEventProcessor
import com.seokjoo.todo.domain.service.outbox.service.OutboxService
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDateTime

class OutboxEventProcessorTest : BehaviorSpec({
    val authService: TodoAuthService = mockk()
    val objectMapper: ObjectMapper = jacksonObjectMapper()
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
            val outboxEvent = OutboxEvent(
                listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
                eventType = OutboxEventType.PURCHASE_REQUEST,
                serializedEvent = "dummy",
                publicationDate = LocalDateTime.now(),
            ).apply {
                id = "99"
            }

            every { outboxService.claimOutbox(any()) } returns ClaimFailure.CheckDLQ(outboxEvent)
            Then("updateOutboxDLQ 가 1회 호출되고 onLog 가 호출되며 메시지는 Check DLQ failure 이다.") {

                every { outboxService.updateOutboxDLQ(any()) } just Runs
                processor.process("99", onFailure = onFailure, onLog = onLog)
                verify(exactly = 1) { outboxService.updateOutboxDLQ(eq(outboxEvent)) }
                verify(exactly = 1) { onLog("Check DLQ failure") }
                verify(exactly = 0) { onFailure(any()) }
            }

            Then("updateOutboxDLQ 를 호출했는데 실패하는 경우 onFailure가 호출된다") {
                every { outboxService.updateOutboxDLQ(any()) } throws TodoException.of(TodoExceptionType.OUTBOX_UPDATE_ERROR)
                processor.process(outboxEvent.id, onFailure = onFailure, onLog = onLog)
                verify(exactly = 1) { onFailure(any()) }
            }
        }

        When("else 문으로 빠질 때 기본 success 로") {
            val outboxEvent = OutboxEvent(
                listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
                eventType = OutboxEventType.CACHE_EVICT_ALL,
                serializedEvent = "dummy",
                publicationDate = LocalDateTime.now(),
            ).apply {
                id = "99"
            }
            every { outboxService.claimOutbox(any()) } returns ClaimSuccess(outboxEvent)
            every { outboxService.updateOutboxSuccess(any()) } just Runs

            Then("메일은 보내지 않고, outbox success만 호출되는지 확인") {
                processor.process(outboxEvent.id, onFailure = onFailure, onLog = onLog)
                verify(exactly = 1) { outboxService.claimOutbox(any()) }
                verify(exactly = 1) { outboxService.updateOutboxSuccess(outboxEvent = outboxEvent) }
                verify(exactly = 0) { mailService.sendEmail(any(), any(), any()) }
            }
        }

        When("NotClaimable 로 들어오면") {
            val outboxEvent = OutboxEvent(
                listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
                eventType = OutboxEventType.CACHE_EVICT_ALL,
                serializedEvent = "dummy",
                publicationDate = LocalDateTime.now(),
            ).apply {
                id = "99"
            }
            every { outboxService.claimOutbox(any()) } returns ClaimFailure.NotClaimable
            Then("log 만 호출된다") {
                processor.process(outboxEvent.id, onFailure = onFailure, onLog = onLog)
                verify(exactly = 1) { outboxService.claimOutbox(any()) }
                verify(exactly = 0) { mailService.sendEmail(any(), any(), any()) }
                verify(exactly = 0) { onFailure(any()) }
                verify(exactly = 1) { onLog("Not claimable — already processed elsewhere: ${outboxEvent.id}") }
            }
        }

        data class NoEmailCase(
            val description: String,
            val eventType: OutboxEventType,
            val event: PublishableEvent,
            val expectedErrorType: TodoExceptionType,
        )

        val noEmailCases = listOf(
            NoEmailCase(
                description = "구매 요청인데 판매자 이메일이 없는 경우",
                eventType = OutboxEventType.PURCHASE_REQUEST,
                event = PublishableEvent.PurchaseRequestEvent(
                    sellerId = "no-email-user",
                    buyerId = "buyer",
                    todoId = 1L,
                    price = 100L,
                    purchaseId = 1L,
                ),
                expectedErrorType = TodoExceptionType.OUTBOX_SELLER_EMAIL_EMPTY,
            ),
            NoEmailCase(
                description = "구매 승인인데 구매자 이메일이 없는 경우",
                eventType = OutboxEventType.PURCHASE_APPROVED,
                event = PublishableEvent.PurchaseApprovedEvent(
                    sellerId = "seller",
                    buyerId = "no-email-user",
                    todoId = 1L,
                    price = 100L,
                    purchaseId = 1L
                ),
                expectedErrorType = TodoExceptionType.OUTBOX_BUYER_EMAIL_EMPTY,
            ),
            NoEmailCase(
                description = "구매 거절인데 구매자 이메일이 없는 경우",
                eventType = OutboxEventType.PURCHASE_REJECTED,
                event = PublishableEvent.PurchaseRejectedEvent(
                    sellerId = "seller",
                    buyerId = "no-email-user",
                    todoId = 1L,
                    price = 100L,
                    purchaseId = 1L
                ),
                expectedErrorType = TodoExceptionType.OUTBOX_BUYER_EMAIL_EMPTY,
            ),
        )

        noEmailCases.forEach { case ->
            When(case.description) {
                val outboxEvent = OutboxEvent(
                    listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
                    eventType = case.eventType,
                    serializedEvent = objectMapper.writeValueAsString(case.event),
                    publicationDate = LocalDateTime.now(),
                    processingStartedDate = LocalDateTime.now(),
                ).apply { id = "99" }

                every { outboxService.claimOutbox(any()) } returns ClaimSuccess(outboxEvent)
                every { authService.findUserByUserId(userId = any()) } returns User(
                    userId = "no-email-user",
                    password = "pita",
                    email = null
                )
                every { outboxService.updateOutboxFail(any(), any(), any()) } just Runs

                // TodoException은 processClaimedEvent 내부 runCatching에 흡수돼 process() 밖으로 다시 던져지지 않는다.
                // 그래서 assertThrows가 아니라, 밖으로 노출되는 유일한 통로인 onFailure 콜백을 캡처해서 검증한다.
                val throwableSlot = slot<Throwable>()
                every { onFailure(capture(throwableSlot)) } just Runs

                Then("메일을 보내지 않고 outbox가 ${case.expectedErrorType}로 실패 처리된다") {
                    processor.process(eventId = outboxEvent.id, onFailure = onFailure, onLog = onLog)

                    verify(exactly = 0) { mailService.sendEmail(any(), any(), any()) }
                    // saveEmailSentRecordToReady 자체가 호출된 적이 없으므로(이메일이 없어서 그 전에 터짐),
                    // 이 워커는 애초에 아무 row도 소유한 적이 없다 -> 지울 게 없다.
                    verify(exactly = 0) { mailService.deleteSentRecord(any()) }
                    verify(exactly = 1) {
                        outboxService.updateOutboxFail(
                            id = outboxEvent.id,
                            errorMessage = any(),
                            claimedAt = any()
                        )
                    }

                    val result = throwableSlot.captured
                    assertThat(result).isInstanceOf(TodoException::class.java)
                    result as TodoException
                    assertThat(result.errorCode).isEqualTo(case.expectedErrorType.errorCode)
                    assertThat(result.message).isEqualTo(case.expectedErrorType.message)
                    assertThat(result.httpStatusCode).isEqualTo(case.expectedErrorType.httpStatusCode)
                }
            }
        }

        When("saveEmailSentRecordToReady는 성공했지만 sendEmail이 진짜로 실패하면") {
            val outboxEvent = OutboxEvent(
                listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
                eventType = OutboxEventType.PURCHASE_REQUEST,
                serializedEvent = objectMapper.writeValueAsString(
                    PublishableEvent.PurchaseRequestEvent(
                        sellerId = "email-user",
                        buyerId = "buyer",
                        todoId = 1L,
                        price = 100L,
                        purchaseId = 1L,
                    )
                ),
                publicationDate = LocalDateTime.now(),
                processingStartedDate = LocalDateTime.now(),
            ).apply { id = "99" }

            every { outboxService.claimOutbox(any()) } returns ClaimSuccess(outboxEvent)
            every { authService.findUserByUserId(userId = any()) } returns User(
                userId = "email-user",
                password = "pita",
                email = "abc"
            )
            every { mailService.saveEmailSentRecordToReady(any()) } returns EmailSentRecord(
                id = outboxEvent.id,
                status = EmailSentStatus.READY,
            )
            every { mailService.sendEmail(any(), any(), any()) } throws RuntimeException("smtp down")
            every { mailService.deleteSentRecord(any()) } just Runs
            every { outboxService.updateOutboxFail(any(), any(), any()) } just Runs

            Then("saveEmailSentRecordToReady로 자신이 만든 row이므로 deleteSentRecord가 호출되고 재시도 대상으로 처리된다") {
                processor.process(eventId = outboxEvent.id, onFailure = onFailure, onLog = onLog)

                verify(exactly = 1) { mailService.deleteSentRecord(id = outboxEvent.id) }
                verify(exactly = 1) {
                    outboxService.updateOutboxFail(
                        id = outboxEvent.id,
                        errorMessage = any(),
                        claimedAt = any(),
                    )
                }
                verify(exactly = 1) { onFailure(any()) }
                verify(exactly = 0) { mailService.markSentAt(any(), any()) }
            }
        }

        When("markSentAt, 이메일 발송 후 발송 이력 작성에 에러가 나면 재시도 대상으로 보내지 않음") {
            every {
                mailService.markSentAt(any(), any())
            } throws (TodoException.of(TodoExceptionType.OUTBOX_UPDATE_ERROR))

            val outboxEvent = OutboxEvent(
                listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
                eventType = OutboxEventType.PURCHASE_REQUEST,
                serializedEvent = objectMapper.writeValueAsString(
                    PublishableEvent.PurchaseRequestEvent(
                        sellerId = "email-user",
                        buyerId = "buyer",
                        todoId = 1L,
                        price = 100L,
                        purchaseId = 1L,
                    )
                ),
                publicationDate = LocalDateTime.now(),
                processingStartedDate = LocalDateTime.now(),
            ).apply { id = "99" }

            every { outboxService.claimOutbox(any()) } returns ClaimSuccess(outboxEvent)
            every { authService.findUserByUserId(userId = any()) } returns User(
                userId = "no-email-user",
                password = "pita",
                email = "abc"
            )
            every { mailService.saveEmailSentRecordToReady(any()) } returns EmailSentRecord(
                id = outboxEvent.id,
                status = EmailSentStatus.READY,
            )
            every { mailService.sendEmail(any(), any(), any()) } just Runs
            every { outboxService.updateOutboxSuccess(outboxEvent = outboxEvent) } just Runs

            processor.process(eventId = outboxEvent.id, onFailure = onFailure, onLog = onLog)

            verify(exactly = 1) { mailService.markSentAt(any(), any()) }
            verify(exactly = 1) { onLog(any()) }
        }

        When("DataIntegrityViolationException 에러를 받앗을 때") {
            every { mailService.saveEmailSentRecordToReady(any()) } throws DataIntegrityViolationException("abx")
            val outboxEvent = OutboxEvent(
                listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
                eventType = OutboxEventType.PURCHASE_REQUEST,
                serializedEvent = objectMapper.writeValueAsString(
                    PublishableEvent.PurchaseRequestEvent(
                        sellerId = "email-user",
                        buyerId = "buyer",
                        todoId = 1L,
                        price = 100L,
                        purchaseId = 1L,
                    )
                ),
                publicationDate = LocalDateTime.now(),
                processingStartedDate = LocalDateTime.now(),
            ).apply { id = "99" }
            every { outboxService.claimOutbox(any()) } returns ClaimSuccess(outboxEvent)
            every { authService.findUserByUserId(userId = any()) } returns User(
                userId = "no-email-user",
                password = "pita",
                email = "abc"
            )

            Then("sentStatus == EmailSentStatus.SENT 인 경우, 다른 워커가 이미 보냈기에 재전송을 하지 않고 성공처리") {
                every { mailService.findStatusByEmailId(any()) } returns EmailSentStatus.SENT
                every { outboxService.updateOutboxSuccess(any()) } just Runs

                processor.process(eventId = outboxEvent.id, onFailure = onFailure, onLog = onLog)

                verify(exactly = 1) { outboxService.updateOutboxSuccess(outboxEvent) }
                verify(exactly = 1) { onLog(any()) }
                verify(exactly = 0) { mailService.sendEmail(any(), any(), any()) }
            }

            Then("sentStatus == EmailSentStatus.READY 인 경우, 다른 워커가 결론을 못냈기에 실패 처리") {
                /**
                 * 실패처리 한 경우
                 * 1. 진짜 실패된 채로 끝나서 ready로 끝나있는 경우 재시도 워커가 처리하므로 안전
                 * 2. 진짜 처리중이였다면 처리 이후 해당 워커가 success 처리하면서 DELETE 처리 하니 정합성에 문제 없음
                 */
                every { mailService.findStatusByEmailId(any()) } returns EmailSentStatus.READY
                every { outboxService.updateOutboxFail(any(), any(), any()) } just Runs

                processor.process(eventId = outboxEvent.id, onFailure = onFailure, onLog = onLog)

                verify(exactly = 1) { onLog(any()) }
                verify(exactly = 1) { outboxService.updateOutboxFail(any(), any(), any()) }
                verify(exactly = 0) { mailService.sendEmail(any(), any(), any()) }
            }

            Then("sentStatus == null") {
                every { mailService.findStatusByEmailId(any()) } returns null

                processor.process(eventId = outboxEvent.id, onFailure = onFailure, onLog = onLog)

                verify(exactly = 1) { onLog(any()) }
            }
        }
    }
}) {
    override fun isolationMode(): IsolationMode = IsolationMode.InstancePerLeaf
}
