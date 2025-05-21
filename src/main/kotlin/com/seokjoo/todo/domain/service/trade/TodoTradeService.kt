package com.seokjoo.todo.domain.service.trade

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import com.seokjoo.todo.domain.service.todo.TodoUpdateServiceRequestDTO
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TodoTradeService(
    private val todoService: TodoService,
) {
    @Transactional
    fun buyTodo(todoId: Long, user: User): TodoServiceResponseDTO {
        // 1. 자기 todo 인지 확인
        val todo = todoService.getTodoById(todoId)
        if (todo.ownerId == user.userId) throw TodoException.of(TodoExceptionType.CAN_NOT_TRADE_OWN_TODO)
        // 2. 아니라면 금액 충분한지 확인
        if (user.money.balance < todo.price) throw TodoException.of(TodoExceptionType.BALANCE_NOT_ENOUGH)
        // 3. 판매처리하기
        // 3-1 금액 차감
        user.decreaseBalance(todo.price)
        // 3-2 owner"만" 변경
        return todoService.updateOwner(todoId = todo.id, owner = user)
        // TODO 4 상대방 금액 올려주기
    }
}
