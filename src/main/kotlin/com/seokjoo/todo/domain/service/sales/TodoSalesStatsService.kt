package com.seokjoo.todo.domain.service.sales

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
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
        // 조회 구간은 [from, to): from은 포함, to는 미포함. from >= to면 빈 구간이라 잘못된 요청으로 본다.
        if (!from.isBefore(to)) {
            throw TodoException.of(TodoExceptionType.SALES_INVALID_DATE_RANGE)
        }
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
