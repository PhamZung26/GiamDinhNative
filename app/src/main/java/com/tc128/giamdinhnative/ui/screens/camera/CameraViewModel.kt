package com.tc128.giamdinhnative.ui.screens.camera

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tc128.giamdinhnative.data.repository.ContainerRepository
import com.tc128.giamdinhnative.data.repository.PhotoRepository
import com.tc128.giamdinhnative.worker.PhotoResizeWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val containerRepository: ContainerRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // photoStatus truyền rõ từ màn gọi (DM→PreRepair, AV/VS→Available). Nếu không truyền,
    // suy ra từ itemEorId: != null → ảnh hư hỏng gắn vào 1 ItemEOR cụ thể (PostRepair), null → Available.
    fun onPhotoCaptured(
        containerId: String,
        itemEorId: Int?,
        filePath: String,
        photoStatus: String? = null,
        updateCleanDate: Boolean = false
    ) {
        viewModelScope.launch {
            val status = photoStatus ?: if (itemEorId != null) "PostRepair" else "Available"
            photoRepository.saveLocal(
                containerNumber = containerId,
                itemEorId = itemEorId,
                filePath = filePath,
                status = status
            )
            if (updateCleanDate) {
                // Ảnh vệ sinh: backend xử lý xác nhận vệ sinh dựa trên ảnh đã nhận được, nên cần
                // upload ngay (không chờ periodic worker 15 phút như ảnh giám định thường).
                // Khớp Xamarin: timer nền upload toàn bộ ảnh chưa gửi chạy mỗi 1 phút — không có
                // luồng "upload tức thì" riêng cho ảnh vệ sinh, nhưng app này cần phản hồi nhanh hơn
                // để backend xác nhận vệ sinh xong gần như ngay sau khi chụp.
                PhotoResizeWorker.enqueueImmediate(context)

                // Port từ Xamarin ChupAnhVeSinh(): chụp ảnh vệ sinh xong, nếu container chưa có
                // DateTimeClean thì cập nhật luôn ngày vệ sinh hiện tại
                val numericId = containerId.toIntOrNull() ?: return@launch
                runCatching {
                    val container = containerRepository.getContainer(numericId)
                    if (container.dateTimeClean.isNullOrBlank()) {
                        containerRepository.uploadCleanDateTime(numericId)
                    }
                }
            }
            // Ảnh giám định thường vẫn để periodic worker xử lý (15 phút / lần)
            // không enqueue ngay để tránh cạnh tranh tài nguyên với camera
        }
    }
}
