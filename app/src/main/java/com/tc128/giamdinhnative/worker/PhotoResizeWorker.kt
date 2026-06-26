package com.tc128.giamdinhnative.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.tc128.giamdinhnative.data.local.PhotoDao
import com.tc128.giamdinhnative.util.ImageResizer
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
        val pending = photoDao.getPendingResize()
        if (pending.isEmpty()) {
            Log.d(TAG, "No pending resize")
            PhotoUploadWorker.enqueueImmediate(applicationContext)
            return Result.success()
        }

        Log.d(TAG, "Resizing ${pending.size} photos")
        var failCount = 0

        for (photo in pending) {
            val file = photo.pathLocal?.let { File(it) }
            if (file == null || !file.exists()) {
                // File mất — đánh dấu resized để bỏ qua ở bước upload
                photoDao.markResized(photo.id)
                continue
            }
            try {
                val newSize = imageResizer.resizeFile(file)
                photoDao.markResized(photo.id)
                Log.d(TAG, "Resized photo ${photo.id} → ${newSize / 1024}KB")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resize photo ${photo.id}", e)
                failCount++
            }
        }

        return if (failCount > 0 && failCount == pending.size) {
            Result.retry()
        } else {
            // Chain sang upload sau khi resize xong
            PhotoUploadWorker.enqueueImmediate(applicationContext)
            Result.success()
        }
    }

    companion object {
        const val WORK_NAME = "photo_resize"

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
