package com.seokjoo.todo.presentation.todo.service

import com.seokjoo.todo.domain.repository.TodoRepository
import org.springframework.stereotype.Service

@Service
class TodoService(
    private val todoRepository: TodoRepository,
)
