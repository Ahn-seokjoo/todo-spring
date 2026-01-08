package com.seokjoo.todo.presentation.todo.dto.request

import com.seokjoo.todo.domain.service.todo.TodoUpdateServiceRequestDTO
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min

@Schema(description = "todo를 update할 때 사용합니다. 일반 TodoRequest랑은 다른점은 todo가 required 하지 않습니다.")
data class TodoPatchRequest(
    @Schema(description = "todo를 입력합니다.", example = "위플래시 재개봉 보러가기", required = false)
    val todo: String = "",
    @Schema(description = "todo 완료 정보를 알려줍니다.", example = "위플래시 재개봉 보러가기", required = false)
    val isDone: Boolean?,
    @field:Min(value = 0L)
    @Schema(description = "todo의 가격을 입력합니다.", example = "0L", required = false)
    val price: Long?,
    @Schema(description = "category를 list로 입력합니다")
    val categories: List<String> = listOf(),
)

fun TodoPatchRequest.toUpdateRequest() = TodoUpdateServiceRequestDTO(
    todo = todo,
    isDone = isDone,
    categoryNames = categories.map { name -> name },
    price = price,
)
