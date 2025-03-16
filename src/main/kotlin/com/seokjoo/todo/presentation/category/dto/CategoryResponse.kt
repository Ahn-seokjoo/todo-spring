package com.seokjoo.todo.presentation.category.dto

import com.seokjoo.todo.domain.service.category.CategoryServiceResponseDTO

data class CategoryResponse(
    val id: Long,
    val name: String,
)

fun CategoryServiceResponseDTO.toResponse() = CategoryResponse(
    id = id,
    name = category,
)
