package com.seokjoo.todo.domain.service.email

import com.seokjoo.todo.domain.entity.email.EmailSentRecord
import com.seokjoo.todo.domain.repository.email.TodoEmailRepository
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val emailRepository: TodoEmailRepository,
) {

    fun sendEmail(to: String, subject: String, content: String) {
        val message = SimpleMailMessage().apply {
            setTo(to)
            this.subject = subject
            this.text = content
        }
        mailSender.send(message)
    }

    @Transactional
    fun markSentAt(id: String, now: LocalDateTime = LocalDateTime.now()): EmailSentRecord {
        return emailRepository.save(EmailSentRecord(id = id, sentAt = now))
    }

    @Transactional
    fun deleteSentRecord(id: String) {
        emailRepository.deleteByEmailId(emailId = id)
    }
}
