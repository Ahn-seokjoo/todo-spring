package com.seokjoo.todo.domain.service.todo

import com.seokjoo.todo.annotation.TodoTest
import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration

@TodoTest
class TodoCacheTest @Autowired constructor(
    private val authService: TodoAuthService,
    private val todoService: TodoService,
    @Qualifier("todoCacheManager") private val cacheManager: CacheManager,
    private val transactionTemplate: TransactionTemplate,
    private val stringRedisTemplate: StringRedisTemplate,
) {
    @BeforeEach
    fun setUp() {
        cacheManager.getCache("todos")?.clear()
        cacheManager.getCache("todo")?.clear()
        authService.signUp("pita", "pita", "")
    }

    // 테스트 안에서 추가로 가입시킨 사용자 (pita 외)
    private val extraUserIds = mutableListOf<String>()

    @AfterEach
    fun after() {
        try {
            (listOf("pita") + extraUserIds).forEach { userId ->
                try {
                    authService.delete(userId, userId)
                } catch (e: TodoException) {
                    // 탈퇴 테스트처럼 이미 지워진 경우만 무시한다
                    if (e.errorCode != TodoExceptionType.AUTH_USER_NOT_EXIST.errorCode) throw e
                }
            }
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

        assertThat(listCache("pita", 0, 10)).isNotNull()   // 채워졌다
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

        assertThat(listCache("pita", 0, 10)).isNotNull()
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

    @Test
    fun `롤백되면 AFTER_COMMIT 리스너가 실행되지 않아 캐시가 유지된다`() {
        val user = authService.findUserByUserId("pita")
        val todo = todoService.createTodo(request = TodoCreateServiceRequestDTO(todo = "hi"), owner = user)
        todoService.getTodoById(todo.id)
        todoService.getPagedTodos("pita", TodoPageServiceDTO(0, 10))
        assertThat(singleCache(todo.id)).isNotNull()
        assertThat(listCache("pita", 0, 10)).isNotNull()

        // 바깥 트랜잭션 안에서 수정하고 롤백한다. updateTodo 의 @Transactional 은 이 트랜잭션에 합류한다.
        transactionTemplate.execute { status ->
            todoService.updateTodo(todo.id, user.userId, TodoUpdateServiceRequestDTO(todo = "bye", null, null))
            status.setRollbackOnly()
        }

        // 롤백이라 evict 되지 않았고, DB 도 그대로다
        assertThat(singleCache(todo.id)).isNotNull()
        assertThat(listCache("pita", 0, 10)).isNotNull()
        assertThat(todoService.getTodoById(todo.id).todo).isEqualTo("hi")
    }

    @Test
    fun `한 사용자의 할 일이 바뀌어도 다른 사용자의 목록 캐시는 남는다`() {
        val pita = authService.findUserByUserId("pita")
        val kim = signUpExtraUser("cache-kim")
        val pitaTodo = todoService.createTodo(request = TodoCreateServiceRequestDTO(todo = "pita todo"), owner = pita)
        todoService.createTodo(request = TodoCreateServiceRequestDTO(todo = "kim todo"), owner = kim)

        // 두 사람의 목록 캐시를 채워 둔다 (빈 목록은 캐시되지 않으므로 할 일을 하나씩 만들어 둠)
        todoService.getPagedTodos("pita", TodoPageServiceDTO(0, 10))
        todoService.getPagedTodos(kim.userId, TodoPageServiceDTO(0, 10))
        assertThat(listCache("pita", 0, 10)).isNotNull()
        assertThat(listCache(kim.userId, 0, 10)).isNotNull()

        todoService.updateTodo(pitaTodo.id, "pita", TodoUpdateServiceRequestDTO(todo = "bye", null, null))

        assertThat(listCache("pita", 0, 10)).isNull()          // 바뀐 사람만 지워지고
        assertThat(listCache(kim.userId, 0, 10)).isNotNull()   // 다른 사람은 남는다
    }

    // 이스케이프하지 않으면 아래 아이디들은 모두 SCAN 패턴에서 "pita"와 매칭된다
    // pi* (* = 아무 글자 여러 개), p?ta (? = 아무 글자 하나), [p]ita ([] = 괄호 안 글자 중 하나), pi\ta (\ = 다음 글자를 그대로)
    @ParameterizedTest
    @ValueSource(strings = ["pi*", "p?ta", "[p]ita", "pi\\ta"])
    fun `아이디에 글롭 문자가 있어도 패턴이 다른 사용자의 목록 캐시를 지우지 않는다`(globUserId: String) {
        val pita = authService.findUserByUserId("pita")
        val globUser = signUpExtraUser(globUserId)
        todoService.createTodo(request = TodoCreateServiceRequestDTO(todo = "pita todo"), owner = pita)
        val globTodo = todoService.createTodo(request = TodoCreateServiceRequestDTO(todo = "glob todo"), owner = globUser)

        todoService.getPagedTodos("pita", TodoPageServiceDTO(0, 10))
        todoService.getPagedTodos(globUserId, TodoPageServiceDTO(0, 10))
        assertThat(listCache("pita", 0, 10)).isNotNull()
        assertThat(listCache(globUserId, 0, 10)).isNotNull()

        todoService.updateTodo(globTodo.id, globUserId, TodoUpdateServiceRequestDTO(todo = "bye", null, null))

        assertThat(listCache(globUserId, 0, 10)).isNull()      // 본인 캐시는 지워지고
        assertThat(listCache("pita", 0, 10)).isNotNull()       // pita 캐시는 남는다
    }

    @Test
    fun `한 사용자의 목록 캐시 키가 100개를 넘어도 남김없이 지워진다`() {
        val pita = authService.findUserByUserId("pita")
        val pitaTodo = todoService.createTodo(request = TodoCreateServiceRequestDTO(todo = "pita todo"), owner = pita)

        // pageSize만 바꿔 가며 조회해 키가 많이 생긴 상황을 흉내 낸다
        // 250개 = 100개 배치 2번 + 마지막에 남는 50개 (나머지 처리까지 검증)
        (1..250).forEach { size ->
            stringRedisTemplate.opsForValue().set("todos::pita:page:0:size:$size", "dummy", Duration.ofMinutes(1))
        }
        val otherUserKey = "todos::cache-kim:page:0:size:5"
        stringRedisTemplate.opsForValue().set(otherUserKey, "dummy", Duration.ofMinutes(1))
        assertThat(scanKeys("todos::pita:*")).hasSize(250)

        todoService.updateTodo(pitaTodo.id, "pita", TodoUpdateServiceRequestDTO(todo = "bye", null, null))

        assertThat(scanKeys("todos::pita:*")).isEmpty()              // 250개 모두 지워지고
        assertThat(stringRedisTemplate.hasKey(otherUserKey)).isTrue() // 다른 사용자 키는 남는다
    }

    private fun scanKeys(pattern: String): Set<String> =
        stringRedisTemplate.scan(ScanOptions.scanOptions().match(pattern).count(1000).build())
            .use { cursor -> cursor.asSequence().toSet() }

    private fun signUpExtraUser(userId: String): User {
        authService.signUp(userId, userId, "")
        extraUserIds += userId
        return authService.findUserByUserId(userId)
    }

    private fun listCache(userId: String, page: Int, size: Int) =
        cacheManager.getCache("todos")!!.get("$userId:page:$page:size:$size")   // 없으면 null

    private fun singleCache(todoId: Long) = cacheManager.getCache("todo")?.get(todoId)
}
