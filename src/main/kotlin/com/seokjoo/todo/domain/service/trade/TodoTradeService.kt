package com.seokjoo.todo.domain.service.trade

import com.seokjoo.todo.common.redisson.LockManager
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import org.springframework.stereotype.Service

@Service
class TodoTradeService(
    private val todoTxService: TodoTxService,
    private val redisLockManager: LockManager,
) {
    fun buyTodo(todoId: Long, userId: String): TodoServiceResponseDTO {
        return redisLockManager.tryLock(key = todoId.toString()) { // 거래 하려는 todoId로 락을 잡음
            todoTxService.buyTodo(todoId = todoId, userId = userId)
        }
    }
}
