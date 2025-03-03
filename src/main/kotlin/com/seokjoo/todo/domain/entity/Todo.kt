package com.seokjoo.todo.domain.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "todos")
class Todo(
    var todo: String,
    var isDone: Boolean = false,

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "todo", orphanRemoval = true)
    var todoCategories: MutableList<TodoCategory> = mutableListOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "todo_id")
    val id: Long? = null,
) : BaseEntity() {
    fun addCategory(category: Category?, isNotExist: Boolean = true) {
        category?.let {
            val todoCategory = TodoCategory(todo = this, category = category)
            todoCategories.add(todoCategory)
            category.todoCategories.add(todoCategory)
        }
    }
}
