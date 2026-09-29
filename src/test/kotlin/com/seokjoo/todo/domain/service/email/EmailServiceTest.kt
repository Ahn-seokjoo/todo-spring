package com.seokjoo.todo.domain.service.email

import com.seokjoo.todo.annotation.TodoTest
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@TodoTest
@Transactional
class EmailServiceTest @Autowired constructor(
    private val emailService: EmailService,
) {

    @Test
    fun `같은 outboxEventId로 markSentAt을 두 번 호출하면 두 번째는 중복 키 예외가 발생한다`() {
        val id = UUID.randomUUID().toString()

        emailService.markSentAt(id = id)

        assertThrows(DataIntegrityViolationException::class.java) {
            emailService.markSentAt(id = id)
        }
    }
}
