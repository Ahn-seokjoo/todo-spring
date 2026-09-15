package com.seokjoo.todo.domain.service.trade

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TodoTxService(
    private val todoService: TodoService,
    private val todoAuthService: TodoAuthService,
) {
    @Transactional
    fun buyTodo(todoId: Long, buyerUserId: String): TodoServiceResponseDTO {
        // 1. 자기 todo 인지 확인
        val todo = todoService.getTodoById(todoId)
        val sellerId = todo.ownerId

        check(sellerId != buyerUserId) { throw TodoException.of(TodoExceptionType.CAN_NOT_TRADE_OWN_TODO) }
        // 2. 미리 id 가 낮은 순서로 정렬
        val (lockFirstUserId, lockSecondUserId) = listOf(todo.ownerId, buyerUserId).sorted()

        // jpa가 업데이트 쿼리를 조회 순서로 날리기 때문에 미리 조회
        val firstUser = todoAuthService.findUserByUserId(lockFirstUserId)
        val secondUser = todoAuthService.findUserByUserId(lockSecondUserId)

        val (buyer, seller) = if (lockFirstUserId == buyerUserId) {
            firstUser to secondUser
        } else {
            secondUser to firstUser
        }

        // 3. 금액이 충분한지 확인
        buyer.isAffordable(todo.price)

        // 4. 구매자 금액 차감 및 판매자 금액 추가
        buyer.decreaseBalance(todo.price)
        seller.increaseBalance(todo.price)

        // 5. owner"만" 변경
        return todoService.updateOwner(todoId = todo.id, owner = buyer)
    }
}
