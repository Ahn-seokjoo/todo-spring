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
                    todoTradeService.buyTodo(todo.id, user2.userId)
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()

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
    fun `20명이 같은 todo를 동시에 구매할 때 모두 다 구매가 잘된다`() {
        val userList = (3..23).map {
            val user = User("pita$it", "pita$it")
            todoAuthService.signUp(user.userId, user.password)
            val token = todoAuthService.login(user.userId, user.password)
            todoChargeService.charge(500L, token.refreshToken)
            todoAuthService.findUser(token.refreshToken)
        }

        val executor = Executors.newFixedThreadPool(32)
        val latch = CountDownLatch(20)

        userList.map {
            executor.submit {
                try {
                    todoTradeService.buyTodo(todo.id, it.userId)
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()

        val newUserList = userList.map {
            todoAuthService.findUserByUserId(it.userId)
        }
        val allNewUserBalance = newUserList.map { it.money }

        /**
         * 21명의 유저가 한 투두를 사고팔면 첫 owner 인 유저1 을 제외하고 21명끼리 사고팔고 진행함.
         * 이때, 20명은 각각 사고 팔아서 500원을 가지고 있고, 마지막 구매한 유저만 잔액이 0원이게됨
         */
        val user = todoAuthService.findUserByUserId("pita1")
        assert(user.money.currentBalance() == 500L)
        assert(allNewUserBalance.count { it.currentBalance() == 500L } == 20)
        assert(allNewUserBalance.count { it.currentBalance() == 0L } == 1)
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
