package com.seokjoo.todo.domain.service.category

import com.seokjoo.todo.domain.entity.category.Category

data class CategoryServiceResponseDTO(
    val id: Long,
    val category: String,
) {
    companion object {
        fun from(category: Category) = CategoryServiceResponseDTO(
            id = category.id,
            category = category.name
        )
    }
}
