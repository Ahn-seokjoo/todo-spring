package com.seokjoo.todo.presentation.category.dto

import com.seokjoo.todo.domain.entity.category.Category
import io.swagger.v3.oas.annotations.media.Schema

data class CategoryDTO(
    @Schema(description = "카테고리에 추가할 name을 입력합니다.")
    val name: String,
)

fun CategoryDTO.toEntity() = Category(name = name)

