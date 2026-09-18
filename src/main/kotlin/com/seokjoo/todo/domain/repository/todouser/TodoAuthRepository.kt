package com.seokjoo.todo.domain.repository.todouser

import com.seokjoo.todo.domain.entity.todouser.User
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface TodoAuthRepository : JpaRepository<User, Long> {
    fun findUserByUserId(userId: String): User?

    @Query("select u from User u left join fetch u.todoList where u.userId = :userId")
    fun findUserWithTodosByUserId(@Param("userId") userId: String): User?
}
