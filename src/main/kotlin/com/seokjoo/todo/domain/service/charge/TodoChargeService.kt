package com.seokjoo.todo.domain.service.charge

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.repository.todouser.TodoAuthRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TodoChargeService(
    private val authRepository: TodoAuthRepository,
) {

    @Transactional
    fun charge(amount: Long, owner: User) {
        val user = authRepository.findUserByUserId(owner.userId)
            ?: throw TodoException.of(TodoExceptionType.AUTH_USER_NOT_EXIST)
        user.increaseBalance(amount)
    }
}
