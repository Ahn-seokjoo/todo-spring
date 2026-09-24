package com.seokjoo.todo.domain.service.purchase

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.purchase.Purchase
import com.seokjoo.todo.domain.entity.purchase.PurchaseStatus
import com.seokjoo.todo.domain.entity.todo.TodoStatus
import com.seokjoo.todo.domain.repository.purchase.TodoPurchaseRepository
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.balance.TodoBalanceService
import com.seokjoo.todo.domain.service.outbox.OutboxListenerType
import com.seokjoo.todo.domain.service.outbox.PublishableEvent
import com.seokjoo.todo.domain.service.outbox.listener.event.TodoPurchaseEvent
import com.seokjoo.todo.domain.service.outbox.service.OutboxService
import com.seokjoo.todo.domain.service.todo.TodoService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TodoPurchaseTxService(
    private val todoService: TodoService,
    private val todoPurchaseRepository: TodoPurchaseRepository,
    private val balanceService: TodoBalanceService,
    private val todoAuthService: TodoAuthService,
    private val outboxService: OutboxService,
    private val eventPublisher: ApplicationEventPublisher,
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

        val purchaseEvent = PublishableEvent.PurchaseRequestEvent(
            sellerId = sellerId,
            buyerId = buyerId,
            todoId = todoId,
            price = todo.price,
            purchaseId = purchase.id,
        )

        val event = outboxService.createOutboxEvent(event = purchaseEvent, listener = OutboxListenerType.EMAIL_NOTIFICATION)
        val outboxEvent = outboxService.saveOutbox(event = event)
        eventPublisher.publishEvent(
            TodoPurchaseEvent(outboxEventId = outboxEvent.id)
        )
    }

    @Transactional
    fun approvePurchaseTodo(todoId: Long, sellerId: String) {
        // 자신의 Todo 인지 체크
        val todo = todoService.getTodoById(todoId)
        check(todo.ownerId == sellerId) { throw TodoException.of(TodoExceptionType.UNAUTHORIZED_TODO_ACCESS) }

        // AVAILABLE 이라면 굳이 아래 로직을 탈필요 없음
        check(todo.status != TodoStatus.AVAILABLE) { throw TodoException.of(TodoExceptionType.NOT_PENDING) }

        // 내역서 확인
        val purchase = todoPurchaseRepository.findByTodoIdAndPurchaseStatus(todoId, PurchaseStatus.PENDING)
            ?: throw TodoException.of(TodoExceptionType.CAN_NOT_FOUND_PURCHASE)
        check(purchase.sellerId == sellerId) {
            throw TodoException.of(TodoExceptionType.UNAUTHORIZED_TODO_ACCESS)
        }
        val buyer = todoAuthService.findUserByUserId(purchase.buyerId)

        // 상태 다시 AVAILABLE 하게 수정
        todoService.changeStatus(todoId = todo.id, status = TodoStatus.AVAILABLE)

        // 주문 내역서 업데이트
        val updateSuccessCount = todoPurchaseRepository.updatePurchaseStatus(todoId, PurchaseStatus.APPROVED)
        check(updateSuccessCount == 1) { throw TodoException.of(TodoExceptionType.NOT_PENDING) }

        // todo owner 변경
        todoService.updateOwner(todoId = todoId, owner = buyer)

        // seller 금액 증가
        balanceService.increaseBalance(amount = todo.price, userId = sellerId)
    }

    @Transactional
    fun rejectPurchaseTodo(todoId: Long, sellerId: String) {
        // 자신의 Todo 인지 체크
        val todo = todoService.getTodoById(todoId)
        check(todo.ownerId == sellerId) { throw TodoException.of(TodoExceptionType.UNAUTHORIZED_TODO_ACCESS) }

        // AVAILABLE 이라면 굳이 아래 로직을 탈필요 없음
        check(todo.status != TodoStatus.AVAILABLE) { throw TodoException.of(TodoExceptionType.NOT_PENDING) }

        // 상태 다시 AVAILABLE 하게 수정
        todoService.changeStatus(todoId = todo.id, status = TodoStatus.AVAILABLE)

        // 업데이트 이전에 pending된 구매요청서 찾아옴
        val purchase = todoPurchaseRepository.findByTodoIdAndPurchaseStatus(todoId, PurchaseStatus.PENDING)
            ?: throw TodoException.of(TodoExceptionType.CAN_NOT_FOUND_PURCHASE)

        // 주문 내역서 업데이트
        val updateSuccessCount = todoPurchaseRepository.updatePurchaseStatus(todoId, PurchaseStatus.REJECTED)
        check(updateSuccessCount == 1) { throw TodoException.of(TodoExceptionType.NOT_PENDING) }

        // buyer 잔액 다시 증가
        balanceService.increaseBalance(amount = todo.price, userId = purchase.buyerId)
    }

    @Transactional
    fun cancelPurchaseTodo(todoId: Long, buyerId: String) {
        // 자신의 Todo 인지 체크
        val todo = todoService.getTodoById(todoId)
        check(todo.ownerId != buyerId) { throw TodoException.of(TodoExceptionType.CAN_NOT_CANCEL_OWN_TODO) }

        // AVAILABLE 이라면 굳이 아래 로직을 탈필요 없음
        check(todo.status != TodoStatus.AVAILABLE) { throw TodoException.of(TodoExceptionType.NOT_PENDING) }

        // 업데이트 이전에 pending된 구매요청서 찾아옴
        val purchase = todoPurchaseRepository.findByTodoIdAndPurchaseStatus(todoId, PurchaseStatus.PENDING)
            ?: throw TodoException.of(TodoExceptionType.CAN_NOT_FOUND_PURCHASE)
        check(purchase.buyerId == buyerId) {
            throw TodoException.of(TodoExceptionType.YOU_ARE_NOT_BUYER)
        }

        // 상태 다시 AVAILABLE 하게 수정
        todoService.changeStatus(todoId = todo.id, status = TodoStatus.AVAILABLE)

        // 주문 내역서 업데이트
        val updateSuccessCount = todoPurchaseRepository.updatePurchaseStatus(todoId, PurchaseStatus.CANCELLED)
        check(updateSuccessCount == 1) { throw TodoException.of(TodoExceptionType.NOT_PENDING) }

        // buyer 잔액 다시 증가
        balanceService.increaseBalance(amount = todo.price, userId = purchase.buyerId)
    }
}
