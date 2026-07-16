package com.tc128.giamdinhnative.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.tc128.giamdinhnative.data.repository.ContainerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "UpdateCleanDateWorker"
private const val KEY_CONTAINER_ID = "containerId"

/**
 * Xác nhận container đã vệ sinh (PUT api/containerv2/UploadCleanDateTime) sau khi chụp ảnh vệ sinh.
 *
 * Chạy qua WorkManager thay vì gọi trực tiếp trong viewModelScope của màn Camera: nếu chạy trong
 * viewModelScope, người dùng bấm Back ngay sau khi chụp (thói quen phổ biến) sẽ huỷ NavBackStackEntry
 * của màn Camera trước khi 2 API tuần tự (getContainer + uploadCleanDateTime) kịp hoàn thành —
 * backend không bao giờ nhận được xác nhận vệ sinh dù ảnh vẫn upload bình thường (ảnh đi qua
 * WorkManager riêng, không phụ thuộc lifecycle màn hình). WorkManager sống độc lập với màn hình
 * nên đảm bảo request được gửi dù người dùng đã rời màn Camera.
 */
@HiltWorker
class UpdateCleanDateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val containerRepository: ContainerRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val containerId = inputData.getInt(KEY_CONTAINER_ID, -1)
        if (containerId <= 0) return Result.failure()

        return try {
            // Port từ Xamarin ChupAnhVeSinh(): chỉ set DateTimeClean nếu container chưa có
            val container = containerRepository.getContainer(containerId)
            if (container.dateTimeClean.isNullOrBlank()) {
                containerRepository.uploadCleanDateTime(containerId)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update clean date for container $containerId", e)
            Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context, containerId: Int) {
            val request = OneTimeWorkRequestBuilder<UpdateCleanDateWorker>()
                .setInputData(workDataOf(KEY_CONTAINER_ID to containerId))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            // REPLACE — nếu container được chụp vệ sinh nhiều lần liên tiếp, chỉ cần request mới nhất
            // (idempotent: cùng set DateTimeClean, không hại gì nếu request cũ bị thay)
            WorkManager.getInstance(context).enqueueUniqueWork(
                "update_clean_date_$containerId",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
