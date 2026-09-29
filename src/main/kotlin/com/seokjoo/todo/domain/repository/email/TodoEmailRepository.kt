package com.seokjoo.todo.domain.repository.email

import com.seokjoo.todo.domain.entity.email.EmailSentRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TodoEmailRepository : JpaRepository<EmailSentRecord, String> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from EmailSentRecord email where email.entityId = :emailId")
    fun deleteByEmailId(emailId: String): Int
}
