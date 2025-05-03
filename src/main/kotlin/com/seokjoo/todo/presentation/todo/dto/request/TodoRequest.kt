package com.seokjoo.todo.presentation.todo.dto.request

import com.seokjoo.todo.domain.service.todo.TodoCreateServiceRequestDTO
import com.seokjoo.todo.presentation.category.dto.CategoryDTO
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class TodoRequest(
    @field:NotBlank
    @Schema(description = "todo를 입력합니다.", example = "위플래시 재개봉 보러가기", required = true)
    val todo: String,
    val isDone: Boolean = false,
    @field:Min(value = 0L)
    @Schema(description = "todo의 가격을 입력합니다.", example = "0L", required = false)
    val price: Long = 0L,
    @Schema(description = "category를 list로 입력합니다", implementation = CategoryDTO::class)
    val categories: List<CategoryDTO> = listOf(),
)

fun TodoRequest.toCreateRequest() = TodoCreateServiceRequestDTO(
    todo = todo,
    isDone = isDone,
    categoryNames = categories.map { it.name },
    price = price,
)
