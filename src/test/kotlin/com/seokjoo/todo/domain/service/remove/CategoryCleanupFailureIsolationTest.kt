package com.seokjoo.todo.domain.service.remove

import com.seokjoo.todo.annotation.TodoTest
import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.repository.category.CategoryRepository
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import com.seokjoo.todo.domain.repository.todouser.TodoAuthRepository
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.category.listener.CategoryDeletedListener
import com.seokjoo.todo.domain.service.todo.TodoCreateServiceRequestDTO
import com.seokjoo.todo.domain.service.todo.TodoService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.timeout
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.util.AopTestUtils

@TodoTest
class CategoryCleanupFailureIsolationTest @Autowired constructor(
    private val authService: TodoAuthService,
    private val todoService: TodoService,
    private val todoRepository: TodoRepository,
    private val categoryRepository: CategoryRepository,
    private val userRepository: TodoAuthRepository,
) {

    @MockitoSpyBean
    private lateinit var injectedListener: CategoryDeletedListener

    private val categoryDeletedListener: CategoryDeletedListener
        get() = AopTestUtils.getUltimateTargetObject(injectedListener)

    private val userId = "cleanup-fail-user"
    private val category = "cleanup-fail-category"

    @BeforeEach
    fun setUp() {
        cleanUp()
        authService.signUp(userId, userId, "abc@nav.com")
    }

    @AfterEach
    fun after() {
        cleanUp()
    }

    @Test
    fun `카테고리 정리 리스너가 예외를 던져도 투두 삭제는 커밋된다`() {
        val owner = authService.findUserByUserId(userId)
        val todo = todoService.createTodo(
            request = TodoCreateServiceRequestDTO(todo = "only", categoryNames = listOf(category)),
            owner = owner,
        )
        doThrow(RuntimeException("정리 실패(테스트)")).whenever(categoryDeletedListener).eventListener(any())

        todoService.deleteTodo(todo.id, userId)   // 예외가 호출자에게 전파되지 않아야 한다

        // 리스너가 실제로 호출되어 실패했음을 먼저 확인한다 (비동기라서 timeout 으로 기다린다)
        verify(categoryDeletedListener, timeout(5000)).eventListener(any())

        assertThat(todoRepository.findById(todo.id)).isEmpty()                  // 삭제는 커밋됨
        assertThat(categoryRepository.findCategoryByName(category)).isNotNull() // 정리만 실패해 고아로 남음
    }

    @Test
    fun `카테고리 정리 리스너가 예외를 던져도 회원 탈퇴는 커밋된다`() {
        val owner = authService.findUserByUserId(userId)
        val todo = todoService.createTodo(
            request = TodoCreateServiceRequestDTO(todo = "only", categoryNames = listOf(category)),
            owner = owner,
        )
        doThrow(RuntimeException("정리 실패(테스트)")).whenever(categoryDeletedListener).eventListener(any())

        authService.delete(userId, userId)   // 예외가 호출자에게 전파되지 않아야 한다

        verify(categoryDeletedListener, timeout(5000)).eventListener(any())

        assertThat(userRepository.findUserWithTodosByUserId(userId)).isNull()   // 탈퇴는 커밋됨
        assertThat(todoRepository.findById(todo.id)).isEmpty()                  // 투두도 cascade 로 삭제됨
        assertThat(categoryRepository.findCategoryByName(category)).isNotNull() // 정리만 실패해 고아로 남음
    }

    private fun cleanUp() {
        try {
            authService.delete(userId, userId)
        } catch (e: TodoException) {
            // 이미 없는 유저만 무시한다
            if (e.errorCode != TodoExceptionType.AUTH_USER_NOT_EXIST.errorCode) throw e
        }
        categoryRepository.findCategoryByName(category)?.let { categoryRepository.delete(it) }
    }
}
