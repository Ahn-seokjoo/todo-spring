package com.seokjoo.todo.domain.service.trade

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TodoTradeService(
    private val todoService: TodoService,
    private val todoAuthService: TodoAuthService,
) {
    @Transactional
    fun buyTodo(todoId: Long, user: User): TodoServiceResponseDTO {
        // 1. 자기 todo 인지 확인
        val todo = todoService.getTodoById(todoId)
        if (todo.ownerId == user.userId) throw TodoException.of(TodoExceptionType.CAN_NOT_TRADE_OWN_TODO)
        // 2. 아니라면 금액 충분한지 확인
        user.money.isOutOfBalance(todo.price)
        // 3. 판매처리하기
        // 3-1 금액 차감
        user.decreaseBalance(todo.price)
        // 3-2 상대방 금액 증가
        todoAuthService.findUserByUserId(todo.ownerId).increaseBalance(todo.price)

        // 3-2 owner"만" 변경
        return todoService.updateOwner(todoId = todo.id, owner = user)
    }
}
