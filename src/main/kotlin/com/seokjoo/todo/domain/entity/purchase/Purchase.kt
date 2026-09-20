package com.seokjoo.todo.domain.entity.purchase

import com.seokjoo.todo.domain.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "todo_purchase")
class Purchase(
    @Column(name = "seller_id", nullable = false)
    val sellerId: String,

    @Column(name = "buyer_id", nullable = false)
    val buyerId: String,

    @Column(name = "todo_id", nullable = false, unique = true)
    val todoId: Long,

    @Column(name = "price", nullable = false)
    val price: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "purchase_status", nullable = false)
    var purchaseStatus: PurchaseStatus = PurchaseStatus.PENDING,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
}
