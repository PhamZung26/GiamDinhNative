package com.tc128.giamdinhnative.data.repository

import com.tc128.giamdinhnative.data.remote.ApiService
import com.tc128.giamdinhnative.data.remote.LoginRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun login(username: String, password: String): String {
        val trimmed = username.trim()
        return apiService.login(
            LoginRequest(
                email = if (trimmed.contains("@")) trimmed else "$trimmed@tc128.com",
                password = password.trim()
            )
        )
    }
}
