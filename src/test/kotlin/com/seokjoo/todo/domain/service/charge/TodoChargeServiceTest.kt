package com.seokjoo.todo.domain.service.charge

import com.seokjoo.todo.domain.service.auth.TodoAuthService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
class TodoChargeServiceTest @Autowired constructor(
    private val todoAuthService: TodoAuthService,
    private val todoChargeService: TodoChargeService,
) {
    @Test
    @Transactional
    fun `금액 충전을 하면 잘 된다`() {
        todoAuthService.signUp("pita", "pita")
        val user = todoAuthService.login("pita", "pita")

        todoChargeService.charge(1000L, user.refreshToken)

        val resultUser = todoAuthService.findUserByUserId("pita")

        assert(resultUser.money.currentBalance() == 1000L)
    }
}
