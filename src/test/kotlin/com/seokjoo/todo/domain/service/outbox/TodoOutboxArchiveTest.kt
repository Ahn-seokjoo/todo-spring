package com.seokjoo.todo.domain.service.outbox

import com.seokjoo.todo.annotation.TodoTest
import com.seokjoo.todo.domain.entity.outbox.OutboxStatus
import com.seokjoo.todo.domain.entity.purchase.PurchaseStatus
import com.seokjoo.todo.domain.repository.purchase.TodoPurchaseRepository
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.charge.TodoChargeService
import com.seokjoo.todo.domain.service.email.EmailService
import com.seokjoo.todo.domain.service.outbox.listener.event.TodoPurchaseEmailEvent
import com.seokjoo.todo.domain.service.outbox.repository.OutboxArchiveRepository
import com.seokjoo.todo.domain.service.outbox.repository.OutboxRepository
import com.seokjoo.todo.domain.service.outbox.service.OutboxService
import com.seokjoo.todo.domain.service.purchase.TodoPurchaseTxService
import com.seokjoo.todo.domain.service.todo.TodoCreateServiceRequestDTO
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import java.time.Duration

@RecordApplicationEvents
@TodoTest
@Transactional
class TodoOutboxArchiveTest @Autowired constructor(
    private val todoService: TodoService,
    private val todoAuthService: TodoAuthService,
    private val todoChargeService: TodoChargeService,

    private val purchaseTxService: TodoPurchaseTxService,
    private val todoPurchaseRepository: TodoPurchaseRepository,
    private val outboxRepository: OutboxRepository,
    private val outboxArchiveRepository: OutboxArchiveRepository,
    private val outboxService: OutboxService,
) {
    // 이 테스트는 AFTER_COMMIT 리스너를 실제로 태우기 위해 트랜잭션을 강제 커밋한다.
    // 즉 @Transactional 롤백에 기대서 데이터를 정리할 수 없으므로,
    // 다른 테스트 클래스와 절대 겹치지 않는 전용 계정을 쓰고 끝나면 직접 지운다.
    private val sellerId = "archive-test-seller"
    private val buyerId = "archive-test-buyer"

    lateinit var todo: TodoServiceResponseDTO

    @MockitoBean
    private lateinit var mailService: EmailService

    @Test
    fun `구매 요청 처리 후 outbox row가 archive로 이동한다`(applicationEvents: ApplicationEvents) {
        // given
        purchaseTxService.purchaseTodo(todo.id, buyerId)

        val outboxEventId = applicationEvents
            .stream(TodoPurchaseEmailEvent::class.java)
            .toList()
            .first()
            .outboxEventId

        // when - AFTER_COMMIT 리스너가 뜨려면 실제 커밋이 필요함
        TestTransaction.flagForCommit()
        TestTransaction.end()

        try {
            // then - @Async라 완료될 때까지 폴링
            await().atMost(Duration.ofSeconds(3)).untilAsserted {
                // outbox 는 성공시에 비워지고
                assertThat(outboxRepository.findByIdOrNull(outboxEventId)).isNull()

                // 아카이브로 이동
                val archived = outboxArchiveRepository.findByIdOrNull(outboxEventId)
                assertThat(archived).isNotNull
                assertThat(archived?.status).isEqualTo(OutboxStatus.SUCCESS)
            }
        } finally {
            // 실제 커밋된 데이터라 직접 정리. deleteOutboxEvent는 대상 없어도 안전(FAILED로 남는 경우 대비).
            outboxArchiveRepository.deleteById(outboxEventId)
            outboxService.deleteOutboxEvent(outboxEventId)
            todoPurchaseRepository.findByTodoIdAndPurchaseStatus(todo.id, PurchaseStatus.PENDING)
                ?.let { todoPurchaseRepository.delete(it) }
            todoAuthService.delete(sellerId, "pita")
            todoAuthService.delete(buyerId, "pita")
        }
    }

    @BeforeEach
    fun before() {
        todoAuthService.signUp(sellerId, "pita", email = "abc@nav.com")
        todoAuthService.signUp(buyerId, "pita", email = "abc@nav.com")

        val user1 = todoAuthService.findUserByUserId(sellerId)
        val user2 = todoAuthService.findUserByUserId(buyerId)
        todoChargeService.charge(1000L, user2.userId)

        val request = TodoCreateServiceRequestDTO("spring", price = 100L)
        todo = todoService.createTodo(request, user1)
    }
}
