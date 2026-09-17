package com.seokjoo.todo.domain.service.balance

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.repository.balance.TodoBalanceRepository
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TodoBalanceService(
    private val todoAuthService: TodoAuthService,
    private val todoBalanceRepository: TodoBalanceRepository,
) {
    @Transactional
    fun increaseBalance(amount: Long, userId: String): User {
        val owner = todoAuthService.findUserByUserId(userId)
        todoBalanceRepository.increaseBalanceIfSufficient(userId, amount)
        return owner
    }

    @Transactional
    fun decreaseBalance(amount: Long, userId: String): User {
        val owner = todoAuthService.findUserByUserId(userId)
        val decreased = todoBalanceRepository.decreaseBalanceIfSufficient(userId, amount) > 0
        if (!decreased) throw TodoException.of(TodoExceptionType.BALANCE_NOT_ENOUGH)
        return owner
    }

    @Transactional(readOnly = true)
    fun getBalance(userId: String): Long {
        return todoBalanceRepository.findBalanceByUserId(userId)
            ?: throw TodoException.of(TodoExceptionType.AUTH_USER_NOT_EXIST)
    }
}
