package com.seokjoo.todo.domain.repository.market

import com.seokjoo.todo.domain.entity.todo.Todo
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface TodoMarketRepository : JpaRepository<Todo, Long> {
    @Query("select t from Todo t where t.id < :cursor order by t.id desc")
    fun findMarketTodos(pageable: Pageable, cursor: Long): Slice<Todo>

    @Query(
        """
        select distinct t
        from Todo t
        join fetch t.owner
        left join fetch t.todoCategories tc
        left join fetch tc.category
        where t in :todos
    """
    )
    fun getFetchJoinedMarketTodoList(@Param("todos") todos: List<Todo>): List<Todo>
}
