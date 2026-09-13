package com.seokjoo.todo.domain.service.charge

import com.seokjoo.todo.annotation.TodoTest
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.repository.todouser.TodoAuthRepository
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@TodoTest
class TodoChargeConcurrencyTest @Autowired constructor(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val todoAuthService: TodoAuthService,
    private val todoAuthRepository: TodoAuthRepository,
    private val chargeService: TodoChargeService,
) {

    @Test
    fun `유저가 동시에 충전을 한다면 lost update 가 발생한다`() {
        val threadCount = 100
        val executor = Executors.newFixedThreadPool(32)
        val latch = CountDownLatch(threadCount)

        val user = todoAuthService.findUserByUserId("pita1")

        repeat(100) {
            executor.submit {
                try {
                    chargeService.charge(100, user.userId)
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        executor.shutdown()

        val finalUser = todoAuthService.findUserByUserId(user.userId)
        assertThat(finalUser.currentBalance()).isEqualTo(10_000L)
    }

    @BeforeEach
    fun before() {
        val keys = redisTemplate.keys("*")
        redisTemplate.delete(keys)

        val user1 = User("pita1", "pita1")
        val user2 = User("pita2", "pita2")
        todoAuthService.signUp(user1.userId, user1.password)
        todoAuthService.signUp(user2.userId, user2.password)
    }

    @AfterEach
    fun after() {
        todoAuthRepository.deleteAll()
    }
}
