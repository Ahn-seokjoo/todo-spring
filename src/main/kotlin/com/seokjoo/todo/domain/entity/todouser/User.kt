package com.seokjoo.todo.domain.entity.todouser

import com.seokjoo.todo.domain.entity.BaseEntity
import com.seokjoo.todo.domain.entity.todo.Todo
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(name = "todo_user", uniqueConstraints = [UniqueConstraint(name = "UC_user_id", columnNames = ["user_id"])])
class User(
    @Column(name = "user_id", nullable = false)
    val userId: String,
    @Column(nullable = false)
    val password: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "todo_user_id")
    val id: Long = 0L,

    @Column(nullable = false)
    var money: Money = Money(),

    @Column(name = "todo_list", nullable = false)
    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "owner", orphanRemoval = true)
    val todoList: MutableList<Todo> = mutableListOf(),
) : BaseEntity()
