package com.seokjoo.todo.domain.service.trade

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TodoTxService(
    private val todoService: TodoService,
    private val todoAuthService: TodoAuthService,
    private val todoRepository: TodoRepository,
) {
    @Transactional
    fun buyTodo(todoId: Long, buyerUserId: String): TodoServiceResponseDTO {
        /**
         * 또 다른 데드락으로, 기존과 같이 Todo 를 조회시에 TodoServiceResponseDTO.from 에서 owner를 조회해버림.
         * 이때 seller A,B가 데드락을 만듦. 즉, 조회를 seller먼저 해버리니 아래 id 정렬이 의미가 없어짐
         */
        // 1. todo id로 seller id만 미리 가져옴
        val sellerId = todoRepository.findTodoOwnerByTodoId(todoId)
            ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)

        check(sellerId != buyerUserId) { throw TodoException.of(TodoExceptionType.CAN_NOT_TRADE_OWN_TODO) }
        // 2. 미리 id 가 낮은 순서로 정렬
        val (lockFirstUserId, lockSecondUserId) = listOf(sellerId, buyerUserId).sorted()

        // jpa가 업데이트 쿼리를 조회 순서로 날리기 때문에 미리 조회
        val firstUser = todoAuthService.findUserByUserId(lockFirstUserId)
        val secondUser = todoAuthService.findUserByUserId(lockSecondUserId)

        val (buyer, seller) = if (lockFirstUserId == buyerUserId) {
            firstUser to secondUser
        } else {
            secondUser to firstUser
        }

        // 3. 금액이 충분한지 확인
        val todo = todoService.getTodoById(todoId)
        buyer.isAffordable(todo.price)

        // 4. 구매자 금액 차감 및 판매자 금액 추가
        buyer.decreaseBalance(todo.price)
        seller.increaseBalance(todo.price)

        // 5. owner"만" 변경
        return todoService.updateOwner(todoId = todo.id, owner = buyer)
    }
}
