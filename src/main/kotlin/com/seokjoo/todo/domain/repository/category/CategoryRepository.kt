package com.seokjoo.todo.domain.repository.category

import com.seokjoo.todo.domain.entity.category.Category
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface CategoryRepository : JpaRepository<Category, Long> {

    @Query("select count(tc) from TodoCategory tc where tc.category.id = :categoryId")
    fun countByCategoryId(categoryId: Long): Long

    fun findCategoryByName(name: String): Category?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Category c WHERE c.name = :name")
    fun findCategoryByNameWithLock(@Param("name") name: String): Category?
}
