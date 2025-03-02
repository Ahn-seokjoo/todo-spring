package com.seokjoo.todo.presentation.category.service

import com.seokjoo.todo.domain.repository.CategoryRepository
import org.springframework.stereotype.Service

@Service
class CategoryService(
    private val repository: CategoryRepository,
) {
}
