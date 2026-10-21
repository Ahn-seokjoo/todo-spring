package com.seokjoo.todo.domain.entity.stats

import java.io.Serializable
import java.time.LocalDate

data class DailySalesStatsId(
    val salesDate: LocalDate = LocalDate.MIN,
    val sellerId: String = "",
) : Serializable
