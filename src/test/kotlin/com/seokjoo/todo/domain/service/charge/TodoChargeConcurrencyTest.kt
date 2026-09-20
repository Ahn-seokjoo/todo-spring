package com.seokjoo.todo.domain.service.charge

import com.seokjoo.todo.annotation.TodoTest
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.repository.todouser.TodoAuthRepository
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.balance.TodoBalanceService
import com.seokjoo.todo.domain.service.todo.TodoCreateServiceRequestDTO
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.domain.service.trade.TodoTradeService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@TodoTest
class TodoChargeConcurrencyTest @Autowired constructor(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val todoAuthService: TodoAuthService,
    private val todoAuthRepository: TodoAuthRepository,
    private val chargeService: TodoChargeService,
    private val todoService: TodoService,
    private val todoTradeService: TodoTradeService,
    private val todoBalanceService: TodoBalanceService,
) {

    @Test
    fun `유저가 동시에 충전을 한다면 lost update 가 발생한다`() {
        val threadCount = 100
        val executor = Executors.newFixedThreadPool(32)
        val latch = CountDownLatch(threadCount)

        val user = todoAuthService.findUserByUserId("pita1")

        val futures = (1..threadCount).map {
            executor.submit(Callable {
                try {
                    chargeService.charge(100, user.userId)
                } finally {
                    latch.countDown()
                }
            })
        }
        // CI 행 방지: 60초 내 미완료 시 즉시 실패 처리
        val completedInTime = latch.await(60, TimeUnit.SECONDS)
        if (!completedInTime) {
            executor.shutdownNow()
            fail<Unit>("60초 내에 모든 charge 스레드가 끝나지 않았습니다 (동시성 회귀 의심 - CI 행 방지를 위해 즉시 실패 처리)")
        }
        futures.forEach { it.get(5, TimeUnit.SECONDS) }
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)

        val finalUser = todoAuthService.findUserByUserId(user.userId)
        assertThat(todoBalanceService.getBalance(finalUser.userId)).isEqualTo(10_000L)
    }

    @Test
    fun `유저가 충전하는 동시에, todo 판매에 성공하여 잔액이 증가할때 최종 값이 맞아야 한다`() {
        val threadCount = 100
        val executor = Executors.newFixedThreadPool(32)
        val latch = CountDownLatch(threadCount)

        val user1 = todoAuthService.findUserByUserId("pita1")
        val user2 = todoAuthService.findUserByUserId("pita2")

        val todoIds = (1..50).map { number ->
            val request = TodoCreateServiceRequestDTO(
                todo = "테스트 100원 짜리 투두 $number",
                price = 100L
            )
            todoService.createTodo(request, user1).id
        }

        // 유저 충전 50번 - 각 반복을 별도 task로 submit해야 실제로 병렬 실행된다
        val chargeFutures = (1..(threadCount / 2)).map {
            executor.submit(Callable {
                try {
                    chargeService.charge(100, user1.userId)
                } finally {
                    latch.countDown()
                }
            })
        }

        val buyFutures = todoIds.map { todoId ->
            executor.submit(Callable {
                try {
                    todoTradeService.buyTodo(todoId = todoId, buyerUserId = user2.userId)
                } catch (e: Exception) {
                    throw e
                } finally {
                    latch.countDown()
                }
            })
        }

        val completedInTime = latch.await(60, TimeUnit.SECONDS)
        if (!completedInTime) {
            executor.shutdownNow()
            fail<Unit>("60초 내에 모든 charge/buy 스레드가 끝나지 않았습니다 (동시성 회귀 의심 - CI 행 방지를 위해 즉시 실패 처리)")
        }
        (chargeFutures + buyFutures).forEach { it.get(5, TimeUnit.SECONDS) }
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)

        val finalUser = todoAuthService.findUserByUserId("pita1")
        val finalUser2 = todoAuthService.findUserByUserId("pita2")

        val finalUser1TodoCounts = todoService.getTodoCounts(finalUser)
        val finalUser2TodoCounts = todoService.getTodoCounts(finalUser2)

        val finalUser1Todos = todoAuthService.findUserWithTodosByUserId("pita1")
        val finalUser2Todos = todoAuthService.findUserWithTodosByUserId("pita2")

        // 투두 전부 판매됨
        assertThat(finalUser1TodoCounts).isEqualTo(0)
        assertThat(finalUser1Todos.todoList.size).isEqualTo(0)
        // 충전 + 판매된 금액 합쳐서 총 10000 원
        assertThat(todoBalanceService.getBalance(finalUser.userId)).isEqualTo(10_000L)
        // 투두 전부 구매한 유저 개수 50개
        assertThat(finalUser2TodoCounts).isEqualTo(50)
        assertThat(finalUser2Todos.todoList.size).isEqualTo(50)
        // 구매 완료한 유저 잔액 0원
        assertThat(todoBalanceService.getBalance(finalUser2.userId)).isEqualTo(0L)
    }

    @BeforeEach
    fun before() {
        val keys = redisTemplate.keys("*")
        redisTemplate.delete(keys)

        val user1 = User("pita1", "pita1", email = "abc@nav.com")
        val user2 = User("pita2", "pita2", email = "abc@nav.com")
        todoAuthService.signUp(user1.userId, user1.password, user1.email!!)
        todoAuthService.signUp(user2.userId, user2.password, user2.email!!)
        chargeService.charge(5_000L, user2.userId)
    }

    @AfterEach
    fun after() {
        todoAuthRepository.deleteAll()
    }
}
