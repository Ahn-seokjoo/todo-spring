package com.seokjoo.todo.domain.service.email

import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    @param:Value("\${spring.mail.username:tjrwn0716@naver.com}") private val appEmail: String,
) {

    fun sendEmail(to: String, subject: String, email: String) {
        val message = SimpleMailMessage().apply {
            setTo(to)
            this.subject = subject
            text = email
        }
        mailSender.send(message)
    }
}
