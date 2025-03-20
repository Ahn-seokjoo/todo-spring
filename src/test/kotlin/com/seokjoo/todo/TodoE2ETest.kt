package com.seokjoo.todo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.seokjoo.todo.presentation.todo.dto.response.TodoResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.client.RestTemplate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TodoE2ETest {

    @LocalServerPort
    private val port: Int = 8080

    private val restTemplate = RestTemplate()

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun beforeEach() {
        jdbcTemplate.execute("INSERT INTO todo (todo_id, is_done, todo) VALUES (1, false, 'spring')")
    }

    @Test
    fun `GET hello should return Hello, user!`() {
        val url = "http://localhost:$port/api/v1/todos"
        val expected = listOf(TodoResponse(id = 1L, todo = "spring", isDone = false, categories = emptyList()))

        val response: ResponseEntity<String> = restTemplate.getForEntity(url, String::class.java)

        assertThat(response.statusCode.value()).isEqualTo(200)

        val result = objectMapper.readValue<List<TodoResponse>>(response.body.orEmpty())

        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(expected)
    }
}
