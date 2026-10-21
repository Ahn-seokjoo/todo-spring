package com.seokjoo.todo.domain.service.stats

import com.seokjoo.todo.annotation.TodoTest
import com.seokjoo.todo.domain.entity.stats.DailySalesStatsId
import com.seokjoo.todo.domain.repository.stats.DailySalesStatsRepository
import com.seokjoo.todo.domain.service.sales.TodoSalesStatsRecordService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@TodoTest
@Transactional
class TodoSalesStatsRecordServiceTest @Autowired constructor(
    private val service: TodoSalesStatsRecordService,
    private val dailySalesStatsRepository: DailySalesStatsRepository,
) {
    @Test
    fun `같은 eventId로 두 번 기록하면 두 번째는 건너뛰고 한 번만 집계된다`() {
        val eventId = UUID.randomUUID().toString()
        val sellerId = "seller-${UUID.randomUUID().toString().take(8)}"
        val salesDate = LocalDate.of(2026, 10, 19)

        val first = service.record(eventId = eventId, amount = 100L, sellerId = sellerId, salesDate = salesDate)
        val second = service.record(eventId = eventId, amount = 100L, sellerId = sellerId, salesDate = salesDate)

        assertThat(first).isTrue()
        assertThat(second).isFalse()

        val stats = dailySalesStatsRepository.findById(DailySalesStatsId(salesDate, sellerId)).orElseThrow()
        assertThat(stats.salesCount).isEqualTo(1L)
        assertThat(stats.salesAmount).isEqualTo(100L)
    }

    @Test
    fun `다른 eventId로 두 번 기록하면, 두번 집계 된다`() {
        val firstEventId = UUID.randomUUID().toString()
        val secondEventId = UUID.randomUUID().toString()
        val sellerId = "seller-${UUID.randomUUID().toString().take(8)}"
        val salesDate = LocalDate.of(2026, 10, 19)

        val first = service.record(eventId = firstEventId, amount = 100L, sellerId = sellerId, salesDate = salesDate)
        val second = service.record(eventId = secondEventId, amount = 200L, sellerId = sellerId, salesDate = salesDate)

        assertThat(first).isTrue()
        assertThat(second).isTrue()

        val stats = dailySalesStatsRepository.findById(DailySalesStatsId(salesDate, sellerId)).orElseThrow()
        assertThat(stats.salesCount).isEqualTo(2L)
        assertThat(stats.salesAmount).isEqualTo(300L)
    }

    @Test
    fun `다른 날짜로 넣게 되면 각각 집계된다`() {
        val eventId = UUID.randomUUID().toString()
        val secondEventId = UUID.randomUUID().toString()
        val sellerId = "seller-${UUID.randomUUID().toString().take(8)}"
        val firstSalesDate = LocalDate.of(2026, 10, 19)
        val secondSalesDate = LocalDate.of(2026, 10, 20)

        val first = service.record(eventId = eventId, amount = 100L, sellerId = sellerId, salesDate = firstSalesDate)
        val second =
            service.record(eventId = secondEventId, amount = 200L, sellerId = sellerId, salesDate = secondSalesDate)

        assertThat(first).isTrue()
        assertThat(second).isTrue()

        val firstStats = dailySalesStatsRepository.findById(DailySalesStatsId(firstSalesDate, sellerId)).orElseThrow()
        assertThat(firstStats.salesCount).isEqualTo(1L)
        assertThat(firstStats.salesAmount).isEqualTo(100L)

        val secondStats = dailySalesStatsRepository.findById(DailySalesStatsId(secondSalesDate, sellerId)).orElseThrow()
        assertThat(secondStats.salesCount).isEqualTo(1L)
        assertThat(secondStats.salesAmount).isEqualTo(200L)
    }
}
