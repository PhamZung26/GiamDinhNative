package com.tc128.giamdinhnative.ui.screens.camera

import android.content.Context
import androidx.lifecycle.ViewModel
import com.tc128.giamdinhnative.data.repository.PhotoRepository
import com.tc128.giamdinhnative.di.ApplicationScope
import com.tc128.giamdinhnative.worker.PhotoResizeWorker
import com.tc128.giamdinhnative.worker.UpdateCleanDateWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val appScope: CoroutineScope
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
        // Lên lịch xác nhận vệ sinh NGAY LẬP TỨC (đồng bộ, không phụ thuộc saveLocal hay lifecycle).
        // enqueueUniqueWork không phải suspend — phải gọi trước mọi điểm suspend, nếu để sau
        // saveLocal (có bước copy ảnh vào gallery, chậm) mà user bấm Back ngay thì scope bị hủy
        // giữa chừng, worker không bao giờ được lên lịch → server không nhận vệ sinh (lỗi cũ).
        if (updateCleanDate) {
            containerId.toIntOrNull()?.let { UpdateCleanDateWorker.enqueue(context, it) }
        }

        // Lưu ảnh + lên lịch resize/upload chạy ở application scope (KHÔNG phải viewModelScope) để
        // không bị hủy khi màn Camera rời back stack — bảo đảm ảnh luôn được lưu và đẩy đi dù user
        // thoát ngay sau khi chụp.
        appScope.launch {
            val status = photoStatus ?: if (itemEorId != null) "PostRepair" else "Available"
            photoRepository.saveLocal(
                containerNumber = containerId,
                itemEorId = itemEorId,
                filePath = filePath,
                status = status
            )
            // Đẩy resize→upload ngay cho MỌI ảnh (không chỉ vệ sinh) — trước đây ảnh giám định thường
            // phải chờ periodic 15', gộp với batch nhỏ khiến "chỉ upload được vài chục ảnh". Worker
            // dùng unique-work KEEP nên gọi nhiều lần cũng chỉ 1 luồng chạy, không gây trùng.
            PhotoResizeWorker.enqueueImmediate(context)
        }
    }
}
