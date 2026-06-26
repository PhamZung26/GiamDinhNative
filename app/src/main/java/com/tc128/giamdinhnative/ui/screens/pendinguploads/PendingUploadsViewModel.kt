package com.tc128.giamdinhnative.ui.screens.pendinguploads

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tc128.giamdinhnative.data.local.PhotoEntity
import com.tc128.giamdinhnative.data.repository.PhotoRepository
import com.tc128.giamdinhnative.worker.PhotoResizeWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PendingUploadsUiState(
    val photos: List<PhotoEntity> = emptyList(),
    val isLoading: Boolean = true, // true ngay từ đầu — tránh chớp khung "rỗng" trước khi load() chạy xong
    val isUploading: Boolean = false
)

@HiltViewModel
class PendingUploadsViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PendingUploadsUiState())
    val uiState = _uiState.asStateFlow()

    // Không load ở đây — PendingUploadsScreen gọi load() qua repeatOnLifecycle(RESUMED),
    // tự fire ngay lần đầu vào màn hình nên load ở init{} nữa sẽ bị trùng (chớp 2 lần)

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val photos = photoRepository.getAllPending()
            _uiState.update { it.copy(isLoading = false, photos = photos) }
        }
    }

    fun triggerUpload() {
        _uiState.update { it.copy(isUploading = true) }
        // Resize trước — PhotoResizeWorker tự chain sang PhotoUploadWorker sau khi xong,
        // nên ảnh chưa resize (trạng thái "Đang chờ xử lý ảnh") cũng được xử lý ngay
        PhotoResizeWorker.enqueueImmediate(context)
        viewModelScope.launch {
            delay(3500) // worker chạy async — chờ 1 nhịp rồi reload để thấy kết quả
            load()
            _uiState.update { it.copy(isUploading = false) }
        }
    }

    fun deletePhoto(photo: PhotoEntity) {
        viewModelScope.launch {
            photoRepository.delete(photo)
            load()
        }
    }
}
