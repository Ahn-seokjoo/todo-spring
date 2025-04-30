package com.seokjoo.todo.domain.entity.todouser

import com.seokjoo.todo.domain.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "todo_user")
class User(
    @Column(name = "user_id", nullable = false)
    val userId: String,
    @Column(nullable = false)
    val password: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "todo_user_id")
    val id: Long = 0L,

    var balance: Long = 0L,
) : BaseEntity()
