package com.seokjoo.todo.presentation.sales.dto

import java.time.LocalDate

data class DailySalesResponse(val date: LocalDate, val count: Long, val amount: Long)
