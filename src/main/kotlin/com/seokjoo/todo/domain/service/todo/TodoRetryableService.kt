package com.seokjoo.todo.domain.service.todo

import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service

@Service
class TodoRetryableService(
    private val todoService: TodoService,
) {

    @Retryable(
        retryFor = [OptimisticLockingFailureException::class],
        backoff = Backoff(delay = 100, maxDelay = 300, random = true),
        maxAttempts = 3,
    )
    fun updateTodo(todoId: Long, userId: String, request: TodoUpdateServiceRequestDTO): TodoServiceResponseDTO {
        return todoService.updateTodo(todoId, userId, request)
    }

    @Retryable(
        retryFor = [OptimisticLockingFailureException::class],
        backoff = Backoff(delay = 100, maxDelay = 300, random = true),
        maxAttempts = 3,
    )
    fun deleteTodo(todoId: Long, userId: String) {
        return todoService.deleteTodo(todoId, userId)
    }
}
