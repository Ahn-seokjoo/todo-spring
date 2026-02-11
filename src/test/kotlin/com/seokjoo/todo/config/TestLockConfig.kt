package com.seokjoo.todo.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@TestConfiguration
@Testcontainers
@Profile("test")
class TestLockConfig {

    private val REDIS_PORT: Int = 6379

    @Container
    private val redisContainer: GenericContainer<*> =
        GenericContainer("redis:7.4.1-alpine3.20").withExposedPorts(REDIS_PORT)

    init {
        redisContainer.start();
    }

    @Bean
    fun redisson(): RedissonClient {
        val config = Config().apply {
            useSingleServer().address = "redis://${redisContainer.host}:${redisContainer.getMappedPort(REDIS_PORT)}"
        }
        return Redisson.create(config)
    }

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        return LettuceConnectionFactory(
            RedisStandaloneConfiguration(
                redisContainer.host, redisContainer.getMappedPort(REDIS_PORT)
            )
        )
    }
}
