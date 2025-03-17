package com.seokjoo.todo.domain.repository.todo

import com.seokjoo.todo.domain.entity.todo.Todo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TodoRepository : JpaRepository<Todo, Long> {

    @Query("select t from Todo t left join fetch t.todoCategories")
    fun findAllWithCategories(): List<Todo>
}
