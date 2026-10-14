package com.seokjoo.todo.domain.service.sales

import com.seokjoo.todo.domain.repository.purchase.TodoPurchaseRepository
import com.seokjoo.todo.presentation.sales.dto.DailySalesResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class TodoSalesStatsService(
    private val todoPurchaseRepository: TodoPurchaseRepository,
) {

    @Transactional(readOnly = true)
    fun getDailySales(sellerId: String, from: LocalDate, to: LocalDate): List<DailySalesResponse> {
        return todoPurchaseRepository
            .findDailySales(sellerId, from.atStartOfDay(), to.atStartOfDay())
            .map {
                DailySalesResponse(
                    date = it.salesDate,
                    count = it.salesCount,
                    amount = it.salesAmount,
                )
            }
    }
}
