package com.tc128.giamdinhnative.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tokenKey = stringPreferencesKey("token")
    private val usernameKey = stringPreferencesKey("username")
    // Size/Operator chọn lần gần nhất ở màn Tạo mới container — nhớ lại giữa các phiên mở app để
    // người dùng không phải chọn lại (đa số container tạo liên tiếp cùng Size/Operator)
    private val lastSizeIdKey = intPreferencesKey("last_size_id")
    private val lastOptIdKey = intPreferencesKey("last_opt_id")
    // Trạng thái flash chọn lần gần nhất (ImageCapture.FLASH_MODE_*), nhớ giữa các lần mở camera
    private val lastFlashModeKey = intPreferencesKey("last_flash_mode")

    val token: Flow<String?> = context.dataStore.data.map { it[tokenKey] }
    val username: Flow<String?> = context.dataStore.data.map { it[usernameKey] }

    suspend fun getToken(): String? = token.firstOrNull()

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[tokenKey] = token }
    }

    suspend fun saveUsername(username: String) {
        context.dataStore.edit { it[usernameKey] = username }
    }

    suspend fun getLastSizeId(): Int? = context.dataStore.data.map { it[lastSizeIdKey] }.firstOrNull()
    suspend fun getLastOptId(): Int? = context.dataStore.data.map { it[lastOptIdKey] }.firstOrNull()

    suspend fun saveLastSizeId(id: Int) {
        context.dataStore.edit { it[lastSizeIdKey] = id }
    }

    suspend fun saveLastOptId(id: Int) {
        context.dataStore.edit { it[lastOptIdKey] = id }
    }

    // Mặc định lần đầu = FLASH_MODE_AUTO (0)
    suspend fun getLastFlashMode(): Int =
        context.dataStore.data.map { it[lastFlashModeKey] }.firstOrNull()
            ?: androidx.camera.core.ImageCapture.FLASH_MODE_AUTO

    suspend fun saveLastFlashMode(mode: Int) {
        context.dataStore.edit { it[lastFlashModeKey] = mode }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
