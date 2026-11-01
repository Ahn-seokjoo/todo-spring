package com.seokjoo.todo.presentation.auth.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.auth.TodoAuthServiceLoginResponse
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
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(controllers = [TodoAuthApiController::class])
class TodoAuthApiControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    internal lateinit var authService: TodoAuthService

    private fun signupBody(userId: String) = objectMapper.writeValueAsString(
        mapOf("user_id" to userId, "password" to "pw", "email" to "abc@nav.com")
    )

    private fun loginBody(userId: String) = objectMapper.writeValueAsString(
        mapOf("user_id" to userId, "password" to "pw")
    )

    @ParameterizedTest
    @ValueSource(strings = ["pita", "pita_1-ok", "a23456789012345678901234567890"]) // 최소 4자, 최대 30자 경계 포함
    fun `영문 숫자 언더바 하이픈으로 된 4~30자 아이디는 가입된다`(userId: String) {
        mockMvc.post("/api/v1/auth/signup") {
            content = signupBody(userId)
            contentType = MediaType.APPLICATION_JSON
        }
            .andExpect { status { isOk() } }

        verify(authService).signUp(eq(userId), eq("pw"), eq("abc@nav.com"))
    }

    // 콜론·글롭 문자는 캐시 키와 SCAN 패턴에서 다른 사용자와 섞일 수 있어서 거절, 공백·한글·길이 범위 밖도 거절
    @ParameterizedTest
    @ValueSource(strings = ["amy:dev", "pi*a", "p?ta", "[p]ita", "pi\\ta", "a b c", "공부하기", "abc", "a234567890123456789012345678901"])
    fun `허용하지 않는 글자가 있거나 길이가 맞지 않는 아이디는 A008로 거절하고 가입하지 않는다`(userId: String) {
        mockMvc.post("/api/v1/auth/signup") {
            content = signupBody(userId)
            contentType = MediaType.APPLICATION_JSON
        }
            .andDo { print() }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value(TodoExceptionType.AUTH_VALIDATION_ERROR.errorCode) }
            }

        verify(authService, never()).signUp(any(), any(), any())
    }

    // 규칙이 생기기 전에 가입한 계정이 막히지 않도록 로그인은 형식을 검사하지 않는다
    @Test
    fun `로그인은 새 아이디 규칙에 맞지 않는 기존 아이디도 검증을 통과한다`() {
        given(authService.login(eq("ab"), eq("pw"))).willReturn(TodoAuthServiceLoginResponse("access", "refresh"))

        mockMvc.post("/api/v1/auth/login") {
            content = loginBody("ab")
            contentType = MediaType.APPLICATION_JSON
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.accessToken") { value("access") }
            }

        verify(authService).login(eq("ab"), eq("pw"))
    }

    @Test
    fun `로그인 아이디가 비어 있으면 400을 반환하고 로그인하지 않는다`() {
        mockMvc.post("/api/v1/auth/login") {
            content = loginBody(" ")
            contentType = MediaType.APPLICATION_JSON
        }
            .andExpect { status { isBadRequest() } }

        verify(authService, never()).login(any(), any())
    }
}
