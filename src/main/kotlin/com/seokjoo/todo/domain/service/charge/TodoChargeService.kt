package com.seokjoo.todo.domain.service.charge

import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.service.balance.TodoBalanceService
import org.springframework.stereotype.Service

@Service
class TodoChargeService(
    private val todoBalanceService: TodoBalanceService,
) {
    fun charge(amount: Long, userId: String): User {
        return todoBalanceService.increaseBalance(amount, userId)
    }
}
