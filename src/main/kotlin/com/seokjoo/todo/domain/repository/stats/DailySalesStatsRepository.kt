package com.seokjoo.todo.domain.repository.stats

import com.seokjoo.todo.domain.entity.purchase.DailySalesRow
import com.seokjoo.todo.domain.entity.stats.DailySalesStats
import com.seokjoo.todo.domain.entity.stats.DailySalesStatsId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface DailySalesStatsRepository : JpaRepository<DailySalesStats, DailySalesStatsId> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        nativeQuery = true,
        value = """
            INSERT INTO daily_sales_stats (sales_date, seller_id, sales_amount, sales_count, created_at, updated_at)
            VALUES (:salesDate, :sellerId, :amount, 1, NOW(6), NOW(6))
            ON DUPLICATE KEY UPDATE sales_amount = sales_amount + :amount, sales_count = sales_count + 1, updated_at = NOW(6)
        """
    )
    fun upsertDailySalesStats(amount: Long, sellerId: String, salesDate: LocalDate): Int

    @Query(
        nativeQuery = true,
        value = """
            SELECT sales_date AS salesDate, sales_count as salesCount, sales_amount AS salesAmount
            FROM daily_sales_stats
            WHERE seller_id = :sellerId
            AND sales_date >= :from
            AND sales_date < :to
            ORDER BY sales_date
        """
    )
    fun findDailySalesStats(
        sellerId: String,
        from: LocalDate,
        to: LocalDate,
    ): List<DailySalesRow>
}
