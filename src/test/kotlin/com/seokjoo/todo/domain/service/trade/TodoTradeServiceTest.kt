package com.seokjoo.todo.domain.service.trade

import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.charge.TodoChargeService
import com.seokjoo.todo.domain.service.todo.TodoCreateServiceRequestDTO
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
class TodoTradeServiceTest @Autowired constructor(
    private val todoService: TodoService,
    private val todoChargeService: TodoChargeService,
    private val todoAuthService: TodoAuthService,
    private val todoTradeService: TodoTradeService,
) {
    lateinit var todo: TodoServiceResponseDTO

    @Test
    @Transactional
    fun `todo를 거래했을 때, 금액이 잘 차감되고 owner 가 자신으로 잘 변경된다`() {
        val user2 = todoAuthService.findUserByUserId("pita2")
        todoTradeService.buyTodo(todo.id, user2)

        val todo = todoService.getTodoById(todo.id)
        assert(todo.todo == "테스트 500원 짜리 투두")
        assert(todo.price == 500L)
        assert(todo.ownerId == user2.userId)
        assert(user2.money.currentBalance() == 500L)

        val user1 = todoAuthService.findUserByUserId("pita1")
        assert(user1.money.currentBalance() == 500L)
    }

    @BeforeEach
    fun before() {
        val user1 = User("pita1", "pita1")
        val user2 = User("pita2", "pita2")
        todoAuthService.signUp(user1.userId, user1.password)
        todoAuthService.signUp(user2.userId, user2.password)

        // 유저2 1000원 충전
        val token = todoAuthService.login(user2.userId, user2.password)
        todoChargeService.charge(1000L, token.refreshToken)

        // 유저 1 todo 500원짜리로 한개 생성
        val user = todoAuthService.findUserByUserId("pita1")
        val request = TodoCreateServiceRequestDTO(
            todo = "테스트 500원 짜리 투두",
            price = 500L
        )
        todo = todoService.createTodo(request, user)
    }

    @AfterEach
    fun after() {
        todoService.deleteTodo(todo.id, "pita2")
        todoAuthService.delete("pita1", "pita1")
        todoAuthService.delete("pita2", "pita2")
    }
}
