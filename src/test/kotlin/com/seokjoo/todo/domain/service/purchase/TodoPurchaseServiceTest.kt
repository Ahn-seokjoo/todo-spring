package com.seokjoo.todo.domain.service.purchase

import com.seokjoo.todo.annotation.TodoTest
import com.seokjoo.todo.domain.entity.purchase.PurchaseStatus
import com.seokjoo.todo.domain.entity.todo.TodoStatus
import com.seokjoo.todo.domain.repository.purchase.TodoPurchaseRepository
import com.seokjoo.todo.domain.repository.todouser.TodoAuthRepository
import com.seokjoo.todo.domain.service.auth.TodoAuthService
import com.seokjoo.todo.domain.service.balance.TodoBalanceService
import com.seokjoo.todo.domain.service.charge.TodoChargeService
import com.seokjoo.todo.domain.service.todo.TodoCreateServiceRequestDTO
import com.seokjoo.todo.domain.service.todo.TodoPageServiceDTO
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@TodoTest
class TodoPurchaseServiceTest @Autowired constructor(
    private val todoService: TodoService,
    private val todoAuthService: TodoAuthService,
    private val todoAuthRepository: TodoAuthRepository,
    private val todoChargeService: TodoChargeService,
    private val userBalanceService: TodoBalanceService,
    private val purchaseTxService: TodoPurchaseTxService,
    private val purchaseRepository: TodoPurchaseRepository,
) {
    lateinit var todo: TodoServiceResponseDTO

    @Test
    fun `생성 즉시 Todo는 AVAILABLE 이다`() {
        assertThat(todo.status).isEqualTo(TodoStatus.AVAILABLE)
    }

    @Test
    fun `구매 요청을 하면 todo 가 PENDING_APPROVAL 로 변경되고 구매자의 금액은 선 차감된다`() {
        purchaseTxService.purchaseTodo(todo.id, "pita2")

        val result = todoService.getPagedTodos(userId = "pita1", TodoPageServiceDTO(0, 10)).responseList.first()
        assertThat(result.status).isEqualTo(TodoStatus.PENDING_APPROVAL)

        val balance = userBalanceService.getBalance("pita2")
        // 1000원 충전하고 100원짜리 구매대기
        assertThat(balance).isEqualTo(900)
        // 구매 요청 이후 상태 조회
        val purchase = purchaseRepository.findByTodoIdAndPurchaseStatus(todo.id, PurchaseStatus.PENDING)
        assertThat(purchase?.purchaseStatus).isEqualTo(PurchaseStatus.PENDING)

        // PENDING_APPROVAL 시에 삭제가 안되어 상태를 다시 approval로 돌려놓음
        purchaseTxService.cancelPurchaseTodo(todo.id, "pita2")
    }

    @Test
    fun `구매 요청 뒤에 구매자 마음이 바뀌어 cancel 한다`() {
        // given - 구매 요청
        purchaseTxService.purchaseTodo(todo.id, "pita2")

        // when - cancel
        purchaseTxService.cancelPurchaseTodo(todo.id, "pita2")

        // 구매 요청 이후 상태 조회
        val purchase = purchaseRepository.findByTodoIdAndPurchaseStatus(todo.id, PurchaseStatus.CANCELLED)
        assertThat(purchase?.purchaseStatus).isEqualTo(PurchaseStatus.CANCELLED)

        // then - 다시 1000원 복귀
        val balance = userBalanceService.getBalance("pita2")
        assertThat(balance).isEqualTo(1000)
    }

    @Test
    fun `구매 요청 뒤에 판매자 마음이 바뀌어 reject 한다`() {
        // given - 구매 요청
        purchaseTxService.purchaseTodo(todo.id, "pita2")

        // when - cancel
        purchaseTxService.rejectPurchaseTodo(todo.id, "pita1")

        // then - 다시 1000원 복귀
        val balance = userBalanceService.getBalance("pita2")
        assertThat(balance).isEqualTo(1000)
        // 구매 요청 이후 상태 조회
        val purchase = purchaseRepository.findByTodoIdAndPurchaseStatus(todo.id, PurchaseStatus.REJECTED)
        assertThat(purchase?.purchaseStatus).isEqualTo(PurchaseStatus.REJECTED)
    }

    @Test
    fun `구매 요청을 approve 하면 판매자 금액 증가, owner 변경, 구매자는 차감된 채로 유지된다`() {
        purchaseTxService.purchaseTodo(todo.id, "pita2")

        val buyer = userBalanceService.getBalance("pita2")
        val seller = userBalanceService.getBalance("pita1")

        // 1000원 충전하고 100원짜리 구매대기
        assertThat(buyer).isEqualTo(900)
        // seller 는 아직 0원
        assertThat(seller).isEqualTo(0)

        purchaseTxService.approvePurchaseTodo(todo.id, "pita1")

        val buyer2 = userBalanceService.getBalance("pita2")
        val seller2 = userBalanceService.getBalance("pita1")

        // approve 이후 900원 그대로
        assertThat(buyer2).isEqualTo(900)
        // seller 는 이제 100원
        assertThat(seller2).isEqualTo(100)

        val updatedTodo = todoService.getTodoById(todo.id)
        // todo owner 변경된 것 확인
        assertThat(updatedTodo.ownerId).isEqualTo("pita2")
        // 다시 available
        assertThat(updatedTodo.status).isEqualTo(TodoStatus.AVAILABLE)

        // 구매 요청 이후 상태 조회
        val purchase = purchaseRepository.findByTodoIdAndPurchaseStatus(todo.id, PurchaseStatus.APPROVED)
        assertThat(purchase?.purchaseStatus).isEqualTo(PurchaseStatus.APPROVED)
    }

    @BeforeEach
    fun before() {
        todoAuthService.signUp("pita1", "pita", email = "abc@nav.com")
        todoAuthService.signUp("pita2", "pita", email = "abc@nav.com")

        val user1 = todoAuthService.findUserByUserId("pita1")
        val user2 = todoAuthService.findUserByUserId("pita2")
        todoChargeService.charge(1000L, user2.userId)

        val request = TodoCreateServiceRequestDTO("spring", price = 100L)
        todo = todoService.createTodo(request, user1)
    }

    @AfterEach
    fun after() {
        val owner = todoService.getTodoById(todo.id).ownerId
        todoService.deleteTodo(todo.id, owner)
        todoAuthRepository.deleteAll()
    }
}
