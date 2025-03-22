package com.seokjoo.todo.domain.repository.todo

import com.seokjoo.todo.domain.entity.todo.Todo
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TodoRepository : JpaRepository<Todo, Long> {
    @Query("select t from Todo t left join fetch t.todoCategories tc left join fetch tc.category ORDER BY t.createdAt ASC")
    fun findAllByOrderByCreatedAtAsc(pageable: Pageable): Page<Todo>
}
