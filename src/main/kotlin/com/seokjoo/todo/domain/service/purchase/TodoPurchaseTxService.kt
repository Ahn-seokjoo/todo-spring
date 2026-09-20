package com.seokjoo.todo.domain.service.purchase

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.purchase.Purchase
import com.seokjoo.todo.domain.entity.todo.TodoStatus
import com.seokjoo.todo.domain.repository.purchase.TodoPurchaseRepository
import com.seokjoo.todo.domain.service.balance.TodoBalanceService
import com.seokjoo.todo.domain.service.todo.TodoService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TodoPurchaseTxService(
    private val todoService: TodoService,
    private val todoPurchaseRepository: TodoPurchaseRepository,
    private val balanceService: TodoBalanceService,
) {
    @Transactional
    fun purchaseTodo(todoId: Long, buyerId: String) {
        // 자신의 Todo 인지 체크
        val todo = todoService.getTodoById(todoId)
        val sellerId = todo.ownerId
        check(sellerId != buyerId) { throw TodoException.of(TodoExceptionType.CAN_NOT_TRADE_OWN_TODO) }

        // 구매 대기중이라면 구매할 수 없음
        check(todo.status != TodoStatus.PENDING_APPROVAL) { throw TodoException.of(TodoExceptionType.CAN_NOT_PURCHASE) }
        // Todo 상태 Pending 으로 변경
        todoService.changeStatus(todoId = todo.id, status = TodoStatus.PENDING_APPROVAL)

        // 구매 요청 내역 생성
        val purchase = Purchase(sellerId = sellerId, buyerId = buyerId, todoId = todoId, price = todo.price)
        todoPurchaseRepository.save(purchase)

        // buyer 잔액 선 차감
        balanceService.decreaseBalance(amount = todo.price, userId = buyerId)
    }
}
