package com.seokjoo.todo.domain.service.remove

import com.seokjoo.todo.annotation.TodoTest
import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.category.Category
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.repository.category.CategoryRepository
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import com.seokjoo.todo.domain.repository.todouser.TodoAuthRepository
import com.seokjoo.todo.domain.service.remove.event.TodoDeletedEvent
import com.seokjoo.todo.domain.service.todo.TodoCreateServiceRequestDTO
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents
import org.springframework.transaction.annotation.Transactional

@TodoTest
@Transactional
@RecordApplicationEvents
class TodoRemoveServiceTest @Autowired constructor(
    private val todoService: TodoService,
    private val todoDeleteService: TodoDeleteService,
    private val todoRepository: TodoRepository,
    private val categoryRepository: CategoryRepository,
    private val userRepository: TodoAuthRepository,
) {
    private lateinit var user: User
    private lateinit var todo: TodoServiceResponseDTO

    @BeforeEach
    fun before() {
        // category id 와 todo_category id 가 우연히 같은 숫자가 되지 않도록 카테고리를 하나 먼저 만들어 둔다
        categoryRepository.save(Category(name = "dummy"))

        user = User("pita", "pita")
        userRepository.save(user)
        val request = TodoCreateServiceRequestDTO(todo = "spring", categoryNames = listOf("drama", "action"))
        todo = todoService.createTodo(request = request, owner = user)
    }

    @Test
    fun `deleteTodo 하면 투두가 삭제되고, 카테고리 id 를 담은 TodoDeletedEvent 가 발행된다`(applicationEvents: ApplicationEvents) {
        // given: 기대값은 서비스 코드와 다른 경로(DB 조회)로 구한다
        val dramaId = categoryRepository.findCategoryByName("drama")!!.id
        val actionId = categoryRepository.findCategoryByName("action")!!.id
        val target = todoRepository.findByIdOrNull(todo.id) ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)

        // when
        todoDeleteService.deleteTodo(target)

        // then
        assertThat(todoRepository.findById(target.id)).isEmpty()

        val events = applicationEvents.stream(TodoDeletedEvent::class.java).toList()
        assertThat(events).hasSize(1)
        assertThat(events[0].categoryIds).containsExactlyInAnyOrder(dramaId, actionId)
    }

    @Test
    fun `카테고리가 없는 투두를 지워도 이벤트는 발행되고 categoryIds 는 비어 있다`(applicationEvents: ApplicationEvents) {
        // given
        val noCategoryTodo = todoService.createTodo(
            request = TodoCreateServiceRequestDTO(todo = "no category"),
            owner = user,
        )
        val target =
            todoRepository.findByIdOrNull(noCategoryTodo.id) ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)

        // when
        todoDeleteService.deleteTodo(target)

        // then
        assertThat(todoRepository.findById(target.id)).isEmpty()

        val events = applicationEvents.stream(TodoDeletedEvent::class.java).toList()
        assertThat(events).hasSize(1)
        assertThat(events[0].categoryIds).isEmpty()
    }
}
