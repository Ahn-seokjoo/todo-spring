package com.seokjoo.todo.presentation.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.seokjoo.todo.common.common.jwt.JwtAuthFilter
import com.seokjoo.todo.common.common.jwt.JwtProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class TodoConfiguration(
    private val jwtProvider: JwtProvider,
    private val objectMapper: ObjectMapper,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    @ConditionalOnProperty(name = ["jwt.filter.enabled"], havingValue = "true", matchIfMissing = true)
    fun jwtAuthFilter(): FilterRegistrationBean<JwtAuthFilter> {
        return FilterRegistrationBean(JwtAuthFilter(jwtProvider, objectMapper)).apply {
            addUrlPatterns("/*")
            order = 1
        }
    }
}
