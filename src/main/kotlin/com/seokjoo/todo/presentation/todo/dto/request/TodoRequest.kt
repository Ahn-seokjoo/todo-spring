package com.seokjoo.todo.presentation.todo.dto.request

import com.seokjoo.todo.presentation.category.dto.CategoryDTO
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class TodoRequest(
    @field:NotBlank(message = "todo는 필수입니다")
    @Schema(description = "todo를 입력합니다.", example = "위플래시 재개봉 보러가기", required = true)
    val todo: String,
    val isDone: Boolean = false,
    @Schema(description = "category를 list로 입력합니다", implementation = CategoryDTO::class)
    val categories: List<CategoryDTO> = listOf(),
)
