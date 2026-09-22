package com.seokjoo.todo.presentation.todo.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.todo.TodoPageServiceResponseDTO
import com.seokjoo.todo.domain.service.todo.TodoRetryableService
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import com.seokjoo.todo.presentation.todo.dto.request.TodoPatchRequest
import com.seokjoo.todo.presentation.todo.dto.request.TodoRequest
import com.seokjoo.todo.presentation.todo.dto.request.TodoUpdateRequest
import com.seokjoo.todo.presentation.todo.dto.request.toPatchRequest
import com.seokjoo.todo.presentation.todo.dto.request.toUpdateRequest
import org.junit.jupiter.api.Test
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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

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

    @MockitoBean
    internal lateinit var todoRetryableService: TodoRetryableService

    @Test
    fun `getAllTodo 함수를 호출하면 200 이 나온다`() {
        val user = User("pita", "pita")
        val mockResponse = TodoServiceResponseDTO(
            id = 1L,
            todo = "create todo",
            isDone = false,
            categories = listOf(),
            ownerId = "pita",
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
            ownerId = "pita",
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

    @Test
    fun `patchTodo 호출시 TodoRetryableService updateTodo 에 route id, 인증된 사용자 id, 변환된 DTO 를 위임한다`() {
        val id = 1L
        val user = User("pita", "pita")
        val request = TodoPatchRequest(
            todo = "update todo",
            isDone = true,
            price = 200L,
            categories = listOf("category1"),
        )
        val mockResponse = TodoServiceResponseDTO(
            id = id,
            todo = "update todo",
            isDone = true,
            categories = listOf(),
            ownerId = user.userId,
            price = 200L,
        )
        given(authService.findUser(any())).willReturn(user)
        given(
            todoRetryableService.updateTodo(eq(id), eq(user.userId), eq(request.toPatchRequest()))
        ).willReturn(mockResponse)

        mockMvc.patch("/api/v1/todos/$id") {
            content = objectMapper.writeValueAsString(request)
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer a")
        }
            .andDo { print() }
            .andExpect {
                status { isOk() }
            }

        verify(todoRetryableService).updateTodo(eq(id), eq(user.userId), eq(request.toPatchRequest()))
    }

    @Test
    fun `patchTodo에 id가 숫자가 아닌 값이 들어오면 bad request 이고 TodoRetryableService 는 호출되지 않는다`() {
        val request = TodoPatchRequest(
            todo = "update todo",
            isDone = true,
            price = 200L,
            categories = listOf("category1"),
        )
        mockMvc.patch("/api/v1/todos/hi") {
            content = objectMapper.writeValueAsString(request)
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer a")
        }
            .andDo { print() }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value(TodoExceptionType.ID_BAD_REQUEST.errorCode) }
            }

        verify(todoRetryableService, never()).updateTodo(any(), any(), any())
    }

    @Test
    fun `updateTodo(PUT) 호출시 TodoRetryableService updateTodo 에 route id, 인증된 사용자 id, 변환된 DTO 를 위임한다`() {
        val id = 1L
        val user = User("pita", "pita")
        val request = TodoUpdateRequest(
            todo = "overwrite todo",
            isDone = false,
            price = 500L,
            categories = listOf("category1", "category2"),
        )
        val mockResponse = TodoServiceResponseDTO(
            id = id,
            todo = "overwrite todo",
            isDone = false,
            categories = listOf(),
            ownerId = user.userId,
            price = 500L,
        )
        given(authService.findUser(any())).willReturn(user)
        given(
            todoRetryableService.updateTodo(eq(id), eq(user.userId), eq(request.toUpdateRequest()))
        ).willReturn(mockResponse)

        mockMvc.put("/api/v1/todos/$id") {
            content = objectMapper.writeValueAsString(request)
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer a")
        }
            .andDo { print() }
            .andExpect {
                status { isOk() }
            }

        verify(todoRetryableService).updateTodo(eq(id), eq(user.userId), eq(request.toUpdateRequest()))
    }

    @Test
    fun `updateTodo(PUT)에 id가 숫자가 아닌 값이 들어오면 bad request 이고 TodoRetryableService 는 호출되지 않는다`() {
        val request = TodoUpdateRequest(
            todo = "overwrite todo",
            isDone = false,
            price = 500L,
            categories = listOf("category1"),
        )
        mockMvc.put("/api/v1/todos/hi") {
            content = objectMapper.writeValueAsString(request)
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer a")
        }
            .andDo { print() }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value(TodoExceptionType.ID_BAD_REQUEST.errorCode) }
            }

        verify(todoRetryableService, never()).updateTodo(any(), any(), any())
    }

    @Test
    fun `deleteTodo 호출시 TodoRetryableService deleteTodo 에 route id, 인증된 사용자 id 를 위임한다`() {
        val id = 1L
        val user = User("pita", "pita")
        given(authService.findUser(any())).willReturn(user)

        mockMvc.delete("/api/v1/todos/$id") {
            header("Authorization", "Bearer a")
        }
            .andDo { print() }
            .andExpect {
                status { isNoContent() }
            }

        verify(todoRetryableService).deleteTodo(eq(id), eq(user.userId))
    }

    @Test
    fun `deleteTodo에 id가 숫자가 아닌 값이 들어오면 bad request 이고 TodoRetryableService 는 호출되지 않는다`() {
        mockMvc.delete("/api/v1/todos/hi") {
            header("Authorization", "Bearer a")
        }
            .andDo { print() }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value(TodoExceptionType.ID_BAD_REQUEST.errorCode) }
            }

        verify(todoRetryableService, never()).deleteTodo(any(), any())
    }
}
