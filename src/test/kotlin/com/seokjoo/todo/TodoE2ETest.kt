package com.seokjoo.todo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.seokjoo.todo.domain.repository.category.CategoryRepository
import com.seokjoo.todo.presentation.category.dto.CategoryDTO
import com.seokjoo.todo.presentation.todo.dto.request.TodoRequest
import com.seokjoo.todo.presentation.todo.dto.response.TodoResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.client.postForEntity
import java.net.URI

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TodoE2ETest {

    @LocalServerPort
    private val port: Int = 8080

    private val restTemplate: RestTemplate = RestTemplate()

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    /**
     * post로 생성시에 1부터 생성해서 2로 세팅함
     */
    @BeforeEach
    fun beforeEach() {
        jdbcTemplate.execute("INSERT INTO todo (todo_id, is_done, todo) VALUES (2, false, 'spring')")
    }

    @AfterEach
    fun afterEach() {
        jdbcTemplate.execute("DELETE FROM todo_category WHERE todo_id = 2")
        jdbcTemplate.execute("DELETE FROM todo WHERE todo_id = 2")
    }

    @Test
    fun `GET All todo e2e 테스트`() {
        val url = "http://localhost:$port/api/v1/todos"
        val expected = listOf(TodoResponse(id = 2L, todo = "spring", isDone = false, categories = emptyList()))

        val response: ResponseEntity<String> = restTemplate.getForEntity(url, String::class.java)

        assertThat(response.statusCode.value()).isEqualTo(200)

        val result = objectMapper.readValue<List<TodoResponse>>(response.body.orEmpty())

        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(expected)
    }

    @Test
    fun `GET Todo by id e2e 테스트`() {
        val url = "http://localhost:$port/api/v1/todos/2"
        val expected = TodoResponse(id = 10L, todo = "spring", isDone = false, categories = emptyList())

        val response: ResponseEntity<String> = restTemplate.getForEntity(url, String::class.java)

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

        val response: ResponseEntity<String> = restTemplate.postForEntity<String>(url = url, request = request)

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
            requestEntity = HttpEntity<String>(HttpHeaders()),
            String::class
        )

        assertThat(response.statusCode.value()).isEqualTo(204)
    }

    @Test
    fun `PATCH Todo update e2e 테스트`() {
        // after code를 지우고, update 만 수행하면 잘된다.. 다른것 다같이 all 수행 시에는 실패 ..왜그럴까
        val restTemplate = RestTemplate().apply {
            requestFactory = HttpComponentsClientHttpRequestFactory()
        }
        val url = "http://localhost:$port/api/v1/todos/2"
        val expected = TodoResponse(id = 2L, todo = "node", isDone = true, categories = listOf("drama"))
        val request = TodoRequest(todo = "node", isDone = true, categories = listOf(CategoryDTO("drama")))

        val responseEntity: ResponseEntity<String> =
            restTemplate.exchange(url, HttpMethod.PATCH, HttpEntity(request), String::class)
        assertThat(responseEntity.statusCode.value()).isEqualTo(200)

        val result = objectMapper.readValue<TodoResponse>(responseEntity.body.orEmpty())

        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(expected)
    }
}
