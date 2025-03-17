package com.seokjoo.todo.domain.entity.todocategory

import com.seokjoo.todo.domain.entity.BaseEntity
import com.seokjoo.todo.domain.entity.category.Category
import com.seokjoo.todo.domain.entity.todo.Todo
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "todo_category")
class TodoCategory(

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "todo_id")
    val todo: Todo,

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
    @JoinColumn(name = "category_id", nullable = true)
    val category: Category?,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "todo_category_id")
    val id: Long = 0L,
) : BaseEntity()
