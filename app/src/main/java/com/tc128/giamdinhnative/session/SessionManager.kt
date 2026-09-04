package com.tc128.giamdinhnative.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val DEFAULT_SERVER_URL = "https://tc128hp.hopto.org/"
        const val DEFAULT_RESIZE_MAX_DIM = 1280
        const val DEFAULT_MAX_UPLOAD_COUNT = 1000
    }

    private val tokenKey = stringPreferencesKey("token")
    private val usernameKey = stringPreferencesKey("username")
    // Size/Operator chọn lần gần nhất ở màn Tạo mới container — nhớ lại giữa các phiên mở app để
    // người dùng không phải chọn lại (đa số container tạo liên tiếp cùng Size/Operator)
    private val lastSizeIdKey = intPreferencesKey("last_size_id")
    private val lastOptIdKey = intPreferencesKey("last_opt_id")
    // Trạng thái flash chọn lần gần nhất (ImageCapture.FLASH_MODE_*), nhớ giữa các lần mở camera
    private val lastFlashModeKey = intPreferencesKey("last_flash_mode")
    // Cấu hình do người dùng chỉnh: tên miền máy chủ, kích cỡ resize ảnh, số lượng ảnh upload/lô
    private val serverUrlKey = stringPreferencesKey("server_url")
    private val serverListKey = stringSetPreferencesKey("server_list")   // danh sách server đã lưu
    private val resizeMaxDimKey = intPreferencesKey("resize_max_dim")
    private val maxUploadCountKey = intPreferencesKey("max_upload_count")
    private val sealUseMlKitKey = booleanPreferencesKey("seal_use_mlkit")   // quét seal bằng ML Kit?
    private val defaultCleanMethodsKey = stringSetPreferencesKey("default_clean_methods")  // loại vệ sinh mặc định (đa chọn)

    // Cache trong RAM để đọc ĐỒNG BỘ (OkHttp interceptor rewrite host, ImageResizer, worker) mà
    // không phải suspend. Nạp 1 lần lúc khởi tạo, cập nhật ngay khi lưu.
    @Volatile var cachedServerUrl: String = DEFAULT_SERVER_URL
        private set
    @Volatile var cachedResizeMaxDim: Int = DEFAULT_RESIZE_MAX_DIM
        private set
    @Volatile var cachedMaxUploadCount: Int = DEFAULT_MAX_UPLOAD_COUNT
        private set

    init {
        runBlocking {
            val prefs = context.dataStore.data.first()
            cachedServerUrl = prefs[serverUrlKey] ?: DEFAULT_SERVER_URL
            cachedResizeMaxDim = prefs[resizeMaxDimKey] ?: DEFAULT_RESIZE_MAX_DIM
            cachedMaxUploadCount = prefs[maxUploadCountKey] ?: DEFAULT_MAX_UPLOAD_COUNT
        }
    }

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

    // ── Cấu hình người dùng (server / resize / số lượng upload) ──────────────
    suspend fun getServerUrl(): String =
        context.dataStore.data.map { it[serverUrlKey] }.firstOrNull() ?: DEFAULT_SERVER_URL

    // Chọn server làm active + tự thêm vào danh sách (nếu chưa có)
    suspend fun saveServerUrl(url: String) {
        val normalized = normalizeUrl(url)
        context.dataStore.edit { prefs ->
            prefs[serverUrlKey] = normalized
            val list = (prefs[serverListKey] ?: emptySet()).toMutableSet()
            list.add(normalized)
            prefs[serverListKey] = list
        }
        cachedServerUrl = normalized
    }

    // Danh sách server đã lưu (luôn có server mặc định), server active đứng đầu
    suspend fun getServerList(): List<String> {
        val prefs = context.dataStore.data.first()
        val active = prefs[serverUrlKey] ?: DEFAULT_SERVER_URL
        val set = (prefs[serverListKey] ?: emptySet()) + DEFAULT_SERVER_URL + active
        return (listOf(active) + set.filter { it != active }).distinct()
    }

    suspend fun removeServer(url: String) {
        val normalized = normalizeUrl(url)
        if (normalized == DEFAULT_SERVER_URL) return   // không cho xoá server mặc định
        context.dataStore.edit { prefs ->
            val list = (prefs[serverListKey] ?: emptySet()).toMutableSet()
            list.remove(normalized)
            prefs[serverListKey] = list
            // Nếu đang active mà bị xoá → quay về mặc định
            if (prefs[serverUrlKey] == normalized) {
                prefs[serverUrlKey] = DEFAULT_SERVER_URL
                cachedServerUrl = DEFAULT_SERVER_URL
            }
        }
    }

    private fun normalizeUrl(url: String): String {
        var u = url.trim()
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://$u"
        if (!u.endsWith("/")) u = "$u/"
        return u
    }

    suspend fun getResizeMaxDim(): Int =
        context.dataStore.data.map { it[resizeMaxDimKey] }.firstOrNull() ?: DEFAULT_RESIZE_MAX_DIM

    suspend fun saveResizeMaxDim(dim: Int) {
        context.dataStore.edit { it[resizeMaxDimKey] = dim }
        cachedResizeMaxDim = dim
    }

    suspend fun getMaxUploadCount(): Int =
        context.dataStore.data.map { it[maxUploadCountKey] }.firstOrNull() ?: DEFAULT_MAX_UPLOAD_COUNT

    suspend fun saveMaxUploadCount(count: Int) {
        context.dataStore.edit { it[maxUploadCountKey] = count }
        cachedMaxUploadCount = count
    }

    // Quét số seal bằng ML Kit (on-device) thay vì ocr.space? Mặc định false (dùng ocr.space).
    suspend fun getSealUseMlKit(): Boolean =
        context.dataStore.data.map { it[sealUseMlKitKey] }.firstOrNull() ?: false

    suspend fun saveSealUseMlKit(enabled: Boolean) {
        context.dataStore.edit { it[sealUseMlKitKey] = enabled }
    }

    // Loại phương án vệ sinh hiển thị mặc định ở màn "Container cần vệ sinh" (đa chọn). Rỗng = chưa
    // cấu hình → màn hình tự mặc định phương án chứa "quét" (giữ hành vi cũ).
    suspend fun getDefaultCleanMethodIds(): Set<Int> =
        (context.dataStore.data.map { it[defaultCleanMethodsKey] }.firstOrNull() ?: emptySet())
            .mapNotNull { it.toIntOrNull() }.toSet()

    suspend fun saveDefaultCleanMethodIds(ids: Set<Int>) {
        context.dataStore.edit { it[defaultCleanMethodsKey] = ids.map { id -> id.toString() }.toSet() }
    }

    // Đăng xuất: CHỈ xoá token + username, GIỮ cấu hình thiết bị (server/resize/số lượng/lựa chọn gần nhất)
    suspend fun clear() {
        context.dataStore.edit {
            it.remove(tokenKey)
            it.remove(usernameKey)
        }
    }
}
