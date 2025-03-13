package com.seokjoo.todo.domain.repository.categorytodo

import com.seokjoo.todo.domain.entity.todocategory.TodoCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TodoCategoryRepository : JpaRepository<TodoCategory, Long> {
    fun countByCategoryId(id: Long?): Int
}
