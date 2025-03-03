package com.seokjoo.todo.domain.repository

import com.seokjoo.todo.domain.entity.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CategoryRepository : JpaRepository<Category, Long> {

    @Query("select count(tc) from TodoCategory tc where tc.category.id = :categoryId")
    fun countByCategoryId(categoryId: Long): Long

    fun findByName(name: String): Category?
}
