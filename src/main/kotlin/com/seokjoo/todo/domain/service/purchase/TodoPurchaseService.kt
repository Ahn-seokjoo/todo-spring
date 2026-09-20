package com.seokjoo.todo.domain.service.purchase

import com.seokjoo.todo.common.redisson.RedisLockManager
import org.springframework.stereotype.Service

@Service
class TodoPurchaseService(
    private val redisLockManager: RedisLockManager,
    private val todoPurchaseTxService: TodoPurchaseTxService,
) {
    fun purchaseTodo(todoId: Long, buyerId: String) {
        redisLockManager.tryLock(key = todoId.toString()) {
            todoPurchaseTxService.purchaseTodo(todoId, buyerId)
        }
    }

    fun approvePurchaseTodo(todoId: Long, sellerId: String) {
        redisLockManager.tryLock(key = todoId.toString()) {
            todoPurchaseTxService.approvePurchaseTodo(todoId, sellerId)
        }
    }
}
