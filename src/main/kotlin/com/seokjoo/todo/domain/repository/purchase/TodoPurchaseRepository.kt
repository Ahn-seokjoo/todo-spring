package com.seokjoo.todo.domain.repository.purchase

import com.seokjoo.todo.domain.entity.purchase.Purchase
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TodoPurchaseRepository : JpaRepository<Purchase, Long>
