package com.seokjoo.todo.domain.service.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.outbox.OutboxEvent
import com.seokjoo.todo.domain.entity.outbox.OutboxEventArchive
import com.seokjoo.todo.domain.entity.outbox.OutboxEventDLQ
import com.seokjoo.todo.domain.entity.outbox.OutboxStatus
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.service.outbox.repository.OutboxArchiveRepository
import com.seokjoo.todo.domain.service.outbox.repository.OutboxDLQRepository
import com.seokjoo.todo.domain.service.outbox.repository.OutboxRepository
import com.seokjoo.todo.domain.service.outbox.service.OutboxService
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull
import java.time.Duration
import java.time.LocalDateTime

class OutboxServiceTest : BehaviorSpec({
    val outboxRepository: OutboxRepository = mockk()
    val outboxArchiveRepository: OutboxArchiveRepository = mockk()
    val outboxDLQRepository: OutboxDLQRepository = mockk()

    val objectMapper: ObjectMapper = jacksonObjectMapper()
    val staleThresholdMillis = 100L
    val outboxService = OutboxService(
        outboxRepository,
        outboxArchiveRepository,
        outboxDLQRepository,
        objectMapper,
        staleThresholdMillis,
    )
    val seller = User(userId = "pita1", password = "pw", email = "seller@test.com")
    val buyer = User(userId = "pita2", password = "pw", email = "buyer@test.com")

    fun outboxEventOf(
        eventType: OutboxEventType,
        event: PublishableEvent,
        completionAttempts: Int = 0,
        status: OutboxStatus = OutboxStatus.PENDING,
    ): OutboxEvent {
        return OutboxEvent(
            listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
            eventType = eventType,
            serializedEvent = objectMapper.writeValueAsString(event),
            publicationDate = LocalDateTime.now(),
            completionAttempts = completionAttempts,
            status = status,
        ).also { it.id = "outbox-${eventType.name}" }
    }

    Given("claim outbox 를 수행했을 때") {
        val purchaseRequestEvent = PublishableEvent.PurchaseRequestEvent(
            sellerId = seller.userId, buyerId = buyer.userId, todoId = 1L, price = 100L, purchaseId = 1L
        )
        val event = outboxEventOf(
            eventType = OutboxEventType.PURCHASE_REQUEST,
            event = purchaseRequestEvent,
        )
        every { outboxRepository.findByIdOrNull(any()) } returns event

        When("outboxRepository 에서 해당 id를 찾아내지 못하면") {
            every {
                outboxRepository.updateProcessingOutbox(any(), now = any())
            } returns 0
            every { outboxRepository.findByIdOrNull(any()) } returns null

            Then("claimOutbox 는 TodoExceptionType.OUTBOX_NOT_FOUND 를 뱉는다") {
                val result = assertThrows<TodoException> {
                    outboxService.claimOutbox("99")
                }
                assertThat(result.errorCode).isEqualTo(TodoExceptionType.OUTBOX_NOT_FOUND.errorCode)
                assertThat(result.message).isEqualTo(TodoExceptionType.OUTBOX_NOT_FOUND.message)
                assertThat(result.httpStatusCode).isEqualTo(TodoExceptionType.OUTBOX_NOT_FOUND.httpStatusCode)
            }
        }
        When("수정이 성공해서 effectedCount = 1이면 ClaimSuccess로 리턴") {
            every { outboxRepository.updateProcessingOutbox(any(), now = any()) } returns 1
            every { outboxRepository.findByIdOrNull(any()) } returns event

            Then("Success랑 같이 event가 나온다.") {
                val result = outboxService.claimOutbox("99")
                assertThat(result).isEqualTo(ClaimSuccess(event))
            }
        }

        When("수정이 실패해서 effectedCount = 0 이면 when 로직을 수행한다") {
            every { outboxRepository.updateProcessingOutbox(any(), now = any()) } returns 0

            Then("MAX_ATTEMPT 보다 크면") {
                val event = outboxEventOf(
                    eventType = OutboxEventType.PURCHASE_REQUEST,
                    event = purchaseRequestEvent,
                    completionAttempts = 5,
                )
                every { outboxRepository.findByIdOrNull(any()) } returns event

                val result = outboxService.claimOutbox("99")
                assertThat(result).isEqualTo(ClaimFailure.CheckDLQ(event))
            }
            Then("status == PROCESSING 이면") {
                val event = outboxEventOf(
                    eventType = OutboxEventType.PURCHASE_REQUEST,
                    event = purchaseRequestEvent,
                    status = OutboxStatus.PROCESSING,
                )
                every { outboxRepository.findByIdOrNull(any()) } returns event

                val result = outboxService.claimOutbox("99")
                assertThat(result).isEqualTo(ClaimFailure.Processing)
            }
            Then("else로 빠지면 ") {
                val result = assertThrows<TodoException> {
                    outboxService.claimOutbox("99")
                }
                assertThat(result.message).isEqualTo(TodoExceptionType.OUTBOX_UPDATE_ERROR.message)
                assertThat(result.errorCode).isEqualTo(TodoExceptionType.OUTBOX_UPDATE_ERROR.errorCode)
                assertThat(result.httpStatusCode).isEqualTo(TodoExceptionType.OUTBOX_UPDATE_ERROR.httpStatusCode)
            }
        }
    }

    Given("updateOutboxSuccess 테스트") {
        When("updateOutboxSuccess 가 호출되면") {
            val outboxEvent = outboxEventOf(
                eventType = OutboxEventType.PURCHASE_REQUEST,
                event = PublishableEvent.CacheEvictAllEvent,
            )

            every { outboxRepository.deleteOutboxEventById(any()) } returns 1
            every { outboxArchiveRepository.save(any()) } returns OutboxEventArchive.from(
                outboxEvent = outboxEvent,
            )
            Then("delete, save 둘다 1회씩 호출된다") {
                outboxService.updateOutboxSuccess(outboxEvent)
                verify(exactly = 1) {
                    outboxRepository.deleteOutboxEventById(any())
                }
                verify(exactly = 1) {
                    outboxArchiveRepository.save(any())
                }
            }
        }
    }

    Given("updateOutboxFail 테스트") {
        When("updateOutboxFail 호출시에") {
            every { outboxRepository.updateFailedOutbox(any(), any(), any(), any()) } returns 1
            Then("updateFailedOutbox 1회 호출") {
                outboxService.updateOutboxFail("id", "message")
                verify(exactly = 1) {
                    outboxRepository.updateFailedOutbox(any(), any(), any(), any())
                }
            }
        }
    }

    Given("updateOutboxDLQ 테스트") {
        When("updateOutboxDLQ 호출시에") {
            val outboxEvent = outboxEventOf(
                eventType = OutboxEventType.PURCHASE_REQUEST,
                event = PublishableEvent.CacheEvictAllEvent,
            )

            every { outboxRepository.deleteOutboxEventById(any()) } returns 1
            every { outboxDLQRepository.save(any()) } returns OutboxEventDLQ.from(outboxEvent)
            Then("deleteOutboxEventById, outboxDLQRepository.save 가 둘다 불린다") {
                outboxService.updateOutboxDLQ(outboxEvent)

                verify(exactly = 1) {
                    outboxRepository.deleteOutboxEventById(any())
                }
                verify(exactly = 1) {
                    outboxDLQRepository.save(any())
                }
            }
        }
    }

    Given("findPollerEvents 테스트") {
        When("findPollerEvents 호출시") {
            val cacheEvictAllEvent = outboxEventOf(
                eventType = OutboxEventType.CACHE_EVICT_ALL,
                event = PublishableEvent.CacheEvictAllEvent,
            )
            val cacheEvictSingleEvent = outboxEventOf(
                eventType = OutboxEventType.CACHE_EVICT_SINGLE,
                event = PublishableEvent.CacheEvictSingleEvent(123),
            )
            every { outboxRepository.findPollerEvents() } returns listOf(cacheEvictAllEvent, cacheEvictSingleEvent)
            Then("repository 위임한다") {
                val result = outboxService.findPollerEvents()

                assertThat(result).hasSize(2)
                assertThat(result).containsExactly(cacheEvictAllEvent, cacheEvictSingleEvent)
            }
        }
    }

    Given("updateAllResubmittedOutbox 테스트") {
        When("updateAllResubmittedOutbox 를 수행했을때") {
            // cutoff는 서비스 내부에서 LocalDateTime.now()로 계산되기 때문에,
            // 테스트에서 eq(정확한값)으로 매칭할 수 없어서 slot으로 실제로 넘어간 값을 캡처해서 검증한다.
            val cutoffSlot = slot<LocalDateTime>()
            every {
                outboxRepository.updateAllResubmittedOutbox(
                    cutoff = capture(cutoffSlot),
                    status = any(),
                    resubmitted = any(),
                )
            } returns 1

            Then("updateAllResubmittedOutbox 가 잘 수행됐는지") {
                outboxService.updateAllResubmittedOutbox()

                verify(exactly = 1) {
                    outboxRepository.updateAllResubmittedOutbox(
                        cutoff = any(),
                        status = eq(OutboxStatus.PROCESSING),
                        resubmitted = eq(OutboxStatus.RESUBMITTED),
                    )
                }
            }
            Then("오차 범위 이내인지") {
                outboxService.updateAllResubmittedOutbox()

                val expectedCutoff = LocalDateTime.now().minus(Duration.ofMillis(staleThresholdMillis))
                val diff = Duration.between(cutoffSlot.captured, expectedCutoff).abs()

                assertThat(diff).isLessThan(Duration.ofSeconds(1))
            }
        }
    }

    Given("createOutboxEvent 테스트") {
        When("PublishableEvent와 listener를 받으면") {
            val purchaseRequestEvent = PublishableEvent.PurchaseRequestEvent(
                sellerId = seller.userId, buyerId = buyer.userId, todoId = 1L, price = 100L, purchaseId = 1L
            )

            Then("listenerType/eventType/serializedEvent/publicationDate가 채워진 OutboxEvent가 만들어진다") {
                val before = LocalDateTime.now()
                val result = outboxService.createOutboxEvent(
                    event = purchaseRequestEvent,
                    listener = OutboxListenerType.EMAIL_NOTIFICATION,
                )
                val after = LocalDateTime.now()

                assertThat(result.listenerType).isEqualTo(OutboxListenerType.EMAIL_NOTIFICATION)
                assertThat(result.eventType).isEqualTo(OutboxEventType.PURCHASE_REQUEST)
                assertThat(result.serializedEvent).isEqualTo(objectMapper.writeValueAsString(purchaseRequestEvent))
                assertThat(result.publicationDate).isBetween(before, after)
            }
        }
    }
}) {
    override fun isolationMode(): IsolationMode = IsolationMode.InstancePerLeaf
}
