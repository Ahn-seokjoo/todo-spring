package com.seokjoo.todo.presentation.charge.controller

import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.charge.TodoChargeService
import com.seokjoo.todo.presentation.charge.dto.TodoChargeRequest
import com.seokjoo.todo.presentation.charge.dto.TodoChargeResponse
import com.seokjoo.todo.presentation.charge.dto.TodoCurrentBalanceResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Charge", description = "금액 충전에 사용합니다")
@RestController
@RequestMapping("/api/v1")
class TodoChargeApiController(
    private val authService: TodoAuthService,
    private val chargeService: TodoChargeService,
) {

    @PostMapping("/charge/{userId}")
    fun chargeBalance(
        @PathVariable userId: String,
        @RequestBody @Valid request: TodoChargeRequest,
    ): TodoChargeResponse {
        val user = chargeService.charge(amount = request.amount, userId = userId)
        return TodoChargeResponse(userId = user.userId, amount = user.currentBalance())
    }

    @GetMapping("/current/{userId}")
    fun checkCurrentBalance(
        @PathVariable userId: String,
    ): TodoCurrentBalanceResponse {
        val user = authService.findUserByUserId(userId)
        return TodoCurrentBalanceResponse(balance = user.currentBalance())
    }
}
