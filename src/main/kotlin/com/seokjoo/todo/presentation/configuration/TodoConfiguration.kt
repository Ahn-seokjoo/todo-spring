package com.seokjoo.todo.presentation.configuration

import com.seokjoo.todo.common.common.jwt.JwtAuthFilter
import com.seokjoo.todo.common.common.jwt.JwtProvider
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class TodoConfiguration(
    private val jwtProvider: JwtProvider,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun jwtAuthFilter(): FilterRegistrationBean<JwtAuthFilter> {
        return FilterRegistrationBean(JwtAuthFilter(jwtProvider)).apply {
            addUrlPatterns("/*")
            order = 1
        }
    }
}
