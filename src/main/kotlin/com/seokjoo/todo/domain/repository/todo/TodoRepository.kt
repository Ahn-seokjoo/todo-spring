package com.seokjoo.todo.domain.repository.todo

import com.seokjoo.todo.domain.entity.todo.Todo
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface TodoRepository : JpaRepository<Todo, Long> {
    @Query("select t from Todo t where t.owner.userId = :ownerId ORDER BY t.createdAt ASC")
    fun findAllSlicedTodoOrderByUpdatedAt(pageable: Pageable, ownerId: String): Slice<Todo>

    @Query("select distinct t from Todo t left join fetch t.todoCategories tc left join fetch tc.category where t in :todos")
    fun getFetchJoinedTodoList(@Param("todos") todos: List<Todo>): List<Todo>

    @Query("select count(*) from Todo t where t.owner.userId = :ownerId")
    fun countByOwnerId(ownerId: String): Long

    @Query("select t.owner.userId from Todo t where t.id = :todoId")
    fun findTodoOwnerByTodoId(todoId: Long): String?

    @Query("select t.price from Todo t where t.id = :todoId")
    fun findTodoPriceByTodoId(todoId: Long): Long?

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Todo t set t.status = 'PENDING_APPROVAL', t.version = t.version + 1 where t.id = :todoId And t.status = 'AVAILABLE'")
    fun updateTodoStatusPendingIfAvailable(todoId: Long): Int

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Todo t set t.status = 'AVAILABLE', t.version = t.version + 1  where t.id = :todoId And t.status = 'PENDING_APPROVAL'")
    fun updateTodoStatusAvailableIfPending(todoId: Long): Int
}
