// package com.seokjoo.todo
//
// import com.seokjoo.todo.domain.repository.category.CategoryRepository
// import com.seokjoo.todo.domain.repository.categorytodo.TodoCategoryRepository
// import com.seokjoo.todo.domain.repository.todo.TodoRepository
// import com.seokjoo.todo.domain.service.auth.TodoAuthService
// import com.seokjoo.todo.domain.service.todo.TodoService
// import com.seokjoo.todo.presentation.category.dto.CategoryDTO
// import com.seokjoo.todo.presentation.todo.dto.request.TodoRequest
// import com.seokjoo.todo.presentation.todo.dto.request.toCreateRequest
// import org.junit.jupiter.api.AfterEach
// import org.junit.jupiter.api.BeforeEach
// import org.junit.jupiter.api.Test
// import org.springframework.beans.factory.annotation.Autowired
//
// @IntegrationTest
// class CategoryConcurrencyTest @Autowired constructor(
//     private val todoService: TodoService,
//     private val todoRepository: TodoRepository,
//     private val categoryRepository: CategoryRepository,
//     private val todoCategoryRepository: TodoCategoryRepository,
//     private val todoAuthService: TodoAuthService,
// ) {
//     @BeforeEach
//     fun before() {
//         todoAuthService.signUp("pita", "pita")
//         categoryRepository.deleteAll()
//         todoCategoryRepository.deleteAll()
//         todoRepository.deleteAll()
//     }
//
//     @AfterEach
//     fun afterEach() {
//         categoryRepository.deleteAll()
//         todoCategoryRepository.deleteAll()
//         todoRepository.deleteAll()
//     }
//
//     @Test
//     fun `동시에 같은 카테고리를 추가해도 중복으로 생성되지 않아야 한다 - Thread 버전`() {
//         val categoryName = listOf(CategoryDTO("Spring"))
//         val token = todoAuthService.login("pita", "pita")
//         val user = todoAuthService.findUser(token.refreshToken)
//         val request = TodoRequest(
//             todo = "Spring 동시성 이슈 실제로 보기",
//             isDone = false,
//             categories = categoryName,
//         )
//
//         val threads = mutableListOf<Thread>()
//
//         repeat(5) {
//             val thread = Thread {
//                 todoService.createTodo(request.toCreateRequest(), user)
//             }
//             threads.add(thread)
//         }
//
//         // Start all threads
//         threads.forEach { it.start() }
//
//         // Wait for all threads to finish
//         threads.forEach { it.join() }
//
//         val allCategories = categoryRepository.findAll()
//         println("최종 카테고리 개수: ${allCategories.size}, allCategories = ${allCategories.map { it.name }}")
//         assert(allCategories.size == 1)
//     }
// }
