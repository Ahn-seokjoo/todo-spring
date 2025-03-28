package com.seokjoo.todo.common.common.encryptor

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class TodoAuthEncryptor(
    private val encoder: PasswordEncoder,
) {

    fun encrypt(password: String): String {
        return encoder.encode(password)
    }

    fun validatePassword(password: String, encodedPassword: String): Boolean {
        return encoder.matches(password, encodedPassword)
    }
}
