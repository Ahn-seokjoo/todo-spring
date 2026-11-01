package com.seokjoo.todo.presentation.market.controller

import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.service.market.TodoMarketPageServiceDTO
import com.seokjoo.todo.domain.service.market.TodoMarketPageServiceResponseDTO
import com.seokjoo.todo.domain.service.market.TodoMarketService
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(controllers = [TodoMarketController::class])
class TodoMarketControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    internal lateinit var marketService: TodoMarketService

    @Test
    fun `파라미터 없이 요청하면 pageSize 20, 첫 페이지 커서(Long MAX)로 조회하고 nextCursor를 내려준다`() {
        given(marketService.getPagedMarketTodos(any())).willReturn(TodoMarketPageServiceResponseDTO(nextCursor = 16L))

        mockMvc.get("/api/v1/market")
            .andExpect {
                status { isOk() }
                jsonPath("$.nextCursor") { value(16) }
            }

        verify(marketService).getPagedMarketTodos(eq(TodoMarketPageServiceDTO(pageSize = 20, cursor = Long.MAX_VALUE)))
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1, 21])
    fun `pageSize가 1~20 밖이면 TO04로 거절하고 서비스를 호출하지 않는다`(pageSize: Int) {
        mockMvc.get("/api/v1/market") { param("pageSize", pageSize.toString()) }
            .andDo { print() }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value(TodoExceptionType.INVALID_PAGE_SIZE.errorCode) }
            }

        verify(marketService, never()).getPagedMarketTodos(any())
    }
}
