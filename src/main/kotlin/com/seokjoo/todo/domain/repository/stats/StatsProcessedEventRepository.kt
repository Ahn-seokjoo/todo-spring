package com.seokjoo.todo.domain.repository.stats

import com.seokjoo.todo.domain.entity.stats.StatsProcessedEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface StatsProcessedEventRepository : JpaRepository<StatsProcessedEvent, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = "INSERT IGNORE INTO stats_processed_event(id,processed_at) VALUES(:eventId, NOW(6))",
        nativeQuery = true,
    )
    fun insertStatsProcessedEvent(eventId: String): Int
}
