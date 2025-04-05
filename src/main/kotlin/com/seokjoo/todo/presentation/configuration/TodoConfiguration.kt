package com.seokjoo.todo.presentation.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.seokjoo.todo.common.common.jwt.JwtAuthFilter
import com.seokjoo.todo.common.common.jwt.JwtProvider
import com.seokjoo.todo.domain.service.todo.TodoPageServiceResponseDTO
import com.seokjoo.todo.domain.service.todo.TodoServiceResponseDTO
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
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
        val pagedTodoSerializer = Jackson2JsonRedisSerializer(objectMapper, TodoPageServiceResponseDTO::class.java)
        val todoSerializer = Jackson2JsonRedisSerializer(objectMapper, TodoServiceResponseDTO::class.java)

        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .entryTtl(Duration.ofMinutes(1))

        val pagedTodoConfig = defaultConfig.serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(pagedTodoSerializer)
        )
        val todoConfig = defaultConfig.serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(todoSerializer)
        )

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(mapOf("todos" to pagedTodoConfig, "todo" to todoConfig))
            .build()
    }

    @Bean
    fun redisConnectionFactory(@Value("\${spring.data.redis.port:6379}") port: Int): RedisConnectionFactory {
        val redisHost = "localhost"
        return LettuceConnectionFactory(redisHost, port)
    }

    @Bean
    fun redisTemplate(redisConnectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        return RedisTemplate<String, Any>().apply {
            connectionFactory = redisConnectionFactory
            keySerializer = StringRedisSerializer()
            valueSerializer = GenericJackson2JsonRedisSerializer()
        }
    }
}
