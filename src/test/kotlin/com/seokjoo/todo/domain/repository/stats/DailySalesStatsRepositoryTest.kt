package com.seokjoo.todo.domain.repository.stats

import com.seokjoo.todo.annotation.TodoTest
import com.seokjoo.todo.domain.entity.stats.DailySalesStats
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@TodoTest
@Transactional
class DailySalesStatsRepositoryTest @Autowired constructor(
    private val dailySalesStatsRepository: DailySalesStatsRepository,
) {
    private val from = LocalDate.of(2026, 10, 19)
    private val to = LocalDate.of(2026, 10, 22)

    @Test
    fun `해당 판매자의 from 이상 to 미만 행만 날짜 오름차순으로 조회된다`() {
        val sellerId = newSellerId()
        val otherSellerId = newSellerId()
        // ORDER BY를 검증하려고 날짜를 일부러 섞어서 저장한다
        dailySalesStatsRepository.saveAllAndFlush(
            listOf(
                stats(sellerId, LocalDate.of(2026, 10, 21), amount = 3_000L, count = 3L),
                stats(sellerId, LocalDate.of(2026, 10, 18), amount = 9_000L, count = 9L), // from 전날: 제외
                stats(sellerId, LocalDate.of(2026, 10, 19), amount = 1_000L, count = 1L), // from 당일: 포함
                stats(sellerId, LocalDate.of(2026, 10, 22), amount = 9_000L, count = 9L), // to 당일: 제외
                stats(sellerId, LocalDate.of(2026, 10, 20), amount = 2_000L, count = 2L),
                stats(otherSellerId, LocalDate.of(2026, 10, 20), amount = 9_000L, count = 9L), // 다른 판매자: 제외
            )
        )

        val rows = dailySalesStatsRepository.findDailySalesStats(sellerId, from, to)

        assertThat(rows.map { Triple(it.salesDate, it.salesCount, it.salesAmount) })
            .containsExactly(
                Triple(LocalDate.of(2026, 10, 19), 1L, 1_000L),
                Triple(LocalDate.of(2026, 10, 20), 2L, 2_000L),
                Triple(LocalDate.of(2026, 10, 21), 3L, 3_000L),
            )
    }

    @Test
    fun `구간 안에 해당 판매자의 집계가 없으면 빈 목록을 반환한다`() {
        val sellerId = newSellerId()
        val otherSellerId = newSellerId()
        dailySalesStatsRepository.saveAllAndFlush(
            listOf(
                stats(sellerId, LocalDate.of(2026, 10, 18), amount = 1_000L, count = 1L), // 구간 전
                stats(sellerId, LocalDate.of(2026, 10, 22), amount = 1_000L, count = 1L), // 구간 끝(미포함)
                stats(otherSellerId, LocalDate.of(2026, 10, 20), amount = 1_000L, count = 1L), // 다른 판매자
            )
        )

        val rows = dailySalesStatsRepository.findDailySalesStats(sellerId, from, to)

        assertThat(rows).isEmpty()
    }

    private fun newSellerId() = "seller-${UUID.randomUUID().toString().take(8)}"

    private fun stats(sellerId: String, salesDate: LocalDate, amount: Long, count: Long) =
        DailySalesStats(
            salesDate = salesDate,
            sellerId = sellerId,
            salesAmount = amount,
            salesCount = count,
        )
}
