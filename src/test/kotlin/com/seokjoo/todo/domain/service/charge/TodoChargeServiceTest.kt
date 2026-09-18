package com.seokjoo.todo.domain.service.charge

import com.seokjoo.todo.annotation.TodoTest
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.balance.TodoBalanceService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

@TodoTest
class TodoChargeServiceTest @Autowired constructor(
    private val todoAuthService: TodoAuthService,
    private val todoChargeService: TodoChargeService,
    private val todoBalanceService: TodoBalanceService,
) {
    @Test
    @Transactional
    fun `금액 충전을 하면 잘 된다`() {
        todoAuthService.signUp("pita", "pita")

        val user = todoAuthService.findUserByUserId("pita")
        todoChargeService.charge(1000L, user.userId)

        val resultUser = todoAuthService.findUserByUserId("pita")

        assert(todoBalanceService.getBalance(resultUser.userId) == 1000L)
    }
}
