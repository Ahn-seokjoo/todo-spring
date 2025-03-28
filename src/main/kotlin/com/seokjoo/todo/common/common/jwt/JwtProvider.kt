package com.seokjoo.todo.common.common.jwt

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Base64
import java.util.Date

@Component
class JwtProvider(
    @Value("\${jwt.secret:jwt-secret}") private val secret: String = "",
    @Value("\${jwt.accessTokenExpiration:1000}") private val accessTokenExpirationMs: Long = 1000L,
    @Value("\${jwt.refreshTokenExpiration:10000}") private val refreshTokenExpirationMs: Long = 1000L,
) {

    private val secretKey by lazy {
        val decodeKey = Base64.getUrlDecoder().decode(secret)
        Keys.hmacShaKeyFor(decodeKey)
    }

    fun generateToken(userId: String, tokenType: JwtTokenType): String {
        val now = Date()
        val expiredTime = if (tokenType == JwtTokenType.ACCESS) {
            Date(now.time + accessTokenExpirationMs)
        } else {
            Date(now.time + refreshTokenExpirationMs)
        }

        return Jwts.builder()
            .subject(userId) // 목적? 인데,,, 보통 id를 쓰나
            .issuer("pita")
            .issuedAt(now)
            .notBefore(now)
            .expiration(expiredTime)
            .signWith(secretKey)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return runCatching {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
            true
        }.getOrDefault(false)
    }
}
