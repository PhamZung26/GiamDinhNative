package com.tc128.giamdinhnative.ui.screens.images

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import com.tc128.giamdinhnative.data.local.PhotoEntity
import com.tc128.giamdinhnative.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

private const val BASE_URL = "https://tc128hp.hopto.org"
private const val BACKUP_BASE_URL = "https://tc128.synology.me:444"

data class ImagesUiState(
    val photos: List<PhotoEntity> = emptyList(),
    val serverUrls: List<String> = emptyList(),
    val isLoading: Boolean = true, // true ngay từ đầu — tránh chớp khung "rỗng" trước khi load() chạy xong
    val error: String? = null,
    val isSelectMode: Boolean = false,
    val selectedKeys: Set<String> = emptySet(),
    val isPreparingShare: Boolean = false,
    val shareUris: List<Uri>? = null // sự kiện one-shot — Composable phải gọi onShareHandled() sau khi xử lý
)

@HiltViewModel
class ImagesViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    @ApplicationContext private val context: Context
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

    fun enterSelectMode() {
        _uiState.update { it.copy(isSelectMode = true) }
    }

    fun exitSelectMode() {
        _uiState.update { it.copy(isSelectMode = false, selectedKeys = emptySet()) }
    }

    fun toggleSelect(key: String) {
        _uiState.update {
            val s = it.selectedKeys
            it.copy(selectedKeys = if (key in s) s - key else s + key)
        }
    }

    fun selectAll() {
        _uiState.update {
            val allKeys = it.photos.map { p -> "local_${p.id}" } + it.serverUrls.map { u -> "server_$u" }
            it.copy(selectedKeys = allKeys.toSet())
        }
    }

    fun deselectAll() {
        _uiState.update { it.copy(selectedKeys = emptySet()) }
    }

    fun onShareHandled() {
        _uiState.update { it.copy(shareUris = null) }
    }

    fun shareSelected() {
        val state = _uiState.value
        if (state.selectedKeys.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingShare = true) }
            val uris = withContext(Dispatchers.IO) {
                state.selectedKeys.mapNotNull { key ->
                    val file = when {
                        key.startsWith("local_") -> {
                            val id = key.removePrefix("local_").toLongOrNull()
                            state.photos.find { it.id == id }?.pathLocal
                                ?.let { path -> File(path).takeIf { it.exists() } }
                        }
                        key.startsWith("server_") -> downloadServerImageToCache(key.removePrefix("server_"))
                        else -> null
                    }
                    file?.let { fileToShareUri(it) }
                }
            }
            _uiState.update { it.copy(isPreparingShare = false, shareUris = uris) }
        }
    }

    private fun fileToShareUri(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private suspend fun downloadServerImageToCache(url: String): File? {
        val cacheDir = File(context.externalCacheDir ?: context.cacheDir, "shared_images").apply { mkdirs() }
        val outFile = File(cacheDir, "share_${url.hashCode()}.jpg")
        if (outFile.exists() && outFile.length() > 0) return outFile

        val imageLoader = context.imageLoader
        val request = coil.request.ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()
        val result = imageLoader.execute(request)
        val bitmap = (result as? coil.request.SuccessResult)
            ?.drawable?.let { it as? android.graphics.drawable.BitmapDrawable }?.bitmap
            ?: return null
        return try {
            FileOutputStream(outFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }
            outFile
        } catch (e: Exception) {
            null
        }
    }
}
