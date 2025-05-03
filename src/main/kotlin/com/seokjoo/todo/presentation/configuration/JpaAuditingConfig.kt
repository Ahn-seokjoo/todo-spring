package com.seokjoo.todo.presentation.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@Configuration
@EnableJpaAuditing
@Profile("!test")
class JpaAuditingConfig
