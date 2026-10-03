package com.seokjoo.todo.domain.service.email

import com.seokjoo.todo.domain.entity.email.EmailSentRecord
import com.seokjoo.todo.domain.entity.email.EmailSentStatus
import com.seokjoo.todo.domain.repository.email.TodoEmailRepository
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(javaClass)

    fun sendEmail(to: String, subject: String, content: String) {
        val message = SimpleMailMessage().apply {
            setTo(to)
            this.subject = subject
            this.text = content
        }
        mailSender.send(message)
    }

    @Transactional(readOnly = true)
    fun findStatusByEmailId(emailId: String): EmailSentStatus? {
        return emailRepository.findStatusByEmailId(emailId)
    }

    @Transactional
    fun saveEmailSentRecordToReady(id: String): EmailSentRecord {
        return emailRepository.save(EmailSentRecord(id = id, status = EmailSentStatus.READY))
    }

    @Transactional
    fun markSentAt(id: String, now: LocalDateTime = LocalDateTime.now()) {
        val effectedCount = emailRepository.updateSent(emailId = id, sentAt = now)
        if (effectedCount == 0) {
            logger.warn("No email found for id $id")
        }
    }

    @Transactional
    fun deleteSentRecord(id: String) {
        emailRepository.deleteByEmailId(emailId = id)
    }

    @Transactional
    fun reconcileConfirmedSentRecords(): Int {
        return emailRepository.reconcileConfirmedSentRecords()
    }
}
