package com.seokjoo.todo.domain.entity.todouser

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType

data class Money(
    val balance: Long = 0L,
) {
    init {
        require(balance >= 0L) { throw TodoException.of(TodoExceptionType.BALANCE_CAN_NOT_BE_NEGATIVE) }
    }

    fun increase(balance: Long): Money {
        return copy(balance = this.balance + balance)
    }

    fun decrease(balance: Long): Money {
        check(this.balance - balance >= 0) { throw TodoException.of(TodoExceptionType.BALANCE_NOT_ENOUGH_MONEY) }
        return copy(balance = this.balance - balance)
    }
}
