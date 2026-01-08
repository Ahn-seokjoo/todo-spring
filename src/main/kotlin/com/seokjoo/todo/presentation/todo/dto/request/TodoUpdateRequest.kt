package com.seokjoo.todo.presentation.todo.dto.request

import com.seokjoo.todo.domain.service.todo.TodoUpdateServiceRequestDTO
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

@Schema(description = "todo를 덮어 쓸 때 사용합니다. 모든 필드가 required 입니다.")
data class TodoUpdateRequest(
    @param:Schema(description = "todo를 입력합니다.", example = "위플래시 재개봉 보러가기", required = true)
    @field:NotBlank
    val todo: String,
    @param:Schema(description = "todo 완료 정보를 알려줍니다.", example = "위플래시 재개봉 보러가기", required = true)
    @field:NotNull
    val isDone: Boolean?,
    @field:Min(value = 0L)
    @param:Schema(description = "todo의 가격을 입력합니다.", example = "0L", required = true)
    @field:NotNull
    val price: Long?,
    @param:Schema(description = "category를 list로 입력합니다", required = true)
    @param:NotEmpty
    val categories: List<String>,
)

fun TodoUpdateRequest.toUpdateRequest() = TodoUpdateServiceRequestDTO(
    todo = todo,
    isDone = isDone,
    categoryNames = categories.map { name -> name },
    price = price,
)
