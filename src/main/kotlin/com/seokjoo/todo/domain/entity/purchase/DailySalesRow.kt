package com.seokjoo.todo.domain.entity.purchase

import java.time.LocalDate

interface DailySalesRow {
    val salesDate: LocalDate
    val salesCount: Long
    val salesAmount: Long
}
