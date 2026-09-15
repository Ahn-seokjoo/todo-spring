package com.seokjoo.todo.domain.service.charge

import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.service.balance.TodoBalanceService
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service

@Service
class TodoChargeService(
    private val todoBalanceService: TodoBalanceService,
) {
    @Retryable(
        retryFor = [ObjectOptimisticLockingFailureException::class],
        maxAttempts = 8,
        backoff = Backoff(delay = 100L, maxDelay = 3200L, random = true, multiplier = 2.0)
    )
    fun charge(amount: Long, userId: String): User {
        return todoBalanceService.increaseBalance(amount, userId)
    }
}
