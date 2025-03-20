package com.seokjoo.todo.domain.service.todo

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.category.Category
import com.seokjoo.todo.domain.entity.todo.Todo
import com.seokjoo.todo.domain.entity.todocategory.TodoCategory
import com.seokjoo.todo.domain.repository.category.CategoryRepository
import com.seokjoo.todo.domain.repository.categorytodo.TodoCategoryRepository
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TodoService(
    private val todoRepository: TodoRepository,
    private val categoryRepository: CategoryRepository,
    private val todoCategoryRepository: TodoCategoryRepository,
) {
    fun getAllTodos(): List<TodoServiceResponseDTO> {
        val todoList = todoRepository.findAllWithCategories()
        return todoList.map { todo -> TodoServiceResponseDTO.from(todo) }
    }

    fun getTodoById(id: Long): TodoServiceResponseDTO {
        val result = todoRepository.findByIdOrNull(id) ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)
        return TodoServiceResponseDTO.from(result)
    }

    @Transactional
    fun createTodo(request: TodoServiceRequestDTO): TodoServiceResponseDTO {
        // 1. 저장하여 영속화 먼저
        val todo = Todo(todo = request.todo, isDone = request.isDone)
        todoRepository.save(todo)

        checkExistAndAddCategory(request, todo)
        return TodoServiceResponseDTO.from(todo)
    }

    @Transactional
    fun updateTodo(id: Long, request: TodoServiceRequestDTO): TodoServiceResponseDTO {
        val todo = todoRepository.findByIdOrNull(id) ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)

        todo.apply {
            this.todo = request.todo
            this.isDone = request.isDone
        }
        // 더티 체킹으로 save 할 필요 없지만 그냥 명시적으로 해줌
        todoRepository.save(todo)

        checkExistAndAddCategory(request, todo)
        return TodoServiceResponseDTO.from(todo)
    }

    @Transactional
    fun deleteTodo(id: Long) {
        val todo = todoRepository.findByIdOrNull(id) ?: throw TodoException.of(TodoExceptionType.NOT_EXISTED_TODO)

        /**
        1. 아래 로직은, 더이상 참조되지 않은 Category를 지워주기 위함
        2. 삭제한 투두에서 사용중이던 카테고리 목록들을 돌면서,
        3. null 아니고,
        4. 더이상 링킹 되지않은걸 지운다.
        5. todo 를 제거하기 전에 category 먼저 제거한다
         */

        todo.todoCategories
            .filter { it.category != null && isCategoryNotUsed(it) }
            .mapNotNull { it.category }
            .forEach { categoryRepository.delete(it) }

        todoRepository.deleteById(id)
    }

    /**
     * 조회 이후에 delete 하기 때문에 count 1로 변경 (자기 자신)
     */
    private fun isCategoryNotUsed(it: TodoCategory): Boolean {
        return it.category?.let { category ->
            todoCategoryRepository.countByCategoryId(categoryId = category.id) == 1
        } ?: false
    }

    private fun checkExistAndAddCategory(
        request: TodoServiceRequestDTO,
        todo: Todo,
    ) {
        if (request.categoryNames.isNotEmpty()) {
            request.categoryNames.forEach { categoryName ->
                if (todo.hasCategory(categoryName).not()) {
                    val matchedCategory =
                        categoryRepository.findCategoryByName(categoryName)
                            ?: categoryRepository.save(Category(name = categoryName))
                    todo.addCategory(category = matchedCategory)
                }
            }
        }
    }
}
