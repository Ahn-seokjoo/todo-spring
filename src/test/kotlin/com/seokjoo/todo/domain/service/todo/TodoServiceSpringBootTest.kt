package com.seokjoo.todo.domain.service.todo

import com.seokjoo.todo.domain.repository.category.CategoryRepository
import com.seokjoo.todo.domain.repository.categorytodo.TodoCategoryRepository
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class TodoServiceSpringBootTest @Autowired constructor(
    private val service: TodoService,
    private val repository: TodoRepository,
    private val categoryRepository: CategoryRepository,
    private val todoCategoryRepository: TodoCategoryRepository,
) {

    // 통합 테스트
    @Test
    fun `createTodo 테스트`() {
        // given
        val request = TodoServiceRequestDTO("spring")
        // when
        val result = service.createTodo(request)

        // then
        Assertions.assertThat(result).usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(
                TodoServiceResponseDTO(
                    todo = "spring",
                    isDone = false,
                    categories = listOf(),
                    id = 0L
                )
            )
    }
}
