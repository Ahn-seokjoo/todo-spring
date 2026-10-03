package com.seokjoo.todo.domain.service.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.annotation.TodoTest
import com.seokjoo.todo.domain.entity.outbox.OutboxEvent
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.email.EmailService
import com.seokjoo.todo.domain.service.outbox.repository.OutboxArchiveRepository
import com.seokjoo.todo.domain.service.outbox.repository.OutboxRepository
import com.seokjoo.todo.domain.service.outbox.service.OutboxEventProcessor
import com.seokjoo.todo.domain.service.outbox.service.OutboxService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

// 이 테스트는 실제 커밋 + 별도 스레드로 "느린 워커가 재claim된 이벤트를 뒤늦게 처리하는" 상황을
// 강제로 재현하기 때문에 @Transactional 롤백에 기대지 못한다. 다른 테스트와 안 겹치게 전용 계정을 쓰고 직접 정리한다.
// @MockitoBean으로 JavaMailSender를 목킹하면 Actuate의 MailHealthContributorAutoConfiguration이
// 빈 Map<String, JavaMailSender>를 받아 컨텍스트 로딩에 실패하는 것으로 알려진 이슈가 있어, 이 테스트에서만 메일 헬스체크를 끈다.
@TodoTest
@TestPropertySource(properties = ["management.health.mail.enabled=false"])
class OutboxEmailDuplicateSendTest @Autowired constructor(
    private val todoAuthService: TodoAuthService,
    private val outboxService: OutboxService,
    private val outboxRepository: OutboxRepository,
    private val outboxArchiveRepository: OutboxArchiveRepository,
    private val processor: OutboxEventProcessor,
    private val objectMapper: ObjectMapper,
    private val transactionManager: PlatformTransactionManager,
    private val mailService: EmailService,
) {
    @MockitoBean
    private lateinit var mailSender: JavaMailSender

    private val sellerId = "dup-send-seller"
    private val buyerId = "dup-send-buyer"

    @Test
    fun `느린 워커가 재claim된 이벤트를 뒤늦게 처리해도 이메일은 한 번만 발송돼야 한다`() {
        todoAuthService.signUp(sellerId, "pita", email = "dup-send-seller@test.com")
        todoAuthService.signUp(buyerId, "pita", email = "dup-send-buyer@test.com")

        val event = PublishableEvent.PurchaseRequestEvent(
            sellerId = sellerId, buyerId = buyerId, todoId = 1L, price = 100L, purchaseId = 1L,
        )
        val outboxEvent = outboxService.saveOutbox(
            OutboxEvent(
                listenerType = OutboxListenerType.EMAIL_NOTIFICATION,
                eventType = OutboxEventType.PURCHASE_REQUEST,
                serializedEvent = objectMapper.writeValueAsString(event),
                publicationDate = LocalDateTime.now(),
            )
        )

        // 첫 번째 send 호출( = 워커 A)은 응답이 느려서 아직 안 끝난 상황을 흉내내려고 일부러 붙잡아둔다.
        // 이후 호출( = 워커 B, 혹은 뒤늦게 풀린 A)은 그냥 즉시 리턴한다.
        val sendCount = AtomicInteger(0)
        val sendStarted = CountDownLatch(1)
        val releaseSend = CountDownLatch(1)
        doAnswer {
            if (sendCount.getAndIncrement() == 0) {
                sendStarted.countDown()
                releaseSend.await(5, TimeUnit.SECONDS)
            }
            null
        }.whenever(mailSender).send(any<SimpleMailMessage>())

        val executor = Executors.newSingleThreadExecutor()
        try {
            // 워커 A: claim에 성공하고 sendEmail 안에서 응답 대기 중(=블로킹)인 상태로 만든다.
            val workerA = executor.submit { processor.process(outboxEvent.id) }
            sendStarted.await(5, TimeUnit.SECONDS)

            // 폴러가 stale로 판단해 되돌리는 것을 결정론적으로 재현(미래 cutoff로 sleep 없이 강제)
            // 이 테스트 클래스엔 앰비언트 @Transactional이 없어서(실제 커밋이 필요하기 때문),
            // 커스텀 @Modifying 쿼리를 직접 호출하면 flush 시점에 EntityManager 트랜잭션이 없어 실패한다.
            // 이 호출 하나만을 위한 짧은 트랜잭션을 직접 열어서 즉시 커밋되게 한다.
            TransactionTemplate(transactionManager).execute {
                outboxRepository.updateAllResubmittedOutbox(cutoff = LocalDateTime.now().plusMinutes(1))
            }

            // 워커 B: 재claim해서 끝까지 처리(이 시점엔 A의 send가 아직 안 끝났으므로 mock은 바로 리턴)
            processor.process(outboxEvent.id)

            // 이제 A의 느렸던 send가 뒤늦게 응답을 받는다
            releaseSend.countDown()
            workerA.get(5, TimeUnit.SECONDS)

            // 실제로 메일은 딱 한 번만 나갔어야 한다
            verify(mailSender, times(1)).send(any<SimpleMailMessage>())
        } finally {
            executor.shutdownNow()
            mailService.deleteSentRecord(id = outboxEvent.id)
            outboxArchiveRepository.deleteById(outboxEvent.id)
            outboxService.deleteOutboxEvent(outboxEvent.id)
            todoAuthService.delete(sellerId, "pita")
            todoAuthService.delete(buyerId, "pita")
        }
    }
}
