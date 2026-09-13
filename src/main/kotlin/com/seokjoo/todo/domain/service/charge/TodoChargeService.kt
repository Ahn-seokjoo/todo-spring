package com.seokjoo.todo.domain.service.charge

import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TodoChargeService(
    private val todoAuthService: TodoAuthService,
) {
    @Retryable(
        retryFor = [ObjectOptimisticLockingFailureException::class],
        maxAttempts = 5,
        backoff = Backoff(delay = 100L, maxDelay = 3000L, random = true, multiplier = 2.0)
    )
    @Transactional
    fun charge(amount: Long, userId: String): User {
        val owner = todoAuthService.findUserByUserId(userId)
        owner.increaseBalance(amount)
        return owner
    }
}
