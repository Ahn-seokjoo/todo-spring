package com.seokjoo.todo.presentation.category.dto

import com.seokjoo.todo.domain.service.category.CategoryServiceRequestDTO
import jakarta.validation.constraints.NotBlank

data class CategoryRequest(
    @field:NotBlank
    val name: String,
)

fun CategoryRequest.toCategoryServiceRequest() = CategoryServiceRequestDTO(
    name = name
)
