package com.seokjoo.todo.domain.service.sales

import com.seokjoo.todo.domain.repository.stats.DailySalesStatsRepository
import com.seokjoo.todo.domain.repository.stats.StatsProcessedEventRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class TodoSalesStatsRecordService(
    private val dailySalesStatsRepository: DailySalesStatsRepository,
    private val statsProcessedEventRepository: StatsProcessedEventRepository,
) {

    @Transactional
    fun record(eventId: String, amount: Long, sellerId: String, salesDate: LocalDate): Boolean {
        val insertResult = statsProcessedEventRepository.insertStatsProcessedEvent(eventId = eventId)
        if (insertResult == 0) return false
        dailySalesStatsRepository.upsertDailySalesStats(
            amount = amount,
            sellerId = sellerId,
            salesDate = salesDate,
        )
        return true
    }
}
