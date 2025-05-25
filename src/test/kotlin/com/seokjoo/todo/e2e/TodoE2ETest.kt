package com.seokjoo.todo.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import com.seokjoo.todo.domain.repository.todouser.TodoAuthRepository
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.auth.TodoAuthServiceLoginResponse
import com.seokjoo.todo.domain.service.todo.TodoCreateServiceRequestDTO
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import com.seokjoo.todo.presentation.category.dto.CategoryDTO
import com.seokjoo.todo.presentation.todo.dto.request.TodoRequest
import com.seokjoo.todo.presentation.todo.dto.request.TodoUpdateRequest
import com.seokjoo.todo.presentation.todo.dto.response.TodoPageResponse
import com.seokjoo.todo.presentation.todo.dto.response.TodoResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.net.URI

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TodoE2ETest @Autowired constructor(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
    private val loginService: TodoAuthService,
    private val todoService: TodoService,
    private val todoRepository: TodoRepository,
    private val userRepository: TodoAuthRepository,
) {

    @LocalServerPort
    private val port: Int = 8080

    private val restTemplate: RestTemplate = RestTemplate()

    private lateinit var header: HttpHeaders
    private lateinit var todoResponse: TodoServiceResponseDTO

    @Test
    fun `GET All todo e2e 테스트`() {
        val url = "http://localhost:$port/api/v1/todos"
        val expected = TodoPageResponse(
            isLast = true,
            todoList = listOf(
                TodoResponse(
                    id = 1L,
                    todo = "spring",
                    isDone = false,
                    categories = emptyList(),
                    owner = "pita",
                    price = 10L,
                )
            )
        )

        val response: ResponseEntity<String> = restTemplate.exchange(
            url,
            HttpMethod.GET,
            HttpEntity<String>(header),
            String::class.java
        )
        val result = objectMapper.readValue<TodoPageResponse>(response.body.orEmpty())

        assertThat(response.statusCode.value()).isEqualTo(200)
        assertThat(result)
            .usingRecursiveComparison()
            .isEqualTo(expected)
    }

    @Test
    fun `GET Todo by id e2e 테스트`() {
        val url = "http://localhost:$port/api/v1/todos/${todoResponse.id}"
        val expected = TodoResponse(
            id = todoResponse.id,
            todo = "spring",
            isDone = false,
            categories = emptyList(),
            owner = "pita",
            price = 10L,
        )

        val response: ResponseEntity<String> = restTemplate.exchange(
            url,
            HttpMethod.GET,
            HttpEntity<String>(header),
            String::class.java
        )
        val result = objectMapper.readValue<TodoResponse>(response.body.orEmpty())

        assertThat(response.statusCode.value()).isEqualTo(200)
        assertThat(result)
            .usingRecursiveComparison()
            .isEqualTo(expected)
    }

    @Test
    fun `POST Todo create e2e 테스트`() {
        val url = "http://localhost:$port/api/v1/todos"
        val expectedId = todoResponse.id + 1
        val expected = TodoResponse(
            id = expectedId,
            todo = "Android",
            isDone = true,
            categories = listOf("drama"),
            owner = "pita",
            price = 10L,
        )
        val request =
            TodoRequest(todo = "Android", isDone = true, categories = listOf(CategoryDTO("drama")), price = 10L)

        val response: ResponseEntity<String> = restTemplate.exchange(
            url,
            HttpMethod.POST,
            HttpEntity(request, header),
            String::class.java
        )
        val result = objectMapper.readValue<TodoResponse>(response.body.orEmpty())

        assertThat(response.statusCode.value()).isEqualTo(201)
        assertThat(response.headers.location).isEqualTo(URI.create("/todos/${expectedId}"))
        assertThat(result)
            .usingRecursiveComparison()
            .isEqualTo(expected)
    }

    @Test
    fun `DELETE Todo delete e2e 테스트`() {
        val url = "http://localhost:$port/api/v1/todos/${todoResponse.id}"

        val response: ResponseEntity<String> = restTemplate.exchange(
            url = url,
            method = HttpMethod.DELETE,
            requestEntity = HttpEntity<String>(header),
            String::class
        )

        assertThat(response.statusCode.value()).isEqualTo(204)
    }

    @Test
    fun `PATCH Todo update e2e 테스트`() {
        val restTemplate = RestTemplate().apply {
            requestFactory = HttpComponentsClientHttpRequestFactory()
        }
        val url = "http://localhost:$port/api/v1/todos/${todoResponse.id}"
        val expected = TodoResponse(
            id = todoResponse.id,
            todo = "node",
            isDone = true,
            categories = listOf("drama"),
            owner = "pita",
            price = 10L,
        )
        val request =
            TodoUpdateRequest(todo = "node", isDone = true, categories = listOf(CategoryDTO("drama")), price = null)

        val responseEntity: ResponseEntity<String> =
            restTemplate.exchange(url, HttpMethod.PATCH, HttpEntity(request, header), String::class)
        val result = objectMapper.readValue<TodoResponse>(responseEntity.body.orEmpty())

        assertThat(responseEntity.statusCode.value()).isEqualTo(200)
        assertThat(result)
            .usingRecursiveComparison()
            .isEqualTo(expected)
    }

    @BeforeEach
    fun beforeEach() {
        val user = userRepository.findUserByUserId("pita") ?: error("올수 없음")
        todoResponse = todoService.createTodo(
            TodoCreateServiceRequestDTO(
                todo = "spring",
                price = 10L,
            ),
            owner = user,
        )
    }

    @AfterEach
    fun afterEach(testInfo: TestInfo) {
        with(redisTemplate) {
            delete(keys("todos:*"))
            delete(keys("todo:*"))
        }
        if (testInfo.displayName.contains("DELETE Todo delete e2e 테스트")) return
        todoService.deleteTodo(todoResponse.id, "pita")
    }

    @BeforeAll
    fun beforeAll() {
        loginService.signUp("pita", "pita")
        createHeader(loginService.login("pita", "pita"))
    }

    private fun createHeader(login: TodoAuthServiceLoginResponse) {
        header = HttpHeaders().apply {
            set("Authorization", "Bearer ${login.refreshToken}")
            contentType = MediaType.APPLICATION_JSON
        }
    }

    @AfterAll
    fun afterAll() {
        loginService.delete("pita", "pita")
        todoRepository.deleteAll()
    }
}
