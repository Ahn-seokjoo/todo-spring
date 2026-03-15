package com.seokjoo.todo.domain.entity.todouser

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
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
) : BaseEntity() {
    fun removeTodo(todo: Todo) {
        todoList.remove(todo)
    }

    fun increaseBalance(balance: Long) {
        money = money.increase(balance)
    }

    fun decreaseBalance(balance: Long) {
        money = money.decrease(balance)
    }

    fun isAffordable(price: Long) {
        check(currentBalance() >= price) { throw TodoException.of(TodoExceptionType.BALANCE_NOT_ENOUGH) }
    }

    fun needToCharge() = money.needToCharge()

    fun currentBalance() = money.currentBalance()
}
