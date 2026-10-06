package com.seokjoo.todo.domain.service.remove

import com.seokjoo.todo.annotation.TodoTest
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.repository.category.CategoryRepository
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import com.seokjoo.todo.domain.repository.todouser.TodoAuthRepository
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.todo.TodoCreateServiceRequestDTO
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 카테고리 정리는 "커밋 이후, 비동기" 로 일어난다.
 * 그래서 이 클래스에는 @Transactional 을 붙이지 않는다. (롤백되면 AFTER_COMMIT 리스너가 실행되지 않는다)
 * 대신 데이터를 직접 정리하고, 비동기 결과는 await 로 기다린다.
 */
@TodoTest
class TodoDeleteEventIntegrationTest @Autowired constructor(
    private val todoService: TodoService,
    private val todoRepository: TodoRepository,
    private val categoryRepository: CategoryRepository,
    private val userRepository: TodoAuthRepository,
    private val authService: TodoAuthService,
) {
    private val userId = "event-user"
    private val withdrawUserId = "event-withdraw-user"
    private val otherUserId = "event-other-user"
    private val password = "pw"
    private val drama = "event-drama"
    private val action = "event-action"
    private lateinit var user: User

    @BeforeEach
    fun before() {
        cleanUp()
        user = userRepository.save(User(userId, "pw"))
    }

    @AfterEach
    fun after() {
        cleanUp()
    }

    @Test
    fun `카테고리를 쓰던 마지막 투두를 지우면, 커밋 이후 카테고리가 같이 삭제된다`() {
        val todo = createTodo("only", listOf(drama, action))

        todoService.deleteTodo(todo.id, userId)

        assertThat(todoRepository.findById(todo.id)).isEmpty()
        awaitCategoryRemoved(drama)
        awaitCategoryRemoved(action)
    }

    @Test
    fun `다른 투두가 쓰고 있는 카테고리는 남고, 더 이상 쓰이지 않는 카테고리만 삭제된다`() {
        val first = createTodo("first", listOf(drama, action))
        createTodo("second", listOf(drama))

        todoService.deleteTodo(first.id, userId)

        // action 이 사라졌다는 건 정리 리스너가 끝났다는 뜻이다. 그 뒤의 drama 확인은 믿을 수 있다.
        awaitCategoryRemoved(action)
        assertThat(categoryRepository.findCategoryByName(drama)).isNotNull()
    }

    @Test
    fun `카테고리가 없는 투두를 지워도 예외 없이 삭제된다`() {
        val todo = createTodo("no category", emptyList())

        todoService.deleteTodo(todo.id, userId)

        assertThat(todoRepository.findById(todo.id)).isEmpty()
    }

    @Test
    fun `같은 카테고리를 쓰는 투두 두 개를 동시에 지워도 카테고리가 고아로 남지 않는다`() {
        val first = createTodo("first", listOf(drama))
        val second = createTodo("second", listOf(drama))

        val executor = Executors.newFixedThreadPool(2)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        try {
            val futures = listOf(first, second).map { target ->
                executor.submit {
                    ready.countDown()
                    start.await()
                    todoService.deleteTodo(target.id, userId)
                }
            }
            ready.await()
            start.countDown()
            futures.forEach { it.get(10, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }

        // 마지막에 커밋한 쪽의 정리가 "참조 0개" 를 보기 때문에, 카테고리가 남지 않아야 한다
        awaitCategoryRemoved(drama)
    }

    @Test
    fun `회원을 탈퇴하면 투두가 삭제되고, 더 이상 쓰이지 않는 카테고리만 커밋 이후 삭제된다`() {
        authService.signUp(withdrawUserId, password, "withdraw@test.com")
        authService.signUp(otherUserId, password, "other@test.com")
        val withdrawUser = authService.findUserByUserId(withdrawUserId)
        val otherUser = authService.findUserByUserId(otherUserId)
        createTodo("mine-1", listOf(drama), withdrawUser)
        createTodo("mine-2", listOf(action), withdrawUser)
        createTodo("other", listOf(drama), otherUser)

        authService.delete(withdrawUserId, password)

        assertThat(todoRepository.countByOwnerId(withdrawUserId)).isEqualTo(0L)
        assertThat(todoRepository.countByOwnerId(otherUserId)).isEqualTo(1L)
        // action 이 사라졌다는 건 정리 리스너가 끝났다는 뜻이다. 다른 유저가 쓰는 drama 는 남아 있어야 한다.
        awaitCategoryRemoved(action)
        assertThat(categoryRepository.findCategoryByName(drama)).isNotNull()
    }

    private fun createTodo(
        todo: String,
        categoryNames: List<String>,
        owner: User = user,
    ): TodoServiceResponseDTO {
        return todoService.createTodo(
            request = TodoCreateServiceRequestDTO(todo = todo, categoryNames = categoryNames),
            owner = owner,
        )
    }

    private fun awaitCategoryRemoved(name: String) {
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertThat(categoryRepository.findCategoryByName(name)).isNull()
        }
    }

    private fun cleanUp() {
        // 유저를 지우면 cascade 로 투두와 todo_category 도 같이 지워진다. 카테고리는 이름으로 직접 지운다.
        listOf(userId, withdrawUserId, otherUserId).forEach { id ->
            userRepository.findUserWithTodosByUserId(id)?.let { userRepository.delete(it) }
        }
        listOf(drama, action).forEach { name ->
            categoryRepository.findCategoryByName(name)?.let { categoryRepository.delete(it) }
        }
    }
}
