package com.seokjoo.todo.domain.repository.purchase

import com.seokjoo.todo.domain.entity.purchase.Purchase
import com.seokjoo.todo.domain.entity.purchase.PurchaseStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TodoPurchaseRepository : JpaRepository<Purchase, Long> {
    fun findByTodoIdAndPurchaseStatus(todoId: Long, purchaseStatus: PurchaseStatus): Purchase?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Purchase p set p.purchaseStatus = :purchaseStatus where p.todoId = :todoId and p.purchaseStatus = 'PENDING'")
    fun updatePurchaseStatus(todoId: Long, purchaseStatus: PurchaseStatus): Long
}
