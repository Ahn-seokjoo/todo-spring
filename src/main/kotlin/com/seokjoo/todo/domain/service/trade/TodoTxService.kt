package com.seokjoo.todo.domain.service.trade

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import com.seokjoo.todo.domain.service.balance.TodoBalanceService
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TodoTxService(
    private val todoService: TodoService,
    private val todoRepository: TodoRepository,
    private val todoBalanceService: TodoBalanceService,
) {
    @Transactional
    fun buyTodo(todoId: Long, buyerUserId: String): TodoServiceResponseDTO {
        // 1. todo id로 seller id 와 todo 금액만 미리 가져옴
        val sellerId = todoRepository.findTodoOwnerByTodoId(todoId)
            ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)
        val todoPrice = todoRepository.findTodoPriceByTodoId(todoId)
            ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)

        check(sellerId != buyerUserId) { throw TodoException.of(TodoExceptionType.CAN_NOT_TRADE_OWN_TODO) }
        // 2. 미리 id 가 낮은 순서로 정렬
        val (lockFirstUserId, lockSecondUserId) = listOf(sellerId, buyerUserId).sorted()

        // 3. id 순서대로 금액 증가/감소, Lock을 잡지 않는 대신, 반드시 sort된 순서대로 update 할 것. 그렇지 않으면 데드락 발생
        return if (lockFirstUserId == buyerUserId) {
            val buyer = todoBalanceService.decreaseBalance(amount = todoPrice, userId = buyerUserId)
            val seller = todoBalanceService.increaseBalance(amount = todoPrice, userId = sellerId)
            todoService.updateOwner(todoId = todoId, owner = buyer)
        } else {
            val seller = todoBalanceService.increaseBalance(amount = todoPrice, userId = sellerId)
            val buyer = todoBalanceService.decreaseBalance(amount = todoPrice, userId = buyerUserId)
            todoService.updateOwner(todoId = todoId, owner = buyer)
        }
    }
}
