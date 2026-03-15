package com.seokjoo.todo.common.jwt

import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import io.jsonwebtoken.Claims
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

    fun getTokenPayload(token: String): Claims {
        return runCatching {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
        }.getOrElse {
            throw TodoException.of(TodoExceptionType.AUTH_REFRESH_TOKEN_NOT_VALID)
        }
    }

    fun isSubjectMatching(userId: String, token: String): Boolean {
        val subject = getSubject(token)
        return subject == userId
    }

    fun getSubject(token: String): String {
        return getTokenPayload(token).subject
    }

    fun getTokenType(token: String): JwtTokenType {
        val tokenType = getTokenPayload(token).get(TYPE, String::class.java)
        return JwtTokenType.valueOf(tokenType)
    }

    companion object {
        private const val ISSUER = "pita"
        private const val TYPE = "TOKEN_TYPE"
    }
}
