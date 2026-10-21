package com.seokjoo.todo.domain.entity.stats

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "stats_processed_event")
class StatsProcessedEvent(
    @Id
    @Column(name = "id", nullable = false, length = 36)
    val id: String, // 아웃박스 이벤트 id

    @Column(name = "processed_at", nullable = false)
    val processedAt: LocalDateTime,
)
