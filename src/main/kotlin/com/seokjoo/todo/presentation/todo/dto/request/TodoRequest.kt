package com.seokjoo.todo.presentation.todo.dto.request

import com.seokjoo.todo.domain.service.todo.TodoCreateServiceRequestDTO
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class TodoRequest(
    @field:NotBlank(message = "Todo는 빈 값일 수 없습니다")
    @Schema(description = "todo를 입력합니다.", example = "위플래시 재개봉 보러가기", required = true)
    val todo: String,
    val isDone: Boolean = false,
    @field:Min(value = 0L, message = "잔액은 음수일 수 없습니다.")
    @Schema(description = "todo의 가격을 입력합니다.", example = "0L", required = false)
    val price: Long = 0L,
    @Schema(description = "category를 list로 입력합니다")
    val categories: List<String> = listOf(),
)

fun TodoRequest.toCreateRequest() = TodoCreateServiceRequestDTO(
    todo = todo,
    isDone = isDone,
    categoryNames = categories.map { name -> name },
    price = price,
)
