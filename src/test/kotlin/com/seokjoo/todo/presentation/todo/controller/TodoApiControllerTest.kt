package com.seokjoo.todo.presentation.todo.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import com.seokjoo.todo.presentation.todo.dto.request.TodoRequest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@WebMvcTest(controllers = [TodoApiController::class])
class TodoApiControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    internal lateinit var service: TodoService

    @Test
    fun `getAllTodo 함수를 호출하면 200 이 나온다`() {
        mockMvc.get("/api/v1/todo")
            .andDo { print() }
            .andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `getTodoById로 id가 숫자가 아닌 값이 들어올 때 bad reqeust`() {
        mockMvc.get("/api/v1/todo/hi")
            .andDo { print() }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value(TodoExceptionType.ID_BAD_REQUEST.errorCode) }
                jsonPath("$.message") { value(TodoExceptionType.ID_BAD_REQUEST.message) }
            }
    }

    @Test
    fun `create todo 시에 validation이 제대로 되고 있는지`() {
        // 유효하지 않은 요청 (todo 비어 있음)
        val request = TodoRequest(
            todo = "",
        )
        mockMvc.post("/api/v1/todo") {
            content = objectMapper.writeValueAsString(request)
            contentType = MediaType.APPLICATION_JSON
        }
            .andDo { print() }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.message") { value("Todo는 빈 값일 수 없습니다") } // 메시지 검증
            }
    }

    @Test
    fun `create todo 시에 예상한 것 처럼 응답을 잘 만들어 낸다`() {
        val request = TodoRequest(
            todo = "create todo",
        )
        val mockResponse = TodoServiceResponseDTO(
            id = 1L,
            todo = "create todo",
            isDone = false,
            categories = listOf()
        )
        given(service.createTodo(any())).willReturn(mockResponse)

        mockMvc.post("/api/v1/todo") {
            content = objectMapper.writeValueAsString(request)
            contentType = MediaType.APPLICATION_JSON
        }
            .andDo { print() }
            .andExpect {
                status { isCreated() }
                content {
                    json(
                        """
                        {
                            "id": 1,
                            "todo": "create todo",
                            "isDone": false
                        }
                        """.trimIndent()
                    )
                }
            }
    }
}
