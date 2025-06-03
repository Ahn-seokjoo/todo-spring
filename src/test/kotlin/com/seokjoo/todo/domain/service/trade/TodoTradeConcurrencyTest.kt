package com.seokjoo.todo.domain.service.trade

import com.seokjoo.todo.annotation.IntegrationTest
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.charge.TodoChargeService
import com.seokjoo.todo.domain.service.todo.TodoCreateServiceRequestDTO
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@IntegrationTest
class TodoTradeConcurrencyTest @Autowired constructor(
    private val todoService: TodoService,
    private val todoChargeService: TodoChargeService,
    private val todoAuthService: TodoAuthService,
    private val todoTradeService: TodoTradeService,
    private val todoRepository: TodoRepository,
    private val redisTemplate: RedisTemplate<String, Any>,
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
                    todoTradeService.buyTodo(todo.id, user2)
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()

        val todo = todoService.getTodoById(todo.id)
        assert(todo.todo == "테스트 500원 짜리 투두")
        assert(todo.price == 500L)
        assert(todo.ownerId == user2.userId)
        assert(user2.currentBalance() == 0L)

        val user1 = todoAuthService.findUserByUserId("pita1")
        assert(user1.currentBalance() == 500L)
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
        todoChargeService.charge(500L, token.refreshToken)

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
        todoAuthService.delete("pita1", "pita1")
        todoAuthService.delete("pita2", "pita2")
    }
}
