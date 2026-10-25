package com.seokjoo.todo.presentation.sales.controller

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.sales.TodoSalesStatsService
import com.seokjoo.todo.presentation.sales.dto.DailySalesResponse
import org.junit.jupiter.api.Test
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
import java.time.LocalDate

@WebMvcTest(controllers = [TodoSalesStatsController::class])
class TodoSalesStatsControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    internal lateinit var authService: TodoAuthService

    @MockitoBean
    internal lateinit var salesStatsService: TodoSalesStatsService

    private val from = LocalDate.of(2026, 10, 19)
    private val to = LocalDate.of(2026, 10, 21)

    @Test
    fun `토큰의 subject를 판매자로 조회하고 200과 일별 통계를 반환한다`() {
        given(authService.getSubject(eq("a"))).willReturn("pita")
        given(salesStatsService.getDailySalesStats(eq("pita"), eq(from), eq(to))).willReturn(
            listOf(DailySalesResponse(date = from, count = 2L, amount = 3_000L))
        )

        mockMvc.get("/api/v1/sales/daily-stats") {
            header("Authorization", "Bearer a")
            param("from", "2026-10-19")
            param("to", "2026-10-21")
        }
            .andDo { print() }
            .andExpect {
                status { isOk() }
                jsonPath("$[0].date") { value("2026-10-19") }
                jsonPath("$[0].count") { value(2) }
                jsonPath("$[0].amount") { value(3000) }
            }
    }

    @Test
    fun `요청에 다른 판매자 id를 넣어도 무시하고 토큰의 subject로 조회한다`() {
        given(authService.getSubject(any())).willReturn("pita")
        given(salesStatsService.getDailySalesStats(any(), any(), any())).willReturn(emptyList())

        mockMvc.get("/api/v1/sales/daily-stats") {
            header("Authorization", "Bearer a")
            param("from", "2026-10-19")
            param("to", "2026-10-21")
            param("sellerId", "other-seller")
        }
            .andExpect { status { isOk() } }

        verify(salesStatsService).getDailySalesStats(eq("pita"), eq(from), eq(to))
        verify(salesStatsService, never()).getDailySalesStats(eq("other-seller"), any(), any())
    }

    @Test
    fun `서비스가 날짜 구간 오류를 던지면 400과 SALES_INVALID_DATE_RANGE를 반환한다`() {
        given(authService.getSubject(any())).willReturn("pita")
        given(salesStatsService.getDailySalesStats(any(), any(), any()))
            .willThrow(TodoException.of(TodoExceptionType.SALES_INVALID_DATE_RANGE))

        mockMvc.get("/api/v1/sales/daily-stats") {
            header("Authorization", "Bearer a")
            param("from", "2026-10-21")
            param("to", "2026-10-19")
        }
            .andDo { print() }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value(TodoExceptionType.SALES_INVALID_DATE_RANGE.errorCode) }
                jsonPath("$.message") { value(TodoExceptionType.SALES_INVALID_DATE_RANGE.message) }
            }
    }

    @Test
    fun `날짜 형식이 잘못되면 400을 반환하고 서비스를 호출하지 않는다`() {
        mockMvc.get("/api/v1/sales/daily-stats") {
            header("Authorization", "Bearer a")
            param("from", "2026/10/19")
            param("to", "2026-10-21")
        }
            .andDo { print() }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value(TodoExceptionType.COMMON_BAD_REQUEST.errorCode) }
            }

        verify(salesStatsService, never()).getDailySalesStats(any(), any(), any())
    }
}
