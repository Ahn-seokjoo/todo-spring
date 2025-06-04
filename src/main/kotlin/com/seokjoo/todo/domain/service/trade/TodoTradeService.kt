package com.seokjoo.todo.domain.service.trade

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.common.redisson.RedisLockManager
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TodoTradeService(
    private val todoService: TodoService,
    private val todoAuthService: TodoAuthService,
    private val redisLockManager: RedisLockManager,
) {
    @Transactional
    fun buyTodo(todoId: Long, userId: String): TodoServiceResponseDTO {
        return redisLockManager.tryLock(key = todoId.toString()) { // 가입시 user_id로 가입 유무를 체크하기 때문에 고유함
            // 1. 자기 todo 인지 확인
            val todo = todoService.getTodoById(todoId)

            check(todo.ownerId != userId) { throw TodoException.of(TodoExceptionType.CAN_NOT_TRADE_OWN_TODO) }
            // 2. 아니라면 구매자의 금액이 충분한지 확인
            val buyer = todoAuthService.findUserByUserId(userId)
            buyer.isAffordable(todo.price)
            // 3. 판매처리하기
            // 3-1 구매자 금액 차감
            buyer.decreaseBalance(todo.price)
            // 3-2 판매자 금액 증가
            val seller = todoAuthService.findUserByUserId(todo.ownerId)
            seller.increaseBalance(todo.price)
            // 3-2 owner"만" 변경
            todoService.updateOwner(todoId = todo.id, owner = buyer)
        }
    }
}
