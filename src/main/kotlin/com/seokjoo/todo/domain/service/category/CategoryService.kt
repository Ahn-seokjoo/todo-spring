package com.seokjoo.todo.domain.service.category

import com.seokjoo.todo.domain.repository.category.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CategoryService(
    private val categoryRepository: CategoryRepository,
) {

    fun removeCategory(categoryId: Long) {
        val count = categoryRepository.countByCategoryId(categoryId)
        if (count == 0L) {
            categoryRepository.deleteById(categoryId)
        }
    }
}
