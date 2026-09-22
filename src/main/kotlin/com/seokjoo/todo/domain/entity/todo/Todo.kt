package com.seokjoo.todo.domain.entity.todo

import com.seokjoo.todo.domain.entity.BaseEntity
import com.seokjoo.todo.domain.entity.category.Category
import com.seokjoo.todo.domain.entity.todocategory.TodoCategory
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.service.todo.TodoUpdateServiceRequestDTO
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.ColumnDefault

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

    @Column(nullable = false)
    var price: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    var owner: User,

    @Column(name = "todo_status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: TodoStatus = TodoStatus.AVAILABLE,

    @Version
    @Column(nullable = false)
    @ColumnDefault(value = "0")
    var version: Long = 0L,
) : BaseEntity() {
    fun hasCategory(categoryName: String): Boolean {
        return todoCategories.any { it.category?.name == categoryName }
    }

    fun addCategory(category: Category) {
        val todoCategory = TodoCategory(todo = this, category = category)
        todoCategories.add(todoCategory)
        category.todoCategories.add(todoCategory)
    }

    fun todoUpdateApply(
        request: TodoUpdateServiceRequestDTO,
    ) {
        todo.apply {
            todo = request.todo.ifBlank { todo }
            isDone = request.isDone ?: isDone
            price = request.price ?: price
        }
    }

    fun updateOwner(owner: User) {
        this.owner = owner
    }

    fun copy(
        todo: String? = null,
        isDone: Boolean? = null,
        todoCategories: MutableList<TodoCategory>? = null,
        id: Long? = null,
        owner: User? = null,
        price: Long? = null,
        status: TodoStatus? = null,
    ) = Todo(
        todo = todo ?: this.todo,
        isDone = isDone ?: this.isDone,
        todoCategories = todoCategories ?: this.todoCategories,
        id = id ?: this.id,
        owner = owner ?: this.owner,
        price = price ?: this.price,
        status = status ?: this.status,
    )
}
