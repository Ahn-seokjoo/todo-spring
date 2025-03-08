package com.seokjoo.todo.presentation.category.controller

import com.seokjoo.todo.domain.service.category.CategoryService
import org.springframework.web.bind.annotation.RestController

@RestController
class CategoryApiController(
    private val categoryService: CategoryService,
)
