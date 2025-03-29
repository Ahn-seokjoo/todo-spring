package com.seokjoo.todo.common.common.jwt

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Base64
import java.util.Date

@Component
class JwtProvider(
    @Value("\${jwt.secret:jwt-secret}") private val secret: String,
    @Value("\${jwt.accessTokenExpiration:1000}") private val accessTokenExpirationMs: Long,
    @Value("\${jwt.refreshTokenExpiration:10000}") private val refreshTokenExpirationMs: Long,
) {

    private val secretKey by lazy {
        val decodeKey = Base64.getUrlDecoder().decode(secret)
        Keys.hmacShaKeyFor(decodeKey)
    }

    /**
     * time 수정 예정
     */
    fun generateToken(userId: String, tokenType: JwtTokenType): String {
        val zoneId = ZoneId.of("Asia/Seoul")
        val now = ZonedDateTime.now(zoneId) // 서울 시간 기준 현재 시간
        val nowDate = Date.from(now.toInstant())

        val expiredTime = if (tokenType == JwtTokenType.ACCESS) {
            Date.from(now.plus(Duration.ofMillis(accessTokenExpirationMs)).toInstant())
        } else {
            Date.from(now.plus(Duration.ofMillis(refreshTokenExpirationMs)).toInstant())
        }

        return Jwts.builder()
            .subject(userId)
            .issuer("pita")
            .issuedAt(nowDate)
            .notBefore(nowDate)
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
        }.getOrElse {
            it
            false
        }
    }

    fun checkSignature(userId: String, accessToken: String): Boolean {
        val subject = runCatching {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(accessToken)
                .payload
                .subject
        }.getOrElse {
            if (it is ExpiredJwtException) it.claims.subject
            else ""
        }

        return subject == userId
    }
}
