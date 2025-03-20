package com.seokjoo.todo.domain.service.todo

import com.seokjoo.todo.domain.repository.category.CategoryRepository
import com.seokjoo.todo.domain.repository.categorytodo.TodoCategoryRepository
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class TodoServiceSpringBootTest @Autowired constructor(
    private val service: TodoService,
    private val todoRepository: TodoRepository,
    private val categoryRepository: CategoryRepository,
    private val todoCategoryRepository: TodoCategoryRepository,
) {
    var result: TodoServiceResponseDTO? = null

    @BeforeEach
    fun beforeEach() {
        val request = TodoServiceRequestDTO(todo = "android", isDone = true, categoryNames = listOf("drama"))
        // when
        result = service.createTodo(request)
    }

    @AfterEach
    fun afterEach() {
        result = null
    }

    // 통합 테스트
    @Test
    fun `createTodo 테스트`() {
        // given
        val request = TodoServiceRequestDTO("spring")
        // when
        val result = service.createTodo(request)

        // then
        Assertions.assertThat(result)
            .usingRecursiveComparison()
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

    @Test
    fun `getTodoById 테스트`() {
        val id = result?.id ?: return

        val todo = service.getTodoById(id)

        Assertions.assertThat(todo)
            .usingRecursiveComparison()
            .isEqualTo(
                TodoServiceResponseDTO(
                    id = id,
                    todo = "android",
                    isDone = true,
                    categories = listOf("drama"),
                )
            )
    }

    @Test
    fun `getAllTodos 테스트`() {
        /**
         * 이미 한개가 존재하기 때문에, 한개 더 추가 이후 총 2개인지 테스트
         * given
         */
        val request = TodoServiceRequestDTO("node")
        service.createTodo(request)

        // when
        val todoList = service.getAllTodos()
        // then
        assert(todoList.size == 2)
        Assertions.assertThat(todoList)
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(
                listOf(
                    TodoServiceResponseDTO(id = 1L, isDone = true, todo = "android", categories = listOf("drama")),
                    TodoServiceResponseDTO(id = 2L, isDone = false, todo = "node", categories = listOf()),
                )
            )
    }

    @Test
    fun `updateTodo 테스트`() {
        // given
        val updateId = result?.id ?: return
        val request = TodoServiceRequestDTO(todo = "iOS", isDone = false, categoryNames = listOf("horror"))

        // when
        val result = service.updateTodo(id = updateId, request = request)

        // then
        Assertions.assertThat(result)
            .isEqualTo(
                TodoServiceResponseDTO(
                    id = updateId,
                    isDone = false,
                    todo = "iOS",
                    categories = listOf("drama", "horror"),
                ),
            )
    }

    /**
     * getAll 같은 함수들은 영속성 컨텍스트에 남아있어서 제대로 테스트가 안됨 ,,
     */
    @Test
    fun `delete 테스트 - 제거 이후 Todo DB 조회`() {
        // given
        val deleteId = result?.id ?: throw IllegalStateException("non-null is null")
        service.deleteTodo(deleteId)

        // when
        val deletedEntity = todoRepository.findByIdOrNull(deleteId)

        // then
        assert(deletedEntity == null)
    }

    @Test
    fun `delete 테스트 - 제거 이후 Category DB 조회`() {
        // given
        val beforeEntity = categoryRepository.findCategoryByName("drama")

        val deleteId = result?.id ?: throw IllegalStateException("non-null is null")
        service.deleteTodo(deleteId)

        // when
        val deletedEntity = categoryRepository.findCategoryByName("drama")

        // then
        assert(beforeEntity?.name == "drama")
        assert(deletedEntity == null)
    }

    @Test
    fun `delete 테스트 - 제거 이후 TodoCategory DB 조회, Cascade로 같이 잘 제거가 됐는지`() {
        // given
        val categoryId =
            categoryRepository.findCategoryByName("drama")?.id ?: throw IllegalStateException("non-null is null")
        val todoCategoryBeforeCountByRemove = todoCategoryRepository.countByCategoryId(categoryId)
        val deleteId = result?.id ?: throw IllegalStateException("non-null is null")
        service.deleteTodo(deleteId)

        // when
        val todoCategoryAfterCountByRemove = todoCategoryRepository.countByCategoryId(categoryId)

        // then
        assert(todoCategoryBeforeCountByRemove == 1)
        assert(todoCategoryAfterCountByRemove == 0)
    }
}
