package com.seokjoo.todo.presentation.category.controller

import com.seokjoo.todo.presentation.category.service.CategoryService
import org.springframework.web.bind.annotation.RestController

@RestController
class CategoryApiController(
    private val categoryService: CategoryService,
)
