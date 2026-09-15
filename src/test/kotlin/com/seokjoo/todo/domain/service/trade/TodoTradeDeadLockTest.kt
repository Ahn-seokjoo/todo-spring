package com.seokjoo.todo.domain.service.trade

import com.seokjoo.todo.annotation.TodoTest
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import com.seokjoo.todo.domain.repository.todouser.TodoAuthRepository
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.charge.TodoChargeService
import com.seokjoo.todo.domain.service.todo.TodoCreateServiceRequestDTO
import com.seokjoo.todo.domain.service.todo.TodoService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.CannotAcquireLockException
import org.springframework.data.redis.core.RedisTemplate
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@TodoTest
class TodoTradeDeadLockTest @Autowired constructor(
    private val todoService: TodoService,
    private val todoChargeService: TodoChargeService,
    private val todoAuthService: TodoAuthService,
    private val todoAuthRepository: TodoAuthRepository,
    private val todoTradeService: TodoTradeService,
    private val todoRepository: TodoRepository,
    private val redisTemplate: RedisTemplate<String, Any>,
) {
    lateinit var user1Todos: List<Long>
    lateinit var user2Todos: List<Long>

    @Test
    fun `유저가 서로의 Todo를 사려고 할 때 데드락이 발생한다`() {
        val user1 = todoAuthService.findUserByUserId("pita1")
        val user2 = todoAuthService.findUserByUserId("pita2")

        val executor = Executors.newFixedThreadPool(32)
        val latch = CountDownLatch(user1Todos.size + user2Todos.size)

        val user1Futures = user1Todos.map { id ->
            executor.submit(Callable {
                try {
                    todoTradeService.buyTodo(id, user2.userId)
                } finally {
                    latch.countDown()
                }
            })
        }

        val user2Futures = user2Todos.map { id ->
            executor.submit(Callable {
                try {
                    todoTradeService.buyTodo(id, user1.userId)
                } finally {
                    latch.countDown()
                }
            })
        }
        latch.await(60, TimeUnit.SECONDS)
        val results = (user1Futures + user2Futures).map { runCatching { it.get(300, TimeUnit.SECONDS) } }
        executor.shutdown()

        val failures = results.mapNotNull { it.exceptionOrNull() }

        // 실패가 있다면 전부 CannotAcquireLockException(데드락)이 원인이어야 한다
        assertThat(failures).allSatisfy { throwable ->
            assertThat(throwable).hasCauseInstanceOf(CannotAcquireLockException::class.java)
        }
        // 실패 여부 체크
        assertThat(failures).isEmpty()

        // 금액 정합성 체크
        val finalUser1 = todoAuthService.findUserByUserId("pita1")
        val finalUser2 = todoAuthService.findUserByUserId("pita2")
        assertThat(finalUser1.currentBalance() + finalUser2.currentBalance()).isEqualTo(1_000_000L)

        // 완벽하게 둘이 교환완료했다면 개수도 동일
        val finalUser1Todos = todoAuthService.findUserWithTodosByUserId("pita1")
        val finalUser2Todos = todoAuthService.findUserWithTodosByUserId("pita2")
        assertThat(finalUser1Todos.todoList.size + finalUser2Todos.todoList.size).isEqualTo(40)
    }

    @BeforeEach
    fun before() {
        val keys = redisTemplate.keys("*")
        redisTemplate.delete(keys)

        val signUpUser1 = User("pita1", "pita1")
        val signUpUser2 = User("pita2", "pita2")
        todoAuthService.signUp(signUpUser1.userId, signUpUser1.password)
        todoAuthService.signUp(signUpUser2.userId, signUpUser2.password)

        // 유저 1,2  충전
        val user1 = todoAuthService.findUserByUserId(signUpUser1.userId)
        todoChargeService.charge(500_000L, user1.userId)
        val user2 = todoAuthService.findUserByUserId(signUpUser2.userId)
        todoChargeService.charge(500_000L, user2.userId)

        val createTodoUser1 = todoAuthService.findUserByUserId("pita1")
        val createTodoUser2 = todoAuthService.findUserByUserId("pita2")

        // 각 유저 투두 20개씩 생성
        user1Todos = (0 until 20).map { i ->
            val request = TodoCreateServiceRequestDTO(
                todo = "테스트 500원 짜리 투두 $i",
                price = 500L
            )
            val todo = todoService.createTodo(request, createTodoUser1)
            todo.id
        }

        user2Todos = (0 until 20).map { i ->
            val request = TodoCreateServiceRequestDTO(
                todo = "테스트 500원 짜리 투두 - 2 $i",
                price = 500L
            )
            val todo = todoService.createTodo(request, createTodoUser2)
            todo.id
        }
    }

    @AfterEach
    fun after() {
        todoRepository.deleteAll()
        todoAuthRepository.deleteAll()
    }
}
