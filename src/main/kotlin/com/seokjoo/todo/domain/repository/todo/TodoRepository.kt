package com.seokjoo.todo.domain.repository.todo

import com.seokjoo.todo.domain.entity.todo.Todo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TodoRepository : JpaRepository<Todo, Long>
