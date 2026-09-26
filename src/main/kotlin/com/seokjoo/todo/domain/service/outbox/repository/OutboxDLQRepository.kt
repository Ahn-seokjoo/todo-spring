package com.seokjoo.todo.domain.service.outbox.repository

import com.seokjoo.todo.domain.entity.outbox.OutboxEventDLQ
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OutboxDLQRepository : JpaRepository<OutboxEventDLQ, String>
