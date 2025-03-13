package com.seokjoo.todo.domain.entity.todo

import com.seokjoo.todo.domain.entity.BaseEntity
import com.seokjoo.todo.domain.entity.category.Category
import com.seokjoo.todo.domain.entity.todocategory.TodoCategory
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "todo")
class Todo(
    var todo: String,
    var isDone: Boolean = false,

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "todo", orphanRemoval = true)
    var todoCategories: MutableList<TodoCategory> = mutableListOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "todo_id")
    val id: Long = 0L,
) : BaseEntity() {
    fun addCategory(category: Category) {
        val todoCategory = TodoCategory(todo = this, category = category)
        todoCategories.add(todoCategory)
        category.todoCategories.add(todoCategory)
    }
}
