package com.seokjoo.todo.domain.entity.todouser

import com.seokjoo.todo.domain.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class TodoUser(
    val userId: String,
    val password: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "todo_user_id")
    val id: Long = 0L,
) : BaseEntity()
