package com.seokjoo.todo.domain.service.email

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
) {

    fun sendEmail(to: String, subject: String, content: String) {
        val message = SimpleMailMessage().apply {
            setTo(to)
            this.subject = subject
            this.text = content
        }
        mailSender.send(message)
    }
}
