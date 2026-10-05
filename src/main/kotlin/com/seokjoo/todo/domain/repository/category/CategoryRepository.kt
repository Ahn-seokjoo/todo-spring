package com.seokjoo.todo.domain.repository.category

import com.seokjoo.todo.domain.entity.category.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CategoryRepository : JpaRepository<Category, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        "delete from Category c where c.id in :ids and " +
            "not exists (select tc.id from TodoCategory tc where tc.category.id = c.id)"
    )
    fun deleteCategoryByIdsIfNotExistInTodoCategory(ids: List<Long>)

    fun findCategoryByName(name: String): Category?
}
