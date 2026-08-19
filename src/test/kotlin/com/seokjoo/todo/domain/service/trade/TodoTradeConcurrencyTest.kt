package com.seokjoo.todo.domain.service.trade

import com.seokjoo.todo.annotation.TodoTest
import com.seokjoo.todo.common.redisson.RedisLockManager
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import com.seokjoo.todo.domain.repository.todouser.TodoAuthRepository
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.charge.TodoChargeService
import com.seokjoo.todo.domain.service.todo.TodoCreateServiceRequestDTO
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@TodoTest
class TodoTradeConcurrencyTest @Autowired constructor(
    private val todoService: TodoService,
    private val todoChargeService: TodoChargeService,
    private val todoAuthService: TodoAuthService,
    private val todoAuthRepository: TodoAuthRepository,
    private val todoTradeService: TodoTradeService,
    private val todoRepository: TodoRepository,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val redisLockManager: RedisLockManager,
) {
    lateinit var todo: TodoServiceResponseDTO

    @Test
    fun `동시에 같은 유저의 금액을 차감시키려할 때`() {
        // user 2 생성
        val user2 = todoAuthService.findUserByUserId("pita2")
        val threadCount = 100
        val executor = Executors.newFixedThreadPool(32)
        val latch = CountDownLatch(threadCount)

        repeat(100) {
            executor.submit {
                try {
                    todoTradeService.buyTodo(todo.id, user2.userId)
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        executor.shutdown()

        val todo = todoService.getTodoById(todo.id)
        assert(todo.todo == "테스트 500원 짜리 투두")
        assert(todo.price == 500L)

        val user1 = todoAuthService.findUserByUserId("pita1")
        assert(user1.currentBalance() == 500L)

        val dbUser2 = todoAuthService.findUserByUserId("pita2")
        assert(todo.ownerId == dbUser2.userId)
        assert(dbUser2.currentBalance() == 0L)
    }

    @Test
    fun `10명이 같은 todo를 동시에 구매할 때 모두 다 구매가 잘된다`() {
        val userList = (10..19).map {
            val user = User("pita$it", "pita$it")
            todoAuthService.signUp(user.userId, user.password)
            val token = todoAuthService.login(user.userId, user.password)
            todoChargeService.charge(500L, token.accessToken)
            todoAuthService.findUser(token.accessToken)
        }

        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(10)

        val futures = userList.map {
            executor.submit(Callable {
                try {
                    todoTradeService.buyTodo(todo.id, it.userId)
                } catch (e: Exception) {
                    throw e
                } finally {
                    latch.countDown()
                }
            })
        }
        latch.await()
        futures.forEach { it.get() }
        executor.shutdown()

        val allNewUserBalance = userList.map { todoAuthService.findUserByUserId(it.userId).currentBalance() }

        /**
         * 20명의 유저가 한 투두를 사고팔면 첫 owner 인 유저1 을 제외하고 20명끼리 사고팔고 진행함.
         * 이때, 19명은 각각 사고 팔아서 500원을 가지고 있고, 마지막 구매한 유저만 잔액이 0원이게됨
         */
        val user = todoAuthService.findUserByUserId("pita1")
        assertThat(user.money.currentBalance()).isEqualTo(500L)
        assertThat(allNewUserBalance.count { it == 500L }).isEqualTo(9)
        assertThat(allNewUserBalance.count { it == 0L }).isEqualTo(1)
    }

    @Test
    fun `동시에 같은 todo를 두 구매자가 노려도 이중지급 없이 순차적으로만 처리된다`() {
        // buyer1, buyer2 생성 - 둘 다 500원 보유
        todoAuthService.signUp("buyer1", "buyer1")
        todoAuthService.signUp("buyer2", "buyer2")
        val token1 = todoAuthService.login("buyer1", "buyer1")
        val token2 = todoAuthService.login("buyer2", "buyer2")
        todoChargeService.charge(500L, token1.accessToken)
        todoChargeService.charge(500L, token2.accessToken)

        // pita1 소유의 500원짜리 todo를 buyer1, buyer2가 동시에 구매 시도
        val latch = CountDownLatch(2)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)
        val executor = Executors.newFixedThreadPool(2)

        executor.submit {
            try {
                todoTradeService.buyTodo(todo.id, "buyer1")
                successCount.incrementAndGet()
            } catch (e: Exception) {
                println("buyer1 실패: ${e.message}")
                failCount.incrementAndGet()
            } finally {
                latch.countDown()
            }
        }

        executor.submit {
            try {
                todoTradeService.buyTodo(todo.id, "buyer2")
                successCount.incrementAndGet()
            } catch (e: Exception) {
                println("buyer2 실패: ${e.message}")
                failCount.incrementAndGet()
            } finally {
                latch.countDown()
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        val finalSeller = todoAuthService.findUserByUserId("pita1")
        val finalBuyer1 = todoAuthService.findUserByUserId("buyer1")
        val finalBuyer2 = todoAuthService.findUserByUserId("buyer2")
        val finalTodo = todoService.getTodoById(todo.id)

        println("=== 결과 ===")
        println("성공: ${successCount.get()}, 실패: ${failCount.get()}")
        println("seller(pita1) 잔액: ${finalSeller.currentBalance()}")
        println("buyer1 잔액: ${finalBuyer1.currentBalance()}")
        println("buyer2 잔액: ${finalBuyer2.currentBalance()}")
        println("최종 owner: ${finalTodo.ownerId}")

        /**
         * 이 도메인은 "한 번 팔리면 끝"이 아니라 현재 소유자가 아니기만 하면 누구든 다시 살 수 있다.
         * buyer1, buyer2가 동시에 같은 todo를 노리면 "경쟁"이 아니라
         * pita1 → buyer1 → buyer2 로 이어지는 정상적인 2단계 재판매 체인이 되고,
         * 락이 트랜잭션 커밋 이후에 풀리도록 고쳤다면 둘 다 성공하는 게 맞다.
         *
         * 여기서 진짜 검증해야 할 건 "몇 명이 성공했냐"가 아니라
         * "pita1이 이중으로 돈을 받지 않았는가 / 총액이 보존됐는가"다.
         * 락 순서가 잘못됐다면(트랜잭션 커밋 전에 락이 풀렸다면) 두 스레드가
         * 동시에 owner=pita1을 읽어서 pita1이 1000원을 받는 이중지급이 발생했을 것이다.
         */
        assertThat(successCount.get()).isEqualTo(2)
        assertThat(failCount.get()).isEqualTo(0)

        // pita1은 정확히 한 번(500원)만 지급받아야 한다 — 이중지급 방지 검증
        assertThat(finalSeller.currentBalance()).isEqualTo(500L)

        // 두 buyer가 잃은 돈의 총합은 정확히 500원(pita1이 받은 금액)이어야 한다 — 총액 보존 검증
        val totalDeducted = (500L - finalBuyer1.currentBalance()) + (500L - finalBuyer2.currentBalance())
        assertThat(totalDeducted).isEqualTo(500L)

        // 최종 owner는 나중에 락을 잡은 쪽이어야 한다 (buyer1 또는 buyer2 중 하나)
        assertThat(finalTodo.ownerId).isIn("buyer1", "buyer2")
    }

    @Test
    fun `leaseTime을 초과하면 처리 중에도 락이 풀려 다른 스레드가 진입할 수 있다`() {
        val user1Started = CountDownLatch(1)
        val user1Finished = AtomicBoolean(false)
        val user2AcquiredWhileUser1StillRunning = AtomicBoolean(false)
        val executor = Executors.newFixedThreadPool(2)

        // user1: 락을 잡고 일부러 4초 동안 안 놓음 (leaseTime 3초 초과)
        executor.submit {
            redisLockManager.tryLock(key = todo.id.toString()) {
                user1Started.countDown()
                Thread.sleep(4000)
            }
            user1Finished.set(true)
        }

        user1Started.await() // user1이 락을 잡은 시점부터 시작

        // user2: 3.2초 뒤 같은 key로 재시도 (leaseTime은 지났지만 user1은 아직 안 끝남)
        executor.submit {
            Thread.sleep(3200)
            redisLockManager.tryLock(key = todo.id.toString()) {
                if (!user1Finished.get()) {
                    user2AcquiredWhileUser1StillRunning.set(true) // user1이 안 끝났는데 잡혔다!
                }
            }
        }

        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)

        // 이게 true면 = leaseTime 만료로 인한 동시 진입이 실제로 재현됨
        // false 면 진입하지 못함
        assertThat(user2AcquiredWhileUser1StillRunning.get()).isFalse()
    }

    @BeforeEach
    fun before() {
        val keys = redisTemplate.keys("*")
        redisTemplate.delete(keys)

        val user1 = User("pita1", "pita1")
        val user2 = User("pita2", "pita2")
        todoAuthService.signUp(user1.userId, user1.password)
        todoAuthService.signUp(user2.userId, user2.password)

        // 유저2 500원 충전
        val token = todoAuthService.login(user2.userId, user2.password)
        todoChargeService.charge(500L, token.accessToken)

        // 유저 1 todo 500원짜리로 한개 생성
        val user = todoAuthService.findUserByUserId("pita1")
        val request = TodoCreateServiceRequestDTO(
            todo = "테스트 500원 짜리 투두",
            price = 500L
        )
        todo = todoService.createTodo(request, user)
    }

    @AfterEach
    fun after() {
        todoRepository.deleteAll()
        todoAuthRepository.deleteAll()
    }
}
