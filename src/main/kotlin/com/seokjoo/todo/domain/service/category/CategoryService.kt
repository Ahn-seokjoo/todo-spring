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
    private val categorySaveHelper: CategorySaveHelper,
) {
    @Transactional(readOnly = true)
    fun getAllCategories(): List<CategoryServiceResponseDTO> {
        return categoryRepository.findAll().map { CategoryServiceResponseDTO.from(category = it) }
    }

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

    @Transactional
    fun getOrCreateCategory(name: String): Category {
        // 조회
        categoryRepository.findCategoryByName(name)?.let { return it }

        // 삽입 (REQUIRES_NEW로 분리)
        runCatching {
            categorySaveHelper.insert(name)
        }

        // 다시 조회해서 영속성 컨텍스트에 붙은 애 리턴
        return categoryRepository.findCategoryByName(name)!!
    }
}
