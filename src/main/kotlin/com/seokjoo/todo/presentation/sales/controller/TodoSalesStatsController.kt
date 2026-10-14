package com.seokjoo.todo.presentation.sales.controller

import com.seokjoo.todo.common.jwt.getBearerToken
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.sales.TodoSalesStatsService
import com.seokjoo.todo.presentation.sales.dto.DailySalesResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("api/v1/sales")
class TodoSalesStatsController(
    private val authService: TodoAuthService,
    private val salesStatsService: TodoSalesStatsService,
) {

    @GetMapping("/daily")
    fun getDailySales(
        servletRequest: HttpServletRequest,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): ResponseEntity<List<DailySalesResponse>> {
        val sellerId = authService.getSubject(servletRequest.getBearerToken())
        val dailySales = salesStatsService.getDailySales(sellerId, from, to)
        return ResponseEntity.ok(dailySales)
    }
}
