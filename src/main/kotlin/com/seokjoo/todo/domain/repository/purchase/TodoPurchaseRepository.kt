package com.seokjoo.todo.domain.repository.purchase

import com.seokjoo.todo.domain.entity.purchase.DailySalesRow
import com.seokjoo.todo.domain.entity.purchase.Purchase
import com.seokjoo.todo.domain.entity.purchase.PurchaseStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface TodoPurchaseRepository : JpaRepository<Purchase, Long> {
    fun findByTodoIdAndPurchaseStatus(todoId: Long, purchaseStatus: PurchaseStatus): Purchase?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Purchase p set p.purchaseStatus = :purchaseStatus where p.todoId = :todoId and p.purchaseStatus = 'PENDING'")
    fun updatePurchaseStatus(todoId: Long, purchaseStatus: PurchaseStatus): Int

    @Query(
        value = """
            SELECT DATE(created_at) AS salesDate, COUNT(*) as salesCount, SUM(price) AS salesAmount
            FROM todo_purchase
            WHERE seller_id = :sellerId
            AND purchase_status = 'APPROVED'
            AND created_at >= :from AND created_at < :to
            GROUP BY salesDate
            ORDER BY salesDate
        """,
        nativeQuery = true,
    )
    fun findDailySales(
        sellerId: String,
        from: LocalDateTime,
        to: LocalDateTime,
    ): List<DailySalesRow>
}
