package com.seokjoo.todo.domain.service.category.listener

import com.seokjoo.todo.domain.repository.category.CategoryRepository
import com.seokjoo.todo.domain.service.outbox.annotation.TodoTransactionalEventListener
import com.seokjoo.todo.domain.service.remove.event.TodoDeletedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class CategoryDeletedListener(
    private val categoryRepository: CategoryRepository,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TodoTransactionalEventListener
    fun eventListener(event: TodoDeletedEvent) {
        if (event.categoryIds.isEmpty()) return
        categoryRepository.deleteCategoryByIdsIfNotExistInTodoCategory(ids = event.categoryIds)
    }
}
