package com.tc128.giamdinhnative.ui.screens.camera

import androidx.camera.core.ImageCapture
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tc128.giamdinhnative.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lưu/đọc các tuỳ chọn camera cần nhớ giữa các lần mở (hiện tại: trạng thái flash).
 * Dùng chung cho màn chụp ảnh (CameraScreen) và màn quét số container (OcrCameraDialog).
 */
@HiltViewModel
class CameraSettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    // -1 = chưa nạp xong; UI chờ giá trị >= 0 rồi mới áp vào flashMode cục bộ
    private val _flashMode = MutableStateFlow(-1)
    val flashMode = _flashMode.asStateFlow()

    init {
        viewModelScope.launch {
            _flashMode.value = sessionManager.getLastFlashMode()
        }
    }

    fun setFlashMode(mode: Int) {
        _flashMode.value = mode
        viewModelScope.launch { sessionManager.saveLastFlashMode(mode) }
    }

    // Kích cỡ resize người dùng cấu hình — dùng cho bước lưu ảnh trung gian ở CameraScreen
    val resizeMaxDim: Int get() = sessionManager.cachedResizeMaxDim

    companion object {
        const val UNLOADED = -1
        val DEFAULT_FLASH = ImageCapture.FLASH_MODE_AUTO
    }
}
