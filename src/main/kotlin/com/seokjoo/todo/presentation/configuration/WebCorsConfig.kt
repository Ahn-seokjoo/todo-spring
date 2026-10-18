package com.seokjoo.todo.presentation.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class WebCorsConfig(
    @param:Value("\${cors.allowed-origins}") private val allowedOriginList: List<String>,
) {

    @Bean
    fun corsFilter(): FilterRegistrationBean<CorsFilter> {
        val config = CorsConfiguration().apply {
            allowedOrigins = allowedOriginList
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH")
            allowedHeaders = listOf("Authorization", "Content-Type")
            maxAge = 3600L
            exposedHeaders = listOf("Location")
        }
        val source = UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/api/**", config)
        }
        return FilterRegistrationBean(CorsFilter(source)).apply {
            addUrlPatterns("/*")
            order = 0
        }
    }
}
