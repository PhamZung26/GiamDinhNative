package com.tc128.giamdinhnative.ui.screens.about

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tc128.giamdinhnative.data.remote.AppUpdateInfo
import com.tc128.giamdinhnative.data.repository.LookupRepository
import com.tc128.giamdinhnative.data.repository.PhotoRepository
import com.tc128.giamdinhnative.session.SessionManager
import com.tc128.giamdinhnative.util.UpdateChecker
import com.tc128.giamdinhnative.worker.PhotoResizeWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AboutUiState(
    val username: String? = null,
    val pendingUploadCount: Int = 0,
    val isUploading: Boolean = false,
    val isLoggedOut: Boolean = false,

    val currentVersionName: String = "",
    val isCheckingUpdate: Boolean = false,
    val availableUpdate: AppUpdateInfo? = null,
    val isDownloadingUpdate: Boolean = false,
    val updateError: String? = null,

    val isSyncingCatalog: Boolean = false,
    val catalogSyncMessage: String? = null
)

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val photoRepository: PhotoRepository,
    private val updateChecker: UpdateChecker,
    private val lookupRepository: LookupRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState = _uiState.asStateFlow()

    // Không load ở đây — AboutScreen gọi load() qua repeatOnLifecycle(RESUMED),
    // tự fire ngay lần đầu vào màn hình nên load ở init{} nữa sẽ bị trùng (chớp 2 lần)

    // getAllPending() (isUploaded=0, không lọc resize) — khớp với màn "Ảnh chưa upload"
    // để không báo "đã đồng bộ hết" trong khi ảnh vẫn còn chờ xử lý/resize.
    fun load() {
        viewModelScope.launch {
            val username = sessionManager.username.firstOrNull()
            val pending = photoRepository.getAllPending().size
            _uiState.update {
                it.copy(
                    username = username,
                    pendingUploadCount = pending,
                    currentVersionName = updateChecker.currentVersionName
                )
            }
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true, updateError = null) }
            try {
                val update = updateChecker.checkForUpdate()
                _uiState.update {
                    it.copy(
                        isCheckingUpdate = false,
                        availableUpdate = update,
                        updateError = if (update == null) "Bạn đang dùng phiên bản mới nhất" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCheckingUpdate = false, updateError = e.message) }
            }
        }
    }

    fun downloadUpdate() {
        val update = _uiState.value.availableUpdate ?: return
        val apkUrl = update.apkUrl ?: return
        _uiState.update { it.copy(isDownloadingUpdate = true) }
        updateChecker.downloadAndInstall(apkUrl, update.versionName ?: updateChecker.currentVersionName)
        _uiState.update { it.copy(isDownloadingUpdate = false) }
    }

    fun dismissUpdateError() = _uiState.update { it.copy(updateError = null) }

    fun triggerUpload() {
        _uiState.update { it.copy(isUploading = true) }
        // Resize trước — PhotoResizeWorker tự chain sang PhotoUploadWorker sau khi xong,
        // nên ảnh chưa resize cũng được xử lý ngay thay vì chờ worker định kỳ 15 phút
        PhotoResizeWorker.enqueueImmediate(context)
        // Reset flag sau 1 nhịp (worker chạy async)
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            val pending = photoRepository.getAllPending().size
            _uiState.update { it.copy(isUploading = false, pendingUploadCount = pending) }
        }
    }

    // Nút tay dự phòng — giống Xamarin AboutViewModel.Danhmuc(): đồng bộ lại toàn bộ danh mục
    // từ server, dùng khi cần dữ liệu mới ngay (không muốn chờ lần login kế tiếp)
    fun syncCatalog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingCatalog = true, catalogSyncMessage = null) }
            try {
                lookupRepository.refreshAll()
                _uiState.update { it.copy(isSyncingCatalog = false, catalogSyncMessage = "Đã đồng bộ danh mục") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncingCatalog = false, catalogSyncMessage = "Đồng bộ thất bại: ${e.message}") }
            }
        }
    }

    fun dismissCatalogSyncMessage() = _uiState.update { it.copy(catalogSyncMessage = null) }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clear()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }
}
