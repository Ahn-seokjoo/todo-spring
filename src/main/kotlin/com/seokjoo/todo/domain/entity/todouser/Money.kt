package com.seokjoo.todo.domain.entity.todouser

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType

@JvmInline
value class Money(
    private val balance: Long = 0L,
) {
    init {
        require(balance >= 0L) { throw TodoException.of(TodoExceptionType.BALANCE_CAN_NOT_BE_NEGATIVE) }
    }
}
