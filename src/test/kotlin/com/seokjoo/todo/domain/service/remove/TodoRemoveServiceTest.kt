package com.seokjoo.todo.domain.service.remove

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.repository.category.CategoryRepository
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import com.seokjoo.todo.domain.service.todo.TodoService
import com.seokjoo.todo.domain.service.todo.TodoServiceRequestDTO
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class TodoRemoveServiceTest @Autowired constructor(
    private val todoService: TodoService,
    private val todoDeleteService: TodoDeleteService,
    private val todoRepository: TodoRepository,
    private val categoryRepository: CategoryRepository,
) {
    private lateinit var todo: TodoServiceResponseDTO

    @BeforeEach
    fun before() {
        val request = TodoServiceRequestDTO(todo = "spring", categoryNames = listOf("drama", "action"))
        todo = todoService.createTodo(request)
    }

    @Test
    fun `deleteTodo 했을 때, 잘제거 되는지 테스트`() {
        //given
        val todo = todoRepository.findByIdOrNull(todo.id)
            ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)
        // 잘 추가 됐는지?
        assert(todoRepository.findAll().count() == 1)

        // when
        todoDeleteService.deleteTodo(todo)

        // then
        val todoResult = todoRepository.findById(todo.id)
        assert(todoResult.isPresent.not())

        val categoryDramaRemoveResult = categoryRepository.findCategoryByName("drama")
        assert(categoryDramaRemoveResult == null)

        val categoryActionRemoveResult = categoryRepository.findCategoryByName("action")
        assert(categoryActionRemoveResult == null)
    }
}
