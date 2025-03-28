package com.seokjoo.todo.domain.repository.todouser

import com.seokjoo.todo.domain.entity.todouser.TodoUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TodoAuthRepository : JpaRepository<TodoUser, Long>
