package com.seokjoo.todo.domain.service.remove

import com.seokjoo.todo.domain.entity.todo.Todo
import com.seokjoo.todo.domain.repository.todo.TodoRepository
import com.seokjoo.todo.domain.service.remove.event.TodoDeletedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TodoDeleteService(
    private val todoRepository: TodoRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {

    @Transactional
    fun deleteTodo(todo: Todo) {
        val categoryIds = todo.todoCategories.mapNotNull { it.category?.id }

        todoRepository.deleteById(todo.id)
        eventPublisher.publishEvent(TodoDeletedEvent(categoryIds = categoryIds))
    }
}
