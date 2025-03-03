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
@Table(name = "categories")
class Category(
    @Column(unique = true)
    val name: String,

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "category")
    val todoCategories: MutableList<TodoCategories> = mutableListOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    val id: Long? = null,
) : BaseEntity() {
    fun addTodo(todo: Todo) {
        val todoCategory = TodoCategories(todo = todo, category = this)
        todoCategories.add(todoCategory)
        todo.todoCategories.add(todoCategory)
    }
}
