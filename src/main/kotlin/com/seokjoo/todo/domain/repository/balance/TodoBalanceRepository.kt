package com.seokjoo.todo.domain.repository.balance

import com.seokjoo.todo.domain.entity.todouser.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TodoBalanceRepository : JpaRepository<User, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.money = u.money + :amount where u.userId = :userId")
    fun increaseBalanceIfSufficient(userId: String, amount: Long): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.money = u.money - :amount where u.userId = :userId and u.money >= :amount")
    fun decreaseBalanceIfSufficient(userId: String, amount: Long): Int

    @Query("select u.money from User u where u.userId = :userId")
    fun findBalanceByUserId(userId: String): Long?
}
