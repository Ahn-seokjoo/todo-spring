package com.seokjoo.todo.presentation.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.seokjoo.todo.common.common.jwt.JwtAuthFilter
import com.seokjoo.todo.common.common.jwt.JwtProvider
import com.seokjoo.todo.domain.service.todo.TodoPageServiceResponseDTO
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Duration

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
    fun jwtAuthFilter(): FilterRegistrationBean<JwtAuthFilter> {
        return FilterRegistrationBean(JwtAuthFilter(jwtProvider, objectMapper)).apply {
            addUrlPatterns("/*")
            order = 1
        }
    }

    @Bean
    fun redisCacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
        val objectMapper = jacksonObjectMapper()
        val serializer = Jackson2JsonRedisSerializer(objectMapper, TodoPageServiceResponseDTO::class.java)

        val config = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
            .entryTtl(Duration.ofMinutes(1))

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build()
    }
}
