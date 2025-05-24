package com.seokjoo.todo.presentation.charge.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min

data class TodoChargeRequest(
    @field:Min(0L)
    @Schema(description = "충전할 금액을 입력합니다.")
    val amount: Long,
)
