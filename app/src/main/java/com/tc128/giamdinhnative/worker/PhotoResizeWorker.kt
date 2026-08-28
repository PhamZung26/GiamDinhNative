package com.tc128.giamdinhnative.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.tc128.giamdinhnative.data.local.PhotoDao
import com.tc128.giamdinhnative.util.ImageResizer
import com.tc128.giamdinhnative.util.UploadNotifications
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.concurrent.TimeUnit

private const val TAG = "PhotoResizeWorker"

/**
 * Resize batch ảnh trước khi upload.
 * Không cần mạng — chạy hoàn toàn local.
 * Sau khi hoàn tất, tự động chain sang PhotoUploadWorker.
 */
@HiltWorker
class PhotoResizeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val photoDao: PhotoDao,
    private val imageResizer: ImageResizer
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        var totalSuccess = 0
        var totalFail = 0

        // Resize 500 ảnh (decode+scale bitmap) nặng CPU, có thể vượt 10 phút → foreground khi nhiều
        val totalToResize = runCatching { photoDao.countPendingResize() }.getOrDefault(0)
        if (totalToResize > FOREGROUND_THRESHOLD) {
            runCatching { setForeground(buildForegroundInfo()) }
                .onFailure { Log.w(TAG, "setForeground failed", it) }
        }

        // Rút cạn TẤT CẢ ảnh chờ resize theo từng lô (getPendingResize LIMIT 20) tới khi hết —
        // trước đây chỉ resize 1 lô 20 ảnh/lần chạy, phần còn lại phải chờ periodic 15'.
        while (true) {
            val pending = photoDao.getPendingResize()
            if (pending.isEmpty()) break

            Log.d(TAG, "Resizing ${pending.size} photos")
            var batchProgress = 0

            for (photo in pending) {
                val file = photo.pathLocal?.let { File(it) }
                if (file == null || !file.exists()) {
                    // File mất — đánh dấu resized để bỏ qua ở bước upload
                    photoDao.markResized(photo.id)
                    batchProgress++
                    continue
                }
                try {
                    val newSize = imageResizer.resizeFile(file)
                    photoDao.markResized(photo.id)
                    batchProgress++
                    totalSuccess++
                    Log.d(TAG, "Resized photo ${photo.id} → ${newSize / 1024}KB")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to resize photo ${photo.id}", e)
                    totalFail++
                }
            }

            // Cả lô không tiến triển (đều lỗi) → dừng tránh lặp vô hạn
            if (batchProgress == 0) break
        }

        // Luôn chain sang upload (kể cả khi không có gì để resize — để đẩy các ảnh đã resize sẵn)
        PhotoUploadWorker.enqueueImmediate(applicationContext)

        return if (totalSuccess == 0 && totalFail > 0) Result.retry() else Result.success()
    }

    private fun buildForegroundInfo(): ForegroundInfo {
        val notif = UploadNotifications.build(applicationContext, "Đang xử lý ảnh…", 0, 0)
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ForegroundInfo(
                UploadNotifications.NOTIF_ID_RESIZE,
                notif,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(UploadNotifications.NOTIF_ID_RESIZE, notif)
        }
    }

    companion object {
        const val WORK_NAME = "photo_resize"
        private const val FOREGROUND_THRESHOLD = 10

        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<PhotoResizeWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
        }

        fun schedulePeriodicResize(context: Context) {
            val request = PeriodicWorkRequestBuilder<PhotoResizeWorker>(15, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "${WORK_NAME}_periodic",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
