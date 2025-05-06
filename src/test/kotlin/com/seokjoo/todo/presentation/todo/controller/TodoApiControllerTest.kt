package com.seokjoo.todo.presentation.todo.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.todo.TodoPageServiceResponseDTO
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import com.seokjoo.todo.presentation.todo.dto.request.TodoRequest
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

@WebMvcTest(controllers = [TodoApiController::class])
class TodoApiControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    internal lateinit var todoService: TodoService

    @MockitoBean
    internal lateinit var authService: TodoAuthService

    @Test
    fun `getAllTodo 함수를 호출하면 200 이 나온다`() {
        val user = User("pita", "pita")
        val mockResponse = TodoServiceResponseDTO(
            id = 1L,
            todo = "create todo",
            isDone = false,
            categories = listOf(),
            owner = "pita",
            price = 0L,
        )
        given(authService.findUser(any())).willReturn(user)
        given(todoService.getPagedTodos(eq(user.userId), any())).willReturn(
            TodoPageServiceResponseDTO(
                isLast = true, responseList = listOf(
                    mockResponse
                )
            )
        )
        mockMvc.get("/api/v1/todos") { header("Authorization", "Bearer a") }
            .andDo { print() }
            .andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `getTodoById로 id가 숫자가 아닌 값이 들어올 때 bad reqeust`() {
        mockMvc.get("/api/v1/todos/hi")
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
        mockMvc.post("/api/v1/todos") {
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
    fun `create todo 시에 음수 값이 못들어가게 막히고 있는지`() {
        // 유효하지 않은 요청
        val request = TodoRequest(
            todo = "ab",
            price = -200L
        )
        mockMvc.post("/api/v1/todos") {
            content = objectMapper.writeValueAsString(request)
            contentType = MediaType.APPLICATION_JSON
        }
            .andDo { print() }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.message") { value("잔액은 음수일 수 없습니다.") } // 메시지 검증
            }
    }

    @Test
    fun `create todo 시에 예상한 것 처럼 응답을 잘 만들어 낸다`() {
        val request = TodoRequest(
            todo = "create todo",
        )
        val user = User("pita", "pita")
        val mockResponse = TodoServiceResponseDTO(
            id = 1L,
            todo = "create todo",
            isDone = false,
            categories = listOf(),
            owner = "pita",
            price = 0L,
        )
        given(authService.findUser(any())).willReturn(user)
        given(todoService.createTodo(any(), eq(user))).willReturn(mockResponse)

        mockMvc.post("/api/v1/todos") {
            content = objectMapper.writeValueAsString(request)
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer a") // 위에 given(authService.findUser(any())).willReturn(user) 를 통해 아무거나 넣어도됨
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
