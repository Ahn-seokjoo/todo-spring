package com.seokjoo.todo.domain.service.charge

import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TodoChargeService(
    private val todoAuthService: TodoAuthService,
) {

    @Transactional
    fun charge(amount: Long, accessToken: String): User {
        val owner = todoAuthService.findUser(accessToken)
        owner.increaseBalance(amount)
        return owner
    }
}
