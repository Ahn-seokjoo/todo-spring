package com.seokjoo.todo.domain.service.todo

import com.seokjoo.todo.annotation.TodoTest
import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager

@TodoTest
class TodoCacheTest @Autowired constructor(
    private val authService: TodoAuthService,
    private val todoService: TodoService,
    @Qualifier("todoCacheManager") private val cacheManager: CacheManager,
) {
    @BeforeEach
    fun setUp() {
        cacheManager.getCache("todos")?.clear()
        cacheManager.getCache("todo")?.clear()
        authService.signUp("pita", "pita", "")
    }

    @AfterEach
    fun after() {
        try {
            authService.delete("pita", "pita")
        } catch (e: TodoException) {
            // 탈퇴 테스트처럼 이미 지워진 경우만 무시한다
            if (e.errorCode != TodoExceptionType.AUTH_USER_NOT_EXIST.errorCode) throw e
        } finally {
            cacheManager.getCache("todos")?.clear()
            cacheManager.getCache("todo")?.clear()
        }
    }

    @Test
    fun `getPagesTodos 는 unless로 빈 결과를 캐시하지 않습니다`() {
        // given
        val user = authService.findUserByUserId("pita")
        todoService.createTodo(request = TodoCreateServiceRequestDTO(todo = "hi"), owner = user)

        todoService.getPagedTodos("pita", TodoPageServiceDTO(0, 10))
        todoService.getPagedTodos("pita", TodoPageServiceDTO(1, 10))   // 빈 결과

        assertThat(listCache("pita", 0, 10)).isNotNull   // 채워졌다
        assertThat(listCache("pita", 1, 10)).isNull()    // 빈 결과는 안 채워졌다
    }

    @Test
    fun `create todo 이후 getPagesTodo가 업데이트된다`() {
        val user = authService.findUserByUserId("pita")
        todoService.createTodo(request = TodoCreateServiceRequestDTO(todo = "hi"), owner = user)
        assertThat(listCache("pita", 0, 10)).isNull()

        val result = todoService.getPagedTodos("pita", TodoPageServiceDTO(0, 10))
        assertThat(result.responseList.size).isEqualTo(1)
        assertThat(listCache("pita", 0, 10)).isNotNull()

        todoService.createTodo(request = TodoCreateServiceRequestDTO(todo = "hi"), owner = user)
        assertThat(listCache("pita", 0, 10)).isNull()

        val result2 = todoService.getPagedTodos("pita", TodoPageServiceDTO(0, 10))
        assertThat(result2.responseList.size).isEqualTo(2)
        assertThat(listCache("pita", 0, 10)).isNotNull()
    }

    @Test
    fun `getTodoById가 update 이후 새로운 값을 준다`() {
        // 1회차 캐시 조회
        val user = authService.findUserByUserId("pita")
        val todo = todoService.createTodo(request = TodoCreateServiceRequestDTO(todo = "hi"), owner = user)
        val firstTodo = todoService.getTodoById(todo.id)
        val firstCache = singleCache(firstTodo.id)

        assertThat((firstCache?.get() as? TodoServiceResponseDTO)?.todo).isEqualTo(firstTodo.todo)
        // 2회차, 업데이트 이후 캐시 조회
        todoService.updateTodo(firstTodo.id, user.userId, TodoUpdateServiceRequestDTO(todo = "bye", null, null))

        assertThat(singleCache(todo.id)).isNull()
        assertThat(todoService.getTodoById(todo.id).todo).isEqualTo("bye")
    }

    @Test
    fun `delete 이후 evict 잘되는지`() {
        // 1회차 캐시 조회
        val user = authService.findUserByUserId("pita")
        val todo = todoService.createTodo(request = TodoCreateServiceRequestDTO(todo = "hi"), owner = user)
        val firstTodo = todoService.getTodoById(todo.id)
        val firstCache = singleCache(firstTodo.id)
        todoService.getPagedTodos("pita", TodoPageServiceDTO(0, 10))

        assertThat(listCache("pita", 0, 10)).isNotNull
        assertThat((firstCache?.get() as? TodoServiceResponseDTO)?.todo).isEqualTo(firstTodo.todo)

        todoService.deleteTodo(todo.id, user.userId)

        // 삭제 이후 조회시에 캐시 리턴안하고 없다고 즉시 표시
        val result = assertThrows<TodoException> { todoService.getTodoById(todo.id) }
        assertThat(result.errorCode).isEqualTo(TodoExceptionType.NOT_EXISTED_TODO.errorCode)
        assertThat(result.httpStatusCode).isEqualTo(TodoExceptionType.NOT_EXISTED_TODO.httpStatusCode)
        assertThat(result.message).isEqualTo(TodoExceptionType.NOT_EXISTED_TODO.message)

        // 캐시 증발 체크
        assertThat(listCache("pita", 0, 10)).isNull()
        assertThat(singleCache(todo.id)).isNull()
    }

    @Test
    fun `탈퇴 이후에도 캐시가 비워진다`() {
        val user = authService.findUserByUserId("pita")
        val todo = todoService.createTodo(request = TodoCreateServiceRequestDTO(todo = "hi"), owner = user)
        val firstTodo = todoService.getTodoById(todo.id)
        val todos = todoService.getPagedTodos("pita", TodoPageServiceDTO(0, 10))
        assertThat(firstTodo.todo).isEqualTo("hi")
        assertThat(todos.responseList.size).isEqualTo(1)
        assertThat(singleCache(todo.id)).isNotNull()
        assertThat(listCache("pita", 0, 10)).isNotNull()

        authService.delete("pita", "pita")

        // 삭제 이후 조회시에 캐시 리턴안하고 없다고 즉시 표시
        val result = assertThrows<TodoException> { todoService.getTodoById(todo.id) }
        assertThat(result.errorCode).isEqualTo(TodoExceptionType.NOT_EXISTED_TODO.errorCode)
        assertThat(result.httpStatusCode).isEqualTo(TodoExceptionType.NOT_EXISTED_TODO.httpStatusCode)
        assertThat(result.message).isEqualTo(TodoExceptionType.NOT_EXISTED_TODO.message)

        // 캐시 증발 체크
        assertThat(listCache("pita", 0, 10)).isNull()
        assertThat(singleCache(todo.id)).isNull()
    }

    private fun listCache(userId: String, page: Int, size: Int) =
        cacheManager.getCache("todos")!!.get("$userId:page:$page:size:$size")   // 없으면 null

    private fun singleCache(todoId: Long) = cacheManager.getCache("todo")?.get(todoId)
}
