package com.seokjoo.todo.domain.entity.stats

import com.seokjoo.todo.domain.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.time.LocalDate

// 판매자별 일 매출 집계
@Entity
@Table(name = "daily_sales_stats")
@IdClass(DailySalesStatsId::class)
class DailySalesStats(
    @Id
    @Column(name = "sales_date", nullable = false)
    val salesDate: LocalDate,

    @Id
    @Column(name = "seller_id", nullable = false)
    val sellerId: String,

    @Column(name = "sales_amount", nullable = false)
    val salesAmount: Long,

    @Column(name = "sales_count", nullable = false)
    val salesCount: Long,
) : BaseEntity()
