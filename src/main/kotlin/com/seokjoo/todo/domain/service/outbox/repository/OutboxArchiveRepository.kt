package com.seokjoo.todo.domain.service.outbox.repository

import com.seokjoo.todo.domain.entity.outbox.OutboxEventArchive
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OutboxArchiveRepository : JpaRepository<OutboxEventArchive, String>
