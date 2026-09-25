package com.seokjoo.todo.presentation.trade.controller

import com.seokjoo.todo.common.jwt.getBearerToken
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.purchase.TodoPurchaseService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
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
    ): ResponseEntity<String> {
        val buyerId = authService.getSubject(servletRequest.getBearerToken())
        todoPurchaseService.purchaseTodo(todoId = todoId, buyerId = buyerId)
        return ResponseEntity.ok("구매 요청 성공")
    }

    @PostMapping("/purchase/{todoId}/approve")
    fun approve(
        servletRequest: HttpServletRequest,
        @PathVariable("todoId") todoId: Long,
    ): ResponseEntity<String> {
        val sellerId = authService.getSubject(servletRequest.getBearerToken())
        todoPurchaseService.approvePurchaseTodo(todoId = todoId, sellerId = sellerId)
        return ResponseEntity.ok("구매 요청 승인 성공")
    }

    @PostMapping("/purchase/{todoId}/reject")
    fun reject(
        servletRequest: HttpServletRequest,
        @PathVariable("todoId") todoId: Long,
    ): ResponseEntity<String> {
        val sellerId = authService.getSubject(servletRequest.getBearerToken())
        todoPurchaseService.rejectPurchaseTodo(todoId = todoId, sellerId = sellerId)
        return ResponseEntity.ok("구매 요청 거절 성공")
    }

    @PostMapping("/purchase/{todoId}/cancel")
    fun cancel(
        servletRequest: HttpServletRequest,
        @PathVariable("todoId") todoId: Long,
    ): ResponseEntity<String> {
        val buyerId = authService.getSubject(servletRequest.getBearerToken())
        todoPurchaseService.cancelPurchaseTodo(todoId = todoId, buyerId = buyerId)
        return ResponseEntity.ok("구매 요청 취소 성공")
    }
}
