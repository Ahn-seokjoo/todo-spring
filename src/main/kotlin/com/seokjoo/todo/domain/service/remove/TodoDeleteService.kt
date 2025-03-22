package com.seokjoo.todo.domain.service.remove

import com.seokjoo.todo.domain.entity.todo.Todo
import com.seokjoo.todo.domain.entity.todocategory.TodoCategory
import com.seokjoo.todo.domain.repository.category.CategoryRepository
import com.seokjoo.todo.domain.repository.categorytodo.TodoCategoryRepository
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TodoDeleteService(
    private val todoRepository: TodoRepository,
    private val categoryRepository: CategoryRepository,
    private val todoCategoryRepository: TodoCategoryRepository,
) {

    /**
    1. 아래 로직은, 더이상 참조되지 않은 Category를 지워주기 위함
    2. 삭제한 투두에서 사용중이던 카테고리 목록들을 돌면서,
    3. null 아니고,
    4. 더이상 링킹 되지않은걸 지운다.
    5. todo 를 제거하기 전에 category 먼저 제거한다
     */
    @Transactional
    fun deleteTodo(todo: Todo) {
        todo.todoCategories
            .filter { it.category != null && isCategoryNotUsed(it) }
            .mapNotNull { it.category }
            .forEach { categoryRepository.delete(it) }

        todoRepository.deleteById(todo.id)
    }

    /**
     * 조회 이후에 delete 하기 때문에 count 1로 변경 (자기 자신)
     */
    private fun isCategoryNotUsed(it: TodoCategory): Boolean {
        return it.category?.let { category ->
            todoCategoryRepository.countByCategoryId(categoryId = category.id) == 1
        } ?: false
    }
}
