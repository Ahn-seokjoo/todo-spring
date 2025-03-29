package com.seokjoo.todo.domain.service.auth

import com.seokjoo.todo.common.common.encryptor.TodoAuthEncryptor
import com.seokjoo.todo.common.common.jwt.JwtProvider
import com.seokjoo.todo.common.common.jwt.JwtTokenType
import com.seokjoo.todo.common.exception.TodoException
import com.seokjoo.todo.common.exception.TodoExceptionType
import com.seokjoo.todo.domain.entity.todouser.User
import com.seokjoo.todo.domain.repository.todouser.TodoAuthRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TodoAuthService(
    private val jwtProvider: JwtProvider,
    private val encryptor: TodoAuthEncryptor,
    private val todoAuthRepository: TodoAuthRepository,
) {
    @Transactional
    fun signUp(userId: String, password: String) {
        val encodedPassword = encryptor.encrypt(password)
        val user = User(userId = userId, password = encodedPassword)
        todoAuthRepository.save(user)
    }

    @Transactional(readOnly = true)
    fun login(userId: String, password: String): TodoAuthServiceLoginResponse {
        val user =
            todoAuthRepository.findUserByUserId(userId) ?: throw TodoException.of(TodoExceptionType.AUTH_USER_NOT_EXIST)
        val validate = encryptor.validatePassword(password = password, encodedPassword = user.password)
        if (validate) {
            val accessToken = jwtProvider.generateToken(userId, JwtTokenType.ACCESS)
            val refreshToken = jwtProvider.generateToken(userId, JwtTokenType.REFRESH)
            return TodoAuthServiceLoginResponse(accessToken, refreshToken)
        } else {
            throw TodoException.of(TodoExceptionType.AUTH_NOT_MATCHED_PASSWORD)
        }
    }

    fun refreshToken() {}
}
