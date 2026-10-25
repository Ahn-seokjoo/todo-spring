package com.seokjoo.todo.domain.service.stats

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.purchase.DailySalesRow
import com.seokjoo.todo.domain.repository.purchase.TodoPurchaseRepository
import com.seokjoo.todo.domain.repository.stats.DailySalesStatsRepository
import com.seokjoo.todo.domain.service.sales.TodoSalesStatsService
import com.seokjoo.todo.presentation.sales.dto.DailySalesResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

class TodoSalesStatsServiceTest {
    private val todoPurchaseRepository: TodoPurchaseRepository = mockk()
    private val dailySalesStatsRepository: DailySalesStatsRepository = mockk()
    private val service = TodoSalesStatsService(todoPurchaseRepository, dailySalesStatsRepository)

    @ParameterizedTest(name = "from={0}, to={1}")
    @CsvSource(
        "2026-10-19, 2026-10-19", // from == to: 빈 구간
        "2026-10-20, 2026-10-19", // from > to: 뒤집힌 구간
    )
    fun `from이 to보다 앞서지 않으면 SALES_INVALID_DATE_RANGE를 던지고 DB를 조회하지 않는다`(from: LocalDate, to: LocalDate) {
        val exception = assertThrows<TodoException> {
            service.getDailySalesStats("pita", from, to)
        }

        assertThat(exception.errorCode).isEqualTo(TodoExceptionType.SALES_INVALID_DATE_RANGE.errorCode)
        verify(exactly = 0) { dailySalesStatsRepository.findDailySalesStats(any(), any(), any()) }
    }

    @Test
    fun `유효한 구간이면 리포지토리 결과를 순서 그대로 응답으로 변환한다`() {
        val from = LocalDate.of(2026, 10, 19)
        val to = LocalDate.of(2026, 10, 21)
        every { dailySalesStatsRepository.findDailySalesStats("pita", from, to) } returns listOf(
            row(LocalDate.of(2026, 10, 19), count = 1L, amount = 1_000L),
            row(LocalDate.of(2026, 10, 20), count = 2L, amount = 2_000L),
        )

        val result = service.getDailySalesStats("pita", from, to)

        assertThat(result).containsExactly(
            DailySalesResponse(date = LocalDate.of(2026, 10, 19), count = 1L, amount = 1_000L),
            DailySalesResponse(date = LocalDate.of(2026, 10, 20), count = 2L, amount = 2_000L),
        )
    }

    private fun row(date: LocalDate, count: Long, amount: Long) = object : DailySalesRow {
        override val salesDate = date
        override val salesCount = count
        override val salesAmount = amount
    }
}
