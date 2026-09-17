package com.seokjoo.todo.presentation.charge.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.balance.TodoBalanceService
import com.seokjoo.todo.domain.service.charge.TodoChargeService
import com.seokjoo.todo.presentation.charge.dto.TodoChargeRequest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@WebMvcTest(controllers = [TodoChargeApiController::class])
class TodoChargeApiControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    internal lateinit var chargeService: TodoChargeService

    @MockitoBean
    internal lateinit var authService: TodoAuthService

    @MockitoBean
    internal lateinit var balanceService: TodoBalanceService

    @Test
    fun `본인 계좌를 충전하면 200이 나온다`() {
        val request = TodoChargeRequest(amount = 1000L)
        val user = User(userId = "pita", password = "pita")

        given(authService.getSubject(any())).willReturn("pita")
        given(chargeService.charge(amount = eq(1000L), userId = eq("pita"))).willReturn(user)

        mockMvc.post("/api/v1/charge/pita") {
            content = objectMapper.writeValueAsString(request)
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer a")
        }
            .andDo { print() }
            .andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `타인의 id로 충전을 시도하면 403과 함께 차단된다`() {
        val request = TodoChargeRequest(amount = 1000L)

        given(authService.getSubject(any())).willReturn("pita")

        mockMvc.post("/api/v1/charge/other-user") {
            content = objectMapper.writeValueAsString(request)
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer a")
        }
            .andDo { print() }
            .andExpect {
                status { isForbidden() }
                jsonPath("$.errorCode") { value(TodoExceptionType.AUTH_FORBIDDEN_USER_ACCESS.errorCode) }
                jsonPath("$.message") { value(TodoExceptionType.AUTH_FORBIDDEN_USER_ACCESS.message) }
            }
    }

    @Test
    fun `본인 잔액을 조회하면 200이 나온다`() {
        val user = User(userId = "pita", password = "pita")

        given(authService.getSubject(any())).willReturn("pita")
        given(authService.findUserByUserId(eq("pita"))).willReturn(user)

        mockMvc.get("/api/v1/current/pita") { header("Authorization", "Bearer a") }
            .andDo { print() }
            .andExpect {
                status { isOk() }
                jsonPath("$.balance") { value(0L) }
            }
    }

    @Test
    fun `타인의 잔액을 조회하려고 하면 403과 함께 차단된다`() {
        given(authService.getSubject(any())).willReturn("pita")

        mockMvc.get("/api/v1/current/other-user") { header("Authorization", "Bearer a") }
            .andDo { print() }
            .andExpect {
                status { isForbidden() }
                jsonPath("$.errorCode") { value(TodoExceptionType.AUTH_FORBIDDEN_USER_ACCESS.errorCode) }
                jsonPath("$.message") { value(TodoExceptionType.AUTH_FORBIDDEN_USER_ACCESS.message) }
            }
    }
}
