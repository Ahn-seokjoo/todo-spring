package com.seokjoo.todo.presentation.charge.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class TodoChargeResponse(
    @field:JsonProperty("user_id")
    val userId: String,
    val amount: Long,
)
