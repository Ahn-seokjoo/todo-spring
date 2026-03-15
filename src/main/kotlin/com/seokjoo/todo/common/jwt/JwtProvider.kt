package com.seokjoo.todo.common.jwt

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
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
    @param:Value("\${jwt.secret}") private val secret: String,
    @param:Value("\${jwt.accessTokenExpiration}") private val accessTokenExpirationMs: Long,
    @param:Value("\${jwt.refreshTokenExpiration}") private val refreshTokenExpirationMs: Long,
) {

    private val secretKey by lazy {
        val decodeKey = Base64.getUrlDecoder().decode(secret)
        Keys.hmacShaKeyFor(decodeKey)
    }

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
            .issuer(ISSUER)
            .issuedAt(nowDate)
            .notBefore(nowDate)
            .expiration(expiredTime)
            .claim(TYPE, tokenType.name)
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

    fun checkValidSignature(userId: String, accessToken: String): Boolean {
        val subject = getSubject(accessToken)
        return subject == userId
    }

    fun getSubject(accessToken: String): String {
        return runCatching {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(accessToken)
                .payload
                .subject
        }.getOrElse {
            throw TodoException.of(TodoExceptionType.AUTH_REFRESH_TOKEN_NOT_VALID)
        }
    }

    fun getTokenType(token: String): JwtTokenType {
        return runCatching {
            val tokenType: String = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
                .get(TYPE, String::class.java)

            JwtTokenType.valueOf(tokenType)
        }.getOrElse {
            throw TodoException.of(TodoExceptionType.AUTH_REFRESH_TOKEN_NOT_VALID)
        }
    }

    companion object {
        private const val ISSUER = "pita"
        private const val TYPE = "TOKEN_TYPE"
    }
}
