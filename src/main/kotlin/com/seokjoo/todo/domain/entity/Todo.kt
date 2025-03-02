package com.seokjoo.todo.domain.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import kotlin.math.ceil

@Entity
@Table(name = "todos")
class Todo(
    var todo: String,
    var isDone: Boolean = false,

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "todo", orphanRemoval = true)
    @JoinColumn(name = "todo_categories_id")
    var todoCategories: MutableList<TodoCategories>,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "todo_id")
    val id: Long? = null,
) : BaseEntity() {
    fun addCategory(category: Category) {
        val todoCategory = TodoCategories(todo = this, category = category)
        todoCategories.add(todoCategory)
        category.todoCategories.add(todoCategory)
    }
}
