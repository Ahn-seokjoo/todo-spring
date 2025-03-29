package com.seokjoo.todo.domain.repository.todouser

import com.seokjoo.todo.domain.entity.todouser.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TodoAuthRepository : JpaRepository<User, Long> {
    fun findUserByUserId(userId: String): User?
}
