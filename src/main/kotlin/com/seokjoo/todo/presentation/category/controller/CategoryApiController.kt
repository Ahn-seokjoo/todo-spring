package com.seokjoo.todo.presentation.category.controller

import com.seokjoo.todo.domain.service.category.CategoryService
import com.seokjoo.todo.presentation.category.dto.CategoryRequest
import com.seokjoo.todo.presentation.category.dto.CategoryResponse
import com.seokjoo.todo.presentation.category.dto.toCategoryServiceRequest
import com.seokjoo.todo.presentation.category.dto.toResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@Tag(name = "Category", description = "Category 조회, 삭제, 수정 API")
@RestController
@RequestMapping("/api/v1")
class CategoryApiController(
    private val categoryService: CategoryService,
) {
    @GetMapping("/category")
    @Operation(summary = "카테고리 존재 여부 확인", description = "name을 이용해 카테고리가 이미 존재하는지 확인")
    fun getCategory(@Parameter name: String): ResponseEntity<CategoryResponse> {
        val category = categoryService.getCategory(name).toResponse()
        return ResponseEntity.ok(category)
    }

    @PostMapping("category")
    @Operation(summary = "카테고리 추가", description = "카테고리만 추가합니다")
    fun createCategory(@RequestBody @Validated request: CategoryRequest): ResponseEntity<CategoryResponse> {
        val category = categoryService.createCategory(request.toCategoryServiceRequest()).toResponse()
        return ResponseEntity.created(URI.create("category")).body(category)
    }
}
