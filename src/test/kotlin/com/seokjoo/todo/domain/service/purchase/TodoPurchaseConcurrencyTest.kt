package com.seokjoo.todo.domain.service.purchase

import com.seokjoo.todo.annotation.TodoTest
import com.seokjoo.todo.domain.entity.purchase.PurchaseStatus
import com.seokjoo.todo.domain.entity.todo.TodoStatus
import com.seokjoo.todo.domain.entity.todouser.Money
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.repository.purchase.TodoPurchaseRepository
import com.seokjoo.todo.domain.repository.todouser.TodoAuthRepository
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.balance.TodoBalanceService
import com.seokjoo.todo.domain.service.charge.TodoChargeService
import com.seokjoo.todo.domain.service.todo.TodoCreateServiceRequestDTO
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@TodoTest
class TodoPurchaseConcurrencyTest @Autowired constructor(
    private val todoService: TodoService,
    private val todoAuthService: TodoAuthService,
    private val todoChargeService: TodoChargeService,
    private val todoPurchaseService: TodoPurchaseService,
    private val todoPurchaseRepository: TodoPurchaseRepository,
    private val todoAuthRepository: TodoAuthRepository,
    private val todoBalanceService: TodoBalanceService,
) {
    lateinit var seller: User
    lateinit var todo: TodoServiceResponseDTO

    @Test
    fun `동시에 같은 available한 todo 에 purchase 를 요청해도 한명만 `() {
        val threadCount = 5
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        val todo = todoService.getTodoById(todo.id)

        val users = listOf("pita2", "pita3", "pita4", "pita5", "pita6").map { id ->
            todoAuthService.findUserByUserId(id)
        }

        val futures = users.map { user ->
            executor.submit(Callable {
                try {
                    todoPurchaseService.purchaseTodo(todo.id, user.userId)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            })
        }

        val completedInTime = latch.await(60, TimeUnit.SECONDS)
        if (!completedInTime) {
            executor.shutdownNow()
            fail<Unit>("60초 내에 모든 charge 스레드가 끝나지 않았습니다 (동시성 회귀 의심 - CI 행 방지를 위해 즉시 실패 처리)")
        }
        futures.forEach { it.get(5, TimeUnit.SECONDS) }
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)

        // then
        // purchase 가 1개 존재 (1명만 성공해서)
        val purchases = todoPurchaseRepository.findAll()
        assertThat(purchases.size).isEqualTo(1)
        assertThat(purchases.count { it.purchaseStatus == PurchaseStatus.PENDING }).isEqualTo(1)
        assertThat(successCount.get()).isEqualTo(1)
        assertThat(failCount.get()).isEqualTo(4)

        // purchase 의 buyerId 잔액 체크시에 100원 줄어있고
        val purchase = purchases.first()
        val buyer = todoAuthService.findUserByUserId(purchase!!.buyerId)
        assertThat(buyer.money).isEqualTo(Money(900L))

        // 구매 못한 자들은 1000원 그대로
        val notBuyers = users.filterNot { it.userId == buyer.userId }
        notBuyers.forEach { notBuyer ->
            assertThat(notBuyer.money).isEqualTo(Money(1_000L))
        }

        // 그리고 해당 todo는 status가 대기중으로 변경됨
        val resultTodo = todoService.getTodoById(todo.id)
        assertThat(resultTodo.status).isEqualTo(TodoStatus.PENDING_APPROVAL)
    }

    @Test
    fun `Approve를 여러번 호출 한 경우에도 첫 번째 요청만 성공하여 buyer 금액이 1회만 차감, seller는 1회만 지급된다`() {
        val threadCount = 5
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        val todo = todoService.getTodoById(todo.id)
        val user = todoAuthService.findUserByUserId("pita2")
        // given todo 구매 요청
        todoPurchaseService.purchaseTodo(todo.id, user.userId)

        // when 중복으로 approve 했음
        val futures = (0..<threadCount).map {
            executor.submit(Callable {
                try {
                    todoPurchaseService.approvePurchaseTodo(todo.id, seller.userId)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            })
        }

        val completedInTime = latch.await(60, TimeUnit.SECONDS)
        if (!completedInTime) {
            executor.shutdownNow()
            fail<Unit>("60초 내에 모든 charge 스레드가 끝나지 않았습니다 (동시성 회귀 의심 - CI 행 방지를 위해 즉시 실패 처리)")
        }
        futures.forEach { it.get(5, TimeUnit.SECONDS) }
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)

        // then
        val purchases = todoPurchaseRepository.findAll()
        assertThat(purchases.size).isEqualTo(1)
        assertThat(successCount.get()).isEqualTo(1)
        assertThat(failCount.get()).isEqualTo(4)
        val purchase = purchases.first()
        val buyer = todoAuthService.findUserByUserId(purchase!!.buyerId)
        assertThat(buyer.money).isEqualTo(Money(900L))
        val seller = todoAuthService.findUserByUserId(seller.userId)
        assertThat(seller.money).isEqualTo(Money(1_100L))
    }

    @Test
    fun `Approve와 cancel을 동시에 요청해도 하나의 종료 상태만 남고 총 잔액은 보존된다`() {
        val threadCount = 2
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        todoPurchaseService.purchaseTodo(todo.id, "pita2")

        val futures = listOf(
            executor.submit(Callable {
                try {
                    todoPurchaseService.approvePurchaseTodo(todo.id, seller.userId)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }),
            executor.submit(Callable {
                try {
                    todoPurchaseService.cancelPurchaseTodo(todo.id, "pita2")
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }),
        )

        val completedInTime = latch.await(60, TimeUnit.SECONDS)
        if (!completedInTime) {
            executor.shutdownNow()
            fail<Unit>("60초 내에 모든 approve/cancel 스레드가 끝나지 않았습니다 (동시성 회귀 의심 - CI 행 방지를 위해 즉시 실패 처리)")
        }
        futures.forEach { it.get(5, TimeUnit.SECONDS) }
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)

        val purchases = todoPurchaseRepository.findAll()
        assertThat(purchases).hasSize(1)
        assertThat(purchases.single().purchaseStatus)
            .isIn(PurchaseStatus.APPROVED, PurchaseStatus.CANCELLED)
        assertThat(successCount.get()).isEqualTo(1)
        assertThat(failCount.get()).isEqualTo(1)

        val sellerBalance = todoBalanceService.getBalance("pita1")
        val buyerBalance = todoBalanceService.getBalance("pita2")
        assertThat(sellerBalance + buyerBalance).isEqualTo(2_000L)
    }

    @BeforeEach
    fun before() {
        todoAuthService.signUp("pita1", "pita", "abc.com")
        todoAuthService.signUp("pita2", "pita", "abc.com")
        todoAuthService.signUp("pita3", "pita", "abc.com")
        todoAuthService.signUp("pita4", "pita", "abc.com")
        todoAuthService.signUp("pita5", "pita", "abc.com")
        todoAuthService.signUp("pita6", "pita", "abc.com")

        todoChargeService.charge(1_000L, "pita1")
        todoChargeService.charge(1_000L, "pita2")
        todoChargeService.charge(1_000L, "pita3")
        todoChargeService.charge(1_000L, "pita4")
        todoChargeService.charge(1_000L, "pita5")
        todoChargeService.charge(1_000L, "pita6")

        seller = todoAuthService.findUserByUserId("pita1")
        todo =
            todoService.createTodo(request = TodoCreateServiceRequestDTO(todo = "hi", price = 100L), owner = seller)
    }

    @AfterEach
    fun after() {
        todoAuthRepository.deleteAll()
        todoPurchaseRepository.deleteAll()
    }
}
