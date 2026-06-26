package com.tc128.giamdinhnative.ui.screens.images

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tc128.giamdinhnative.data.local.PhotoEntity
import com.tc128.giamdinhnative.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val BASE_URL = "https://tc128hp.hopto.org"
private const val BACKUP_BASE_URL = "https://tc128.synology.me:444"

data class ImagesUiState(
    val photos: List<PhotoEntity> = emptyList(),
    val serverUrls: List<String> = emptyList(),
    val isLoading: Boolean = true, // true ngay từ đầu — tránh chớp khung "rỗng" trước khi load() chạy xong
    val error: String? = null
)

@HiltViewModel
class ImagesViewModel @Inject constructor(
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImagesUiState())
    val uiState = _uiState.asStateFlow()

    private var currentContainerId: String? = null

    fun load(containerId: String) {
        currentContainerId = containerId
        val numericId = containerId.toIntOrNull()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val localDeferred = async { photoRepository.getByContainer(containerId) }
            val serverDeferred = async {
                if (numericId != null) photoRepository.getFromServer(numericId)
                else emptyList()
            }

            // Ảnh local đã upload (isUploaded=true) vẫn được giữ lại 7 ngày để xem offline (giống Xamarin
            // PhotoService.UploadPhotoAsync — không xoá ngay sau khi upload), nhưng đã xuất hiện trong
            // serverUrls rồi nên chỉ hiển thị ảnh local CHƯA upload để tránh trùng lặp.
            val localPhotos = localDeferred.await().filter { !it.isUploaded }
            val serverPhotos = serverDeferred.await()

            // Port từ Xamarin XemAnhViewModel.LoadItemId(): server trả Path là đường dẫn filesystem
            // tuyệt đối (chứa "wwwroot"), không phải URL — phải cắt phần sau "wwwroot" rồi nối base URL.
            // Một số ảnh cũ được backup sang domain Synology riêng (PathOfBackup chứa "AnhBackup").
            val serverUrls = serverPhotos.mapNotNull { photo ->
                val backupPath = photo.pathOfBackup
                val path = photo.path
                when {
                    !backupPath.isNullOrBlank() && backupPath.contains("AnhBackup") ->
                        BACKUP_BASE_URL + backupPath.substringAfter("AnhBackup").replace('\\', '/')
                    path == null -> null
                    path.contains("wwwroot") ->
                        BASE_URL + path.substringAfter("wwwroot").replace('\\', '/')
                    path.startsWith("http") -> path
                    else -> null
                }
            }

            _uiState.update {
                it.copy(isLoading = false, photos = localPhotos, serverUrls = serverUrls)
            }
        }
    }

    fun onPhotoCaptured(containerId: String, filePath: String) {
        viewModelScope.launch {
            photoRepository.saveLocal(
                containerNumber = containerId,
                itemEorId = null,
                filePath = filePath
            )
            load(containerId)
        }
    }

    fun deletePhoto(photo: PhotoEntity) {
        viewModelScope.launch {
            photoRepository.delete(photo)
            currentContainerId?.let { load(it) }
        }
    }
}
