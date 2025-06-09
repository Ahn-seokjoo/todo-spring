package com.seokjoo.todo.presentation.trade.controller

import com.seokjoo.todo.common.jwt.getBearerToken
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.trade.TodoTradeService
import com.seokjoo.todo.presentation.todo.dto.response.TodoResponse
import com.seokjoo.todo.presentation.todo.dto.response.toResponse
import com.seokjoo.todo.presentation.trade.dto.request.TodoBuyRequest
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
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
        @Valid @RequestBody buyRequest: TodoBuyRequest,
    ): TodoResponse {
        val userId = authService.getSubject(servletRequest.getBearerToken())

        return todoTradeService.buyTodo(buyRequest.todoId, userId).toResponse()
    }
}
