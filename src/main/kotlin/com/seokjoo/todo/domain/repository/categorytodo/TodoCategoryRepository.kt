package com.seokjoo.todo.domain.repository.categorytodo

import com.seokjoo.todo.domain.entity.todocategory.TodoCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TodoCategoryRepository : JpaRepository<TodoCategory, Long> {
    @Query(
        "select distinct tc.category.id from TodoCategory tc " +
            "where tc.todo.owner.userId = :ownerId and tc.category is not null"
    )
    fun findCategoryIdsByTodoOwnerId(ownerId: String): List<Long>
}
