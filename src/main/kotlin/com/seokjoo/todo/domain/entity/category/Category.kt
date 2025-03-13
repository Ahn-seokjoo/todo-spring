package com.seokjoo.todo.domain.entity.category

import com.seokjoo.todo.domain.entity.BaseEntity
import com.seokjoo.todo.domain.entity.todocategory.TodoCategory
import com.seokjoo.todo.domain.entity.todo.Todo
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "category")
class Category(
    @Column(unique = true)
    val name: String,

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "category")
    val todoCategories: MutableList<TodoCategory> = mutableListOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    val id: Long = 0L,
) : BaseEntity() {
    fun addTodo(todo: Todo) {
        val todoCategory = TodoCategory(todo = todo, category = this)
        todoCategories.add(todoCategory)
        todo.todoCategories.add(todoCategory)
    }
}
