package com.seokjoo.todo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.presentation.category.dto.CategoryDTO
import com.seokjoo.todo.presentation.todo.dto.request.TodoRequest
import com.seokjoo.todo.presentation.todo.dto.response.TodoPageResponse
import com.seokjoo.todo.presentation.todo.dto.response.TodoResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TodoE2ETest @Autowired constructor(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
    private val jdbcTemplate: JdbcTemplate,
    private val loginService: TodoAuthService,
) {

    @LocalServerPort
    private val port: Int = 8080

    private val restTemplate: RestTemplate = RestTemplate()

    private lateinit var header: HttpHeaders

    /**
     * post로 생성시에 1부터 생성해서 2로 세팅함
     */
    @BeforeEach
    fun beforeEach() {
        insertInitialData()
        addHeader()
    }

    @AfterEach
    fun cleanupRedis() {
        val todos = redisTemplate.keys("todos:*")
        val todo = redisTemplate.keys("todo:*")
        redisTemplate.delete(todos)
        redisTemplate.delete(todo)
    }

    private fun insertInitialData() {
        val currentTime = LocalDateTime.now()
        val formattedTime = currentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val sql = "INSERT INTO todo (todo_id, is_done, todo, created_at, updated_at) VALUES (2, false, 'spring', ?, ?)"
        jdbcTemplate.update(sql, formattedTime, formattedTime)
    }

    private fun addHeader() {
        loginService.signUp("pita", "pita")
        val result = loginService.login("pita", "pita")
        header = HttpHeaders().apply {
            set("Authorization", "Bearer ${result.refreshToken}")
            contentType = MediaType.APPLICATION_JSON
        }
    }

    @AfterEach
    fun afterEach() {
        jdbcTemplate.execute("DELETE FROM todo_category WHERE todo_id = 2")
        jdbcTemplate.execute("DELETE FROM todo WHERE todo_id = 2")
        loginService.delete("pita", "pita")
    }

    @Test
    fun `GET All todo e2e 테스트`() {
        val url = "http://localhost:$port/api/v1/todos"
        val expected = TodoPageResponse(
            isLast = true,
            todoList = listOf(
                TodoResponse(id = 2L, todo = "spring", isDone = false, categories = emptyList())
            )
        )

        val response: ResponseEntity<String> = restTemplate.exchange(
            url,
            HttpMethod.GET,
            HttpEntity<String>(header),
            String::class.java
        )

        assertThat(response.statusCode.value()).isEqualTo(200)

        val result = objectMapper.readValue<TodoPageResponse>(response.body.orEmpty())

        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(expected)
    }

    @Test
    fun `GET Todo by id e2e 테스트`() {
        val url = "http://localhost:$port/api/v1/todos/2"
        val expected = TodoResponse(id = 10L, todo = "spring", isDone = false, categories = emptyList())

        val response: ResponseEntity<String> = restTemplate.exchange(
            url,
            HttpMethod.GET,
            HttpEntity<String>(header),
            String::class.java
        )

        assertThat(response.statusCode.value()).isEqualTo(200)

        val result = objectMapper.readValue<TodoResponse>(response.body.orEmpty())

        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(expected)
    }

    @Test
    fun `POST Todo create e2e 테스트`() {
        val url = "http://localhost:$port/api/v1/todos"
        val expected = TodoResponse(id = 1L, todo = "Android", isDone = true, categories = listOf("drama"))
        val request = TodoRequest(todo = "Android", isDone = true, categories = listOf(CategoryDTO("drama")))

        val response: ResponseEntity<String> = restTemplate.exchange(
            url,
            HttpMethod.POST,
            HttpEntity(request, header),
            String::class.java
        )

        assertThat(response.statusCode.value()).isEqualTo(201)
        assertThat(response.headers.location).isEqualTo(URI.create("/todos/1"))

        val result = objectMapper.readValue<TodoResponse>(response.body.orEmpty())

        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(expected)
    }

    @Test
    fun `DELETE Todo delete e2e 테스트`() {
        val url = "http://localhost:$port/api/v1/todos/1"

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
        val url = "http://localhost:$port/api/v1/todos/2"
        val expected = TodoResponse(id = 2L, todo = "node", isDone = true, categories = listOf("drama"))
        val request = TodoRequest(todo = "node", isDone = true, categories = listOf(CategoryDTO("drama")))

        val responseEntity: ResponseEntity<String> =
            restTemplate.exchange(url, HttpMethod.PATCH, HttpEntity(request, header), String::class)
        assertThat(responseEntity.statusCode.value()).isEqualTo(200)

        val result = objectMapper.readValue<TodoResponse>(responseEntity.body.orEmpty())

        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(expected)
    }
}
