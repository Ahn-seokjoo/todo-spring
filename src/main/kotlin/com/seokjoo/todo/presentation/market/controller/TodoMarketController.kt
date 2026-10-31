package com.seokjoo.todo.presentation.market.controller

import com.seokjoo.todo.domain.service.market.TodoMarketService
import com.seokjoo.todo.presentation.market.dto.TodoMarketPageRequest
import com.seokjoo.todo.presentation.market.dto.TodoMarketPageResponse
import com.seokjoo.todo.presentation.market.dto.toTodoMarketPageServiceDTO
import com.seokjoo.todo.presentation.todo.dto.response.toResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/market")
class TodoMarketController(
    private val marketService: TodoMarketService,
) {

    @GetMapping
    fun getMarketTodos(
        @RequestParam(defaultValue = "20") pageSize: Int,
        @RequestParam(required = false) cursor: Long?,
    ): ResponseEntity<TodoMarketPageResponse> {
        val todoPageRequest = TodoMarketPageRequest(pageSize = pageSize, cursor = cursor ?: Long.MAX_VALUE)
        val pagedDto = todoPageRequest.toTodoMarketPageServiceDTO()
        val pagedResponse = marketService.getPagedMarketTodos(pageServiceDTO = pagedDto)

        return ResponseEntity.ok(
            TodoMarketPageResponse(
                nextCursor = pagedResponse.nextCursor,
                todoList = pagedResponse.responseList.map { it.toResponse() }
            )
        )
    }
}
