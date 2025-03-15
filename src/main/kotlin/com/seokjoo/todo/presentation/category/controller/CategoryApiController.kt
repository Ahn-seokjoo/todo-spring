package com.seokjoo.todo.presentation.category.controller

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.service.category.CategoryResponseDTO
import com.seokjoo.todo.domain.service.category.CategoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class CategoryApiController(
    private val categoryService: CategoryService,
) {
    @GetMapping("/category")
    @Operation(summary = "카테고리 존재 여부 확인", description = "name을 이용해 카테고리가 이미 존재하는지 확인")
    fun getCategory(@Parameter name: String): ResponseEntity<CategoryResponseDTO> {
        val category = categoryService.getCategory(name) ?: throw TodoException.of(TodoExceptionType.CATEGORY_NOT_EXIST)
        val response = CategoryResponseDTO.from(category = category)
        return ResponseEntity.ok(response)
    }
}
