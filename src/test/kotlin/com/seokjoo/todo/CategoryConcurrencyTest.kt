package com.seokjoo.todo

import com.seokjoo.todo.domain.repository.category.CategoryRepository
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.presentation.category.dto.CategoryDTO
import com.seokjoo.todo.presentation.todo.dto.request.TodoRequest
import com.seokjoo.todo.presentation.todo.dto.request.toTodoServiceRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class CategoryConcurrencyTest @Autowired constructor(
    private val todoService: TodoService,
    private val categoryRepository: CategoryRepository,
) {
    @BeforeEach
    fun before() {
        categoryRepository.deleteAll()
    }

    @Test
    fun `동시에 같은 카테고리를 추가해도 중복으로 생성되지 않아야 한다`() = runBlocking {
        val categoryName = listOf(CategoryDTO("Spring"))

        val request = TodoRequest(
            todo = "Spring 동시성 이슈 실제로 보기",
            isDone = false,
            categories = categoryName,
        )

        val jobs = (1..30).map {
            launch(Dispatchers.IO) {
                todoService.createTodo(request.toTodoServiceRequest())
            }
        }
        jobs.joinAll()

        val allCategories = categoryRepository.findAll()
        println("tjrwn categories size is ${allCategories.size}")
    }

    @Test
    fun `동시에 같은 카테고리를 추가해도 중복으로 생성되지 않아야 한다 - Thread 버전`() {
        val categoryName = listOf(CategoryDTO("Spring"))

        val request = TodoRequest(
            todo = "Spring 동시성 이슈 실제로 보기",
            isDone = false,
            categories = categoryName,
        )

        val threads = mutableListOf<Thread>()

        repeat(30) {
            val thread = Thread {
                todoService.createTodo(request.toTodoServiceRequest())
            }
            threads.add(thread)
        }

        // Start all threads
        threads.forEach { it.start() }

        // Wait for all threads to finish
        threads.forEach { it.join() }

        val allCategories = categoryRepository.findAll()
        println("최종 카테고리 개수: ${allCategories.size}")
    }

}
