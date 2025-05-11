package com.seokjoo.todo

import com.seokjoo.todo.domain.repository.category.CategoryRepository
import com.seokjoo.todo.domain.repository.categorytodo.TodoCategoryRepository
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.presentation.category.dto.CategoryDTO
import com.seokjoo.todo.presentation.todo.dto.request.TodoRequest
import com.seokjoo.todo.presentation.todo.dto.request.toCreateRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class CategoryConcurrencyTest @Autowired constructor(
    private val todoService: TodoService,
    private val todoRepository: TodoRepository,
    private val categoryRepository: CategoryRepository,
    private val todoCategoryRepository: TodoCategoryRepository,
    private val todoAuthService: TodoAuthService,
) {

    @BeforeEach
    fun before() {
        categoryRepository.deleteAll()
        todoCategoryRepository.deleteAll()
        todoRepository.deleteAll()
        todoAuthService.signUp("pita", "pita")
    }

    @Test
    fun `동시에 같은 카테고리를 추가해도 중복으로 생성되지 않아야 한다 - Thread 버전`() {
        val token = todoAuthService.login("pita", "pita").accessToken
        val user = todoAuthService.findUser(token)
        val categoryName = listOf(CategoryDTO("Spring"), CategoryDTO("Android"))

        val request = TodoRequest(
            todo = "Spring 동시성 이슈 실제로 보기",
            isDone = false,
            categories = categoryName,
        )
        val threads = mutableListOf<Thread>()

        repeat(5) {
            val thread = Thread {
                todoService.createTodo(request.toCreateRequest(), user)
            }
            threads.add(thread)
        }

        // Start all threads
        threads.forEach { it.start() }

        // Wait for all threads to finish
        threads.forEach { it.join() }

        val allCategories = categoryRepository.findAll()
        assert(allCategories.size == 2)
    }
}
