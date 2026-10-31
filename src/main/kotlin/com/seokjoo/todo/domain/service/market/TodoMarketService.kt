package com.seokjoo.todo.domain.service.market

import com.seokjoo.todo.domain.repository.market.TodoMarketRepository
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TodoMarketService(
    private val marketRepository: TodoMarketRepository,
) {
    @Transactional(readOnly = true)
    fun getPagedMarketTodos(pageServiceDTO: TodoMarketPageServiceDTO): TodoMarketPageServiceResponseDTO {
        val pageRequest = PageRequest.of(0, pageServiceDTO.pageSize.coerceIn(1, 20))
        val marketPage = marketRepository.findMarketTodos(
            pageable = pageRequest,
            cursor = pageServiceDTO.cursor,
        )
        if (marketPage.content.isEmpty()) return TodoMarketPageServiceResponseDTO()

        val pageResult = marketRepository.getFetchJoinedMarketTodoList(todos = marketPage.content)
        val responseList = pageResult
            .map { todo -> TodoServiceResponseDTO.from(todo) }
            .sortedByDescending { it.id }

        val nextCursor = if (marketPage.hasNext()) marketPage.content.last().id else null
        return TodoMarketPageServiceResponseDTO(nextCursor = nextCursor, responseList = responseList)
    }
}
