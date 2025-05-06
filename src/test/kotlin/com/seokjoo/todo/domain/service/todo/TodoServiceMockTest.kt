package com.seokjoo.todo.domain.service.todo

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.category.Category
import com.seokjoo.todo.domain.entity.todo.Todo
import com.seokjoo.todo.domain.entity.todocategory.TodoCategory
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import com.seokjoo.todo.domain.service.category.CategoryService
import com.seokjoo.todo.presentation.todo.dto.request.TodoPageRequest
import com.seokjoo.todo.presentation.todo.dto.request.toPageServiceDTO
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import org.springframework.data.repository.findByIdOrNull

class TodoServiceMockTest : BehaviorSpec({
    val todoRepository: TodoRepository = mockk()
    val categoryService: CategoryService = mockk()
    val user = User("pita", "pita")

    val todoService = TodoService(
        todoRepository = todoRepository,
        todoDeleteService = mockk(),
        categoryService = categoryService,
    )

    Given("create todo") {
        val request = TodoCreateServiceRequestDTO(
            todo = "abcde",
            isDone = false,
        )
        val requestTodo = Todo(id = 1L, todo = request.todo, isDone = request.isDone, owner = user)
        When("정상 케이스에서") {
            every { todoRepository.save(any()) } returns requestTodo
            val todo = todoService.createTodo(request, user)
            Then("create Todo 시에, todo 한개가 잘 생성된다") {
                todo.isDone shouldBe false
                todo.todo shouldBe "abcde"
            }
        }
    }

    Given("getTodos") {
        val id = 1L
        every { todoRepository.findByIdOrNull(id) } returns Todo(id = 1L, todo = "abcde", owner = user)
        When("정상 케이스에서") {
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
        val result = listOf(
            Todo(todo = "abcde", isDone = true, owner = user),
            Todo(todo = "abcdef", isDone = false, owner = user),
        )
        val pageRequest = PageRequest.of(0, 10)
        every { todoRepository.findAllSlicedTodoOrderByUpdatedAt(any(), any()) } returns SliceImpl(result, pageRequest, true)
        every { todoRepository.getFetchJoinedTodoList(any()) } returns result

        When("정상 케이스에서") {
            val todoPageRequest = TodoPageRequest()
            val allTodos = todoService.getPagedTodos(user.userId, todoPageRequest.toPageServiceDTO())
            Then("getAll 시에 추가한 만큼 잘 들어가있다.") {
                allTodos.responseList.count() shouldBe 2
            }
        }
    }

    Given("update test") {
        val id = 1L
        val request = TodoUpdateServiceRequestDTO(
            todo = "abcdef",
            isDone = false,
            categoryNames = listOf(),
            price = 10L,
        )
        val previousTodo = Todo(
            id = 1L,
            todo = "abcde",
            isDone = false,
            owner = user,
        )
        val nextTodo = Todo(
            id = 1L,
            todo = "abcdef",
            isDone = false,
            owner = user
        )
        When("업데이트 시에 없는 id를 주면") {
            every { todoRepository.findByIdOrNull(id) } throws TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)
            Then("에러가 난다") {
                val exception = shouldThrow<TodoException> {
                    todoService.updateTodo(
                        id = id,
                        request = TodoUpdateServiceRequestDTO(todo = "abas", isDone = null, price = null)
                    )
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
                updatedTodo.price shouldBe 10L
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
                categoryNames = listOf(
                    "horror",
                    "comedy",
                )
            )
            val newPreviousTodo = previousTodo.copy(todo = "abcde")
            val newNextTodo = nextTodo.copy(
                todo = "abcde", todoCategories = mutableListOf(
                    TodoCategory(
                        todo = Todo("abcde", isDone = false, owner = user, price = 10L),
                        Category(name = "horror")
                    ),
                    TodoCategory(
                        todo = Todo("abcde", isDone = false, owner = user, price = 10L),
                        Category(name = "comedy")
                    ),
                )
            )
            every { todoRepository.findByIdOrNull(1L) } returns newPreviousTodo
            every { todoRepository.save(any()) } returns newNextTodo
            every { categoryService.getOrCreateCategory(any()) } returnsMany listOf(
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
