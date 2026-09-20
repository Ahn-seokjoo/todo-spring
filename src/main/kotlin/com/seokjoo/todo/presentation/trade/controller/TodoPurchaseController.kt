package com.seokjoo.todo.presentation.trade.controller

import com.seokjoo.todo.common.jwt.getBearerToken
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.purchase.TodoPurchaseService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class TodoPurchaseController(
    private val authService: TodoAuthService,
    private val todoPurchaseService: TodoPurchaseService,
) {
    @PostMapping("/purchase/{todoId}")
    fun purchase(
        servletRequest: HttpServletRequest,
        @PathVariable("todoId") todoId: Long,
    ) {
        val buyerId = authService.getSubject(servletRequest.getBearerToken())
        todoPurchaseService.purchaseTodo(todoId = todoId, buyerId = buyerId)
    }
}
