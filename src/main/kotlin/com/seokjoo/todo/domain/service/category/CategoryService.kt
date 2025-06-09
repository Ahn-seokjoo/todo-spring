package com.seokjoo.todo.domain.service.category

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.common.redisson.RedisLockManager
import com.seokjoo.todo.domain.entity.category.Category
import com.seokjoo.todo.domain.repository.category.CategoryRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val redisLockManager: RedisLockManager,
) {
    @Transactional(readOnly = true)
    fun getAllCategories(): List<CategoryServiceResponseDTO> {
        return categoryRepository.findAll().map { CategoryServiceResponseDTO.from(category = it) }
    }

    @Transactional(readOnly = true)
    fun getCategoryServiceResponse(name: String): CategoryServiceResponseDTO {
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

    @Transactional(readOnly = true)
    fun findById(id: Long): Category {
        return categoryRepository.findByIdOrNull(id) ?: throw TodoException.of(TodoExceptionType.CATEGORY_NOT_EXIST)
    }

    @Transactional
    fun getOrCreateCategory(name: String): Category {
        // 조회
        return kotlin.runCatching {
            redisLockManager.tryLock(name) {
                val category =
                    categoryRepository.findCategoryByName(name = name) ?: categoryRepository.save(Category(name = name))
                category
            }
        }.getOrNull() ?: throw TodoException.of(TodoExceptionType.LOCK_GET_FAILED)
    }
}
