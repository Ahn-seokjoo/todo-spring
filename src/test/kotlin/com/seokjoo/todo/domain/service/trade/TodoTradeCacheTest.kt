package com.seokjoo.todo.domain.service.trade

import com.seokjoo.todo.annotation.TodoTest
import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.charge.TodoChargeService
import com.seokjoo.todo.domain.service.todo.TodoCreateServiceRequestDTO
import com.seokjoo.todo.domain.service.todo.TodoPageServiceDTO
import com.seokjoo.todo.domain.service.todo.TodoService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager

@TodoTest
class TodoTradeCacheTest @Autowired constructor(
    private val authService: TodoAuthService,
    private val todoService: TodoService,
    private val chargeService: TodoChargeService,
    private val tradeService: TodoTradeService,
    @Qualifier("todoCacheManager") private val cacheManager: CacheManager,
) {
    private val sellerId = "trade-cache-seller"
    private val buyerId = "trade-cache-buyer"
    private val page = TodoPageServiceDTO(0, 10)

    @BeforeEach
    fun setUp() {
        clearCaches()
        cleanUpUsers()
        authService.signUp(sellerId, sellerId, "abc@nav.com")
        authService.signUp(buyerId, buyerId, "abc@nav.com")
        chargeService.charge(1000L, buyerId)
    }

    @AfterEach
    fun after() {
        try {
            cleanUpUsers()
        } finally {
            clearCaches()
        }
    }

    @Test
    fun `거래 이후 단건 캐시가 비워지고, 조회하면 새 owner 가 나온다`() {
        val seller = authService.findUserByUserId(sellerId)
        val todo = todoService.createTodo(TodoCreateServiceRequestDTO(todo = "trade", price = 500L), seller)

        // 캐시를 채워 둔다 (precondition)
        assertThat(todoService.getTodoById(todo.id).ownerId).isEqualTo(sellerId)
        assertThat(singleCache(todo.id)).isNotNull()

        tradeService.buyTodo(todo.id, buyerId)

        // 커밋 이후 evict 되었다
        assertThat(singleCache(todo.id)).isNull()
        // 다시 조회하면 새 owner
        assertThat(todoService.getTodoById(todo.id).ownerId).isEqualTo(buyerId)
    }

    @Test
    fun `거래 이후 판매자와 구매자의 리스트 캐시가 모두 비워진다`() {
        val seller = authService.findUserByUserId(sellerId)
        val buyer = authService.findUserByUserId(buyerId)
        val todo = todoService.createTodo(TodoCreateServiceRequestDTO(todo = "trade", price = 500L), seller)
        todoService.createTodo(TodoCreateServiceRequestDTO(todo = "buyer own"), buyer)

        // 두 사람의 리스트 캐시를 채워 둔다 (precondition)
        assertThat(todoService.getPagedTodos(sellerId, page).responseList).hasSize(1)
        assertThat(todoService.getPagedTodos(buyerId, page).responseList).hasSize(1)
        assertThat(listCache(sellerId, 0, 10)).isNotNull()
        assertThat(listCache(buyerId, 0, 10)).isNotNull()

        tradeService.buyTodo(todo.id, buyerId)

        assertThat(listCache(sellerId, 0, 10)).isNull()
        assertThat(listCache(buyerId, 0, 10)).isNull()
        // 다시 조회하면 최신 상태: 판매자는 비고, 구매자는 2개
        assertThat(todoService.getPagedTodos(sellerId, page).responseList).isEmpty()
        assertThat(todoService.getPagedTodos(buyerId, page).responseList).hasSize(2)
    }

    private fun cleanUpUsers() {
        listOf(sellerId, buyerId).forEach { id ->
            try {
                authService.delete(id, id)
            } catch (e: TodoException) {
                // 이미 없는 유저만 무시한다
                if (e.errorCode != TodoExceptionType.AUTH_USER_NOT_EXIST.errorCode) throw e
            }
        }
    }

    private fun clearCaches() {
        cacheManager.getCache("todos")?.clear()
        cacheManager.getCache("todo")?.clear()
    }

    private fun listCache(userId: String, page: Int, size: Int) =
        cacheManager.getCache("todos")!!.get("$userId:page:$page:size:$size")   // 없으면 null

    private fun singleCache(todoId: Long) = cacheManager.getCache("todo")?.get(todoId)
}
