package com.seokjoo.todo.presentation.charge.controller

import com.seokjoo.todo.common.jwt.getBearerToken
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.charge.TodoChargeService
import com.seokjoo.todo.presentation.charge.dto.TodoChargeRequest
import com.seokjoo.todo.presentation.charge.dto.TodoChargeResponse
import com.seokjoo.todo.presentation.charge.dto.TodoCurrentBalanceResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
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

    @PostMapping("/charge")
    fun chargeBalance(
        servletRequest: HttpServletRequest,
        @RequestBody @Validated request: TodoChargeRequest,
    ): TodoChargeResponse {
        val user = chargeService.charge(amount = request.amount, accessToken = servletRequest.getBearerToken())
        return TodoChargeResponse(userId = user.userId, amount = user.money.currentBalance())
    }

    @GetMapping("/current")
    fun checkCurrentBalance(
        servletRequest: HttpServletRequest,
    ): TodoCurrentBalanceResponse {
        val user = authService.findUser(servletRequest.getBearerToken())
        return TodoCurrentBalanceResponse(balance = user.money.currentBalance())
    }
}
