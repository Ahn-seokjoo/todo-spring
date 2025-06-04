package com.seokjoo.todo.domain.service.todo

import com.seokjoo.todo.annotation.IntegrationTest
import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.repository.category.CategoryRepository
import com.seokjoo.todo.domain.repository.categorytodo.TodoCategoryRepository
import com.seokjoo.todo.domain.repository.todouser.TodoAuthRepository
import com.seokjoo.todo.presentation.todo.dto.request.TodoPageRequest
import com.seokjoo.todo.presentation.todo.dto.request.toPageServiceDTO
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.transaction.annotation.Transactional

@IntegrationTest
@Transactional
class TodoServiceSpringBootTest @Autowired constructor(
    private val service: TodoService,
    private val categoryRepository: CategoryRepository,
    private val todoCategoryRepository: TodoCategoryRepository,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val authRepository: TodoAuthRepository,
) {
    lateinit var result: TodoServiceResponseDTO
    val user = User("pita", "pita")

    @BeforeEach
    fun beforeEach() {
        authRepository.save(user)
        val request = TodoCreateServiceRequestDTO(todo = "android", isDone = true, categoryNames = listOf("drama"))
        // when
        result = service.createTodo(request, user)
    }

    @AfterEach
    fun cleanupRedis() {
        val keys = redisTemplate.keys("todos:*")
        redisTemplate.delete(keys)
    }

    // 통합 테스트
    @Test
    fun `createTodo 테스트`() {
        // given
        val user = User("pita2", "pita")
        authRepository.save(user)
        val request = TodoCreateServiceRequestDTO("spring", price = 10L)
        // when
        val result = service.createTodo(request, user)

        // then
        Assertions.assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(
                TodoServiceResponseDTO(
                    todo = request.todo,
                    isDone = request.isDone,
                    categories = listOf(),
                    id = 0L,
                    ownerId = user.userId,
                    price = request.price,
                )
            )
    }

    @Test
    fun `getTodoById 테스트`() {
        val id = result.id

        val todo = service.getTodoById(id)

        Assertions.assertThat(todo)
            .usingRecursiveComparison()
            .isEqualTo(
                TodoServiceResponseDTO(
                    id = id,
                    todo = "android",
                    isDone = true,
                    categories = listOf("drama"),
                    ownerId = "pita",
                    price = 0L,
                )
            )
    }

    @Test
    fun `getAllTodos 테스트`() {
        /**
         * 이미 한개가 존재하기 때문에, 한개 더 추가 이후 총 2개인지 테스트
         * given
         */
        val request = TodoCreateServiceRequestDTO(todo = "node", price = 100L)
        service.createTodo(request, user)

        // when
        val todoPageRequest = TodoPageRequest()
        val todoList = service.getPagedTodos(user.userId, todoPageRequest.toPageServiceDTO()).responseList
        // then
        assert(todoList.size == 2)
        Assertions.assertThat(todoList)
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(
                listOf(
                    TodoServiceResponseDTO(
                        id = 1L,
                        isDone = true,
                        todo = "android",
                        categories = listOf("drama"),
                        ownerId = "pita",
                        price = 0L,
                    ),
                    TodoServiceResponseDTO(
                        id = 2L,
                        isDone = false,
                        todo = "node",
                        categories = listOf(),
                        ownerId = "pita",
                        price = 100L,
                    ),
                )
            )
    }

    @Test
    fun `updateTodo 테스트`() {
        // given
        val updateId = result.id
        val request =
            TodoUpdateServiceRequestDTO(todo = "iOS", isDone = false, categoryNames = listOf("horror"), price = 400L)

        // when
        val result = service.updateTodo(todoId = updateId, request = request, userId = user.userId)

        // then
        Assertions.assertThat(result)
            .isEqualTo(
                TodoServiceResponseDTO(
                    id = updateId,
                    isDone = false,
                    todo = "iOS",
                    categories = listOf("drama", "horror"),
                    ownerId = "pita",
                    price = 400L,
                ),
            )
    }

    @Test
    fun `delete 테스트 - 제거 이후 Todo DB 조회`() {
        // given
        val deleteId = result.id
        service.deleteTodo(deleteId, user.userId)

        // when
        val todoPageRequest = TodoPageRequest()
        val removedList = service.getPagedTodos(user.userId, todoPageRequest.toPageServiceDTO())

        // then
        assert(removedList.responseList.isEmpty())
    }

    @Test
    fun `delete 테스트 - 제거 이후 Category DB 조회`() {
        // given
        val beforeEntity = categoryRepository.findCategoryByName("drama")

        val deleteId = result.id
        service.deleteTodo(deleteId, user.userId)

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
            categoryRepository.findCategoryByName("drama")?.id ?: 0L
        val todoCategoryBeforeCountByRemove = todoCategoryRepository.countByCategoryId(categoryId)
        val deleteId = result.id
        service.deleteTodo(deleteId, user.userId)

        // when
        val todoCategoryAfterCountByRemove = todoCategoryRepository.countByCategoryId(categoryId)

        // then
        assert(todoCategoryBeforeCountByRemove == 1)
        assert(todoCategoryAfterCountByRemove == 0)
    }

    @Test
    fun `getByTodo 시에 없는 아이디를 조회했을 때 NOT_EXISTED_TODO 을 잘 던져주는지`() {
        val exception = kotlin.runCatching {
            service.getTodoById(200L)
        }.exceptionOrNull()

        assert(exception is TodoException)
        with(exception as TodoException) {
            assert(message == "존재하지 않는 Todo 입니다")
            assert(errorCode == "T000_TODO_ERROR")
            assert(httpStatusCode == 404)
        }
    }

    @Test
    fun `updateByTodo 시에 없는 아이디를 조회했을 때 NOT_EXISTED_TODO 을 잘 던져주는지`() {
        val exception = kotlin.runCatching {
            service.updateTodo(200L, user.userId, TodoUpdateServiceRequestDTO(todo = "", isDone = null, price = null))
        }.exceptionOrNull()

        assert(exception is TodoException)
        with(exception as TodoException) {
            assert(message == "존재하지 않는 Todo 입니다")
            assert(errorCode == "T000_TODO_ERROR")
            assert(httpStatusCode == 404)
        }
    }

    @Test
    fun `deleteTodo 시에 없는 아이디를 조회했을 때 NOT_EXISTED_TODO 을 잘 던져주는지`() {
        val exception = kotlin.runCatching {
            service.deleteTodo(200L, user.userId)
        }.exceptionOrNull()

        assert(exception is TodoException)
        with(exception as TodoException) {
            assert(message == "존재하지 않는 Todo 입니다")
            assert(errorCode == "T000_TODO_ERROR")
            assert(httpStatusCode == 404)
        }
    }
}
