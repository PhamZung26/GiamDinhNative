package com.tc128.giamdinhnative.ui.screens.camera

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.tc128.giamdinhnative.data.repository.PhotoRepository
import com.tc128.giamdinhnative.di.ApplicationScope
import com.tc128.giamdinhnative.util.ImageResizer
import com.tc128.giamdinhnative.worker.PhotoResizeWorker
import com.tc128.giamdinhnative.worker.UpdateCleanDateWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Nhập ảnh từ bộ sưu tập (thay cho chụp ảnh). Xử lý giống CameraViewModel.onPhotoCaptured:
 * lưu ảnh + xếp hàng resize/upload; nếu là ảnh vệ sinh (updateCleanDate) thì cũng xác nhận vệ sinh.
 * Dùng cho màn Chi tiết container (DM/AV) và màn Vệ sinh.
 */
@HiltViewModel
class GalleryImportViewModel @Inject constructor(
    private val imageResizer: ImageResizer,
    private val photoRepository: PhotoRepository,
    @ApplicationScope private val appScope: CoroutineScope,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _importing = MutableStateFlow(false)
    val importing = _importing.asStateFlow()

    fun import(
        uris: List<Uri>,
        containerId: String,
        itemEorId: Int?,
        photoStatus: String,
        updateCleanDate: Boolean
    ) {
        if (uris.isEmpty()) return

        // Xác nhận vệ sinh NGAY (đồng bộ, không phụ thuộc lifecycle) — giống CameraViewModel.
        if (updateCleanDate) {
            containerId.toIntOrNull()?.let { UpdateCleanDateWorker.enqueue(context, it) }
        }

        _importing.value = true
        // Chạy ở application scope để không bị hủy khi màn hình rời đi
        appScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    for (uri in uris) {
                        runCatching {
                            // resizeToBytes: đọc EXIF + xoay đúng chiều + resize theo cấu hình
                            val bytes = imageResizer.resizeToBytes(uri)
                            val file = imageResizer.writeJpegFile(bytes)
                            photoRepository.saveLocal(
                                containerNumber = containerId,
                                itemEorId = itemEorId,
                                filePath = file.absolutePath,
                                status = photoStatus
                            )
                        }
                    }
                }
                PhotoResizeWorker.enqueueImmediate(context)
            } finally {
                _importing.value = false
            }
        }
    }
}
