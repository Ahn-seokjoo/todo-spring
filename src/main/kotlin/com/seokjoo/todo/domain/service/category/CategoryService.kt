package com.seokjoo.todo.domain.service.category

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.category.Category
import com.seokjoo.todo.domain.repository.category.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CategoryService(
    private val categoryRepository: CategoryRepository,
) {

    @Transactional(readOnly = true)
    fun getCategory(name: String): CategoryServiceResponseDTO {
        val category =
            categoryRepository.findCategoryByName(name) ?: throw TodoException.of(TodoExceptionType.CATEGORY_NOT_EXIST)
        return CategoryServiceResponseDTO.from(category = category)
    }

    @Transactional
    fun createCategory(request: CategoryServiceRequestDTO): CategoryServiceResponseDTO {
        val category = categoryRepository.save(Category(name = request.name))
        return CategoryServiceResponseDTO.from(category = category)
    }

    @Transactional
    fun removeCategory(categoryId: Long) {
        val count = categoryRepository.countByCategoryId(categoryId)
        if (count == 0L) {
            categoryRepository.deleteById(categoryId)
        }
    }
}
