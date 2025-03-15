package com.seokjoo.todo.domain.service.category

import com.seokjoo.todo.domain.entity.category.Category

data class CategoryServiceResponseDTO(
    val id: Long,
    val category: String,
) {
    companion object {
        fun from(category: Category) = CategoryServiceResponseDTO(
            id = category.id ?: 0L, // TODO 이건 앞의 pr들이 머지되면 non-null로 수정 예정
            category = category.name
        )
    }
}
