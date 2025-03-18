package com.seokjoo.todo.domain.service.todo

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
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
        val todoCategory = todo.todoCategories
        todoRepository.deleteById(id)

        /**
        1. 아래 로직은, 더이상 참조되지 않은 Category를 지워주기 위함
        2. 삭제한 투두에서 사용중이던 카테고리 목록들을 돌면서,
        3. null 아니고,
        4. 더이상 링킹 되지않은걸 지운다.
         */

        todoCategory
            .filter { it.category != null && isCategoryNotUsed(it) }
            .mapNotNull { it.category }
            .forEach { categoryRepository.delete(it) }
    }

    private fun isCategoryNotUsed(it: TodoCategory): Boolean {
        return it.category?.let { category ->
            todoCategoryRepository.countByCategoryId(categoryId = category.id) == 0
        } ?: false
    }

    private fun checkExistAndAddCategory(
        request: TodoServiceRequestDTO,
        todo: Todo,
    ) {
        if (request.categories.isNotEmpty()) {
            request.categories.forEach { category ->
                val matchedCategory =
                    categoryRepository.findCategoryByName(category.name) ?: categoryRepository.save(category)
                val isAlreadyNotExists = todo.todoCategories.any { it.category?.name == category.name }.not()
                if (isAlreadyNotExists) todo.addCategory(category = matchedCategory)
            }
        }
    }
}
