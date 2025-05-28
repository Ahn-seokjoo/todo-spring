package com.seokjoo.todo.presentation.trade.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min

data class TodoBuyRequest(
    @field:Min(0L)
    @Schema(description = "사려고 하는 todo id를 입력합니다")
    @JsonProperty("todo_id")
    val todoId: Long,
)
