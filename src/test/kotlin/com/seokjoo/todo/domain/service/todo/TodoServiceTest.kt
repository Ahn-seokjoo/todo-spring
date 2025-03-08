package com.seokjoo.todo.domain.service.todo

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.category.Category
import com.seokjoo.todo.domain.entity.todo.Todo
import com.seokjoo.todo.domain.entity.todocategory.TodoCategory
import com.seokjoo.todo.domain.repository.category.CategoryRepository
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class TodoServiceTest : BehaviorSpec({
    val todoRepository: TodoRepository = mockk()
    val categoryRepository: CategoryRepository = mockk()

    val todoService = TodoService(
        todoRepository = todoRepository,
        categoryRepository = categoryRepository,
        todoCategoryRepository = mockk(),
    )

    Given("create todo") {
        val request = TodoServiceRequestDTO(
            todo = "abcde",
            isDone = false,
        )
        val requestTodo = Todo(id = 1L, todo = request.todo, isDone = request.isDone)
        When("when") {
            every { todoRepository.save(any()) } returns requestTodo
            val todo = todoService.createTodo(request)
            Then("create Todo 시에, todo 한개가 잘 생성된다") {
                todo.isDone shouldBe false
                todo.todo shouldBe "abcde"
            }
        }
    }

    Given("getTodos") {
        val id = 1L
        every { todoRepository.findByIdOrNull(id) } returns Todo(id = 1L, todo = "abcde")
        When("when") {
            val todo = todoService.getTodoById(id)
            Then("getTodos 를 수행했을 때, todo 한개가 잘 나온다") {
                todo.isDone shouldBe false
                todo.todo shouldBe "abcde"
                todo.id shouldBe 1L
            }
        }
        When("exception") {
            every { todoRepository.findByIdOrNull(id) } throws TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)
            Then("에러 케이스의 경우 404, T000_TODO_ERROR를 준다") {
                val exception = shouldThrow<TodoException> {
                    todoService.getTodoById(id = id)
                }
                exception.message shouldBe "존재하지 않는 Todo 입니다"
                exception.httpStatusCode shouldBe 404
                exception.errorCode shouldBe "T000_TODO_ERROR"
            }
        }
    }

    Given("getAll todo") {
        every { todoRepository.findAll() } returns listOf(
            Todo(todo = "abcde", isDone = true),
            Todo(todo = "abcdef", isDone = false),
        )
        When("when") {
            val allTodos = todoService.getAllTodos()
            Then("getAll 시에 추가한 만큼 잘 들어가있다.") {
                allTodos.count() shouldBe 2
            }
        }
    }

    Given("update test") {
        val id = 1L
        val request = TodoServiceRequestDTO(
            todo = "abcdef",
            isDone = false,
            categories = listOf()
        )
        val previousTodo = Todo(
            id = 1L,
            todo = "abcde",
            isDone = false,
        )
        val nextTodo = Todo(
            id = 1L,
            todo = "abcdef",
            isDone = false,
        )
        When("업데이트 시에 없는 id를 주면") {
            every { todoRepository.findByIdOrNull(id) } throws TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)
            Then("에러가 난다") {
                val exception = shouldThrow<TodoException> {
                    todoService.updateTodo(id = id, request = TodoServiceRequestDTO(todo = "abas"))
                }
                exception.message shouldBe "존재하지 않는 Todo 입니다"
                exception.httpStatusCode shouldBe 404
                exception.errorCode shouldBe "T000_TODO_ERROR"
            }
        }

        When("todo를 바꿔서 주는 경우") {
            every { todoRepository.findByIdOrNull(id) } returns previousTodo
            every { todoRepository.save(any()) } returns nextTodo

            val updatedTodo = todoService.updateTodo(id = id, request = request)
            Then("업데이트가 잘 된다") {
                updatedTodo.todo shouldBe "abcdef"
                updatedTodo.isDone shouldBe false
                updatedTodo.id shouldNotBe null
            }
        }

        When("isDone을 바꿔서 주는 경우") {
            val newRequest = request.copy(isDone = true)
            val newPreviousTodo = previousTodo.copy()
            val newNextTodo = nextTodo.copy(isDone = true)
            every { todoRepository.findByIdOrNull(id) } returns newPreviousTodo
            every { todoRepository.save(any()) } returns newNextTodo
            val updatedTodo = todoService.updateTodo(id = id, request = newRequest)

            Then("업데이트가 잘 된다.") {
                updatedTodo.todo shouldBe "abcdef"
                updatedTodo.isDone shouldBe true
                updatedTodo.id shouldNotBe null
            }
        }

        When("category를 바꿔서 주는 경우") {
            val newRequest = request.copy(
                categories = listOf(
                    Category(name = "horror"),
                    Category(name = "comedy"),
                )
            )
            val newPreviousTodo = previousTodo.copy(todo = "abcde")
            val newNextTodo = nextTodo.copy(
                todo = "abcde", todoCategories = mutableListOf(
                    TodoCategory(todo = Todo("abcde", isDone = false), Category(name = "horror")),
                    TodoCategory(todo = Todo("abcde", isDone = false), Category(name = "comedy")),
                )
            )
            every { todoRepository.findByIdOrNull(1L) } returns newPreviousTodo
            every { todoRepository.save(any()) } returns newNextTodo
            every { categoryRepository.findByName(any()) } returnsMany listOf(
                Category(name = "horror"),
                Category(name = "comedy")
            )
            val updatedTodo = todoService.updateTodo(id = id, request = newRequest)
            Then("업데이트가 잘 된다") {
                updatedTodo.todo shouldBe "abcdef"
                updatedTodo.isDone shouldBe false
                updatedTodo.id shouldNotBe null
                updatedTodo.categories.size shouldBe 2
                updatedTodo.categories[0] shouldBe "horror"
                updatedTodo.categories[1] shouldBe "comedy"
            }
        }
    }

    Given("delete test") {
        val id = 1L
        val expectedStatusCode = 404
        When("delete when") {
            every { todoRepository.findByIdOrNull(id) } throws TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)
            Then("없는 ID를 주는 경우 ") {
                val exception = shouldThrow<TodoException> {
                    todoService.deleteTodo(id)
                }
                exception.message shouldBe "존재하지 않는 Todo 입니다"
                exception.httpStatusCode shouldBe expectedStatusCode
                exception.errorCode shouldBe "T000_TODO_ERROR"
            }
        }
    }
})
