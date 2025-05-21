package com.seokjoo.todo.presentation.trade.controller

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.common.jwt.getBearerToken
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.trade.TodoTradeService
import com.seokjoo.todo.presentation.todo.dto.response.TodoResponse
import com.seokjoo.todo.presentation.todo.dto.response.toResponse
import com.seokjoo.todo.presentation.trade.dto.request.TodoBuyRequest
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Trade", description = "Todo 사고팔기 API")
@RestController
@RequestMapping("/api/v1")
class TodoTradeApiController(
    private val todoTradeService: TodoTradeService,
    private val authService: TodoAuthService,
) {
    @PostMapping("/trade")
    fun buyTodo(
        servletRequest: HttpServletRequest,
        @Validated @RequestBody buyRequest: TodoBuyRequest,
    ): TodoResponse {
        val user = authService.findUser(servletRequest.getBearerToken())
        // 잔액이 0원이면 다른 요청 없이 끝냄
        // 임의로 천원 충전 -> charge로 분리
        user.increaseBalance(1000L)

        if (user.money.balance == 0L) throw TodoException.of(TodoExceptionType.BALANCE_NOT_ENOUGH)
        return todoTradeService.buyTodo(buyRequest.todoId, user).toResponse()
    }
}
