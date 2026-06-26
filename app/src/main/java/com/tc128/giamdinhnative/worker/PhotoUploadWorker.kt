package com.tc128.giamdinhnative.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.tc128.giamdinhnative.data.local.PhotoDao
import com.tc128.giamdinhnative.data.remote.ApiService
import com.tc128.giamdinhnative.data.remote.OcrService
import com.tc128.giamdinhnative.session.SessionManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "PhotoUploadWorker"

@HiltWorker
class PhotoUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val photoDao: PhotoDao,
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    private val ocrService: OcrService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork() started")
        val token = sessionManager.getToken()
        if (token.isNullOrBlank()) {
            Log.d(TAG, "No token — skip upload")
            return Result.success()
        }

        val pending = photoDao.getPendingUpload()
        if (pending.isEmpty()) {
            Log.d(TAG, "No pending photos")
            return Result.success()
        }

        Log.d(TAG, "Uploading ${pending.size} photos")
        var failCount = 0
        val plain = "text/plain".toMediaTypeOrNull()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

        for (photo in pending) {
            val file = photo.pathLocal?.let { File(it) }
            if (file == null || !file.exists()) {
                // File local đã mất — không còn gì để upload, dọn record luôn
                photoDao.delete(photo)
                continue
            }

            // containerNumber stores the numeric container ID as string (e.g. "16687")
            val containerNumericId = photo.containerNumber?.toIntOrNull()
            if (containerNumericId == null) {
                Log.w(TAG, "Photo ${photo.id} has no numeric containerId, skip")
                failCount++
                continue
            }

            try {
                setProgress(workDataOf(
                    KEY_PROGRESS to "Đang upload ${file.name}",
                    KEY_UPLOADED to pending.indexOf(photo),
                    KEY_TOTAL to pending.size
                ))

                val containerIdPart = containerNumericId.toString().toRequestBody(plain)
                val dateCreatePart = dateFormat.format(Date(photo.createdAt)).toRequestBody(plain)
                val statusPart = photo.status.toRequestBody(plain)
                val itemEorPart = photo.itemEorId?.toString()?.toRequestBody(plain)

                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("Image", file.name, requestFile)

                val responseBody = apiService.uploadPhoto(
                    containerId = containerIdPart,
                    dateCreate = dateCreatePart,
                    status = statusPart,
                    itemEorId = itemEorPart,
                    image = imagePart
                )
                responseBody.close()

                // Server trả về 2xx → ảnh đã có trên server. Giống Xamarin (PhotoService.UploadPhotoAsync):
                // chỉ đánh dấu isUploaded = true, KHÔNG xoá file/record ngay — giữ lại để xem offline,
                // dọn dẹp sau bằng cleanupOldUploaded() (ngưỡng 7 ngày, giống Xamarin DeleteItemsOverTime)
                photoDao.markUploaded(photo.id)
                Log.d(TAG, "Uploaded photo ${photo.id}, marked as uploaded")

                // Ảnh quét seal: gửi thêm 1 bản lên server OCR nội bộ để tập hợp dữ liệu huấn
                // luyện model seal riêng, thay dần ocr.space. Best-effort — lỗi ở đây KHÔNG được
                // làm fail việc upload ảnh chính (đã thành công ở trên rồi).
                if (photo.isSeal && !photo.isSealUploaded) {
                    runCatching {
                        ocrService.uploadSealForTraining(file, photo.sealNumber, photo.containerNumber)
                        photoDao.markSealUploaded(photo.id)
                    }.onFailure { Log.w(TAG, "Upload seal training data failed for photo ${photo.id}", it) }
                }

            } catch (e: Exception) {
                val detail = describeUploadError(e)
                Log.e(TAG, "Failed to upload photo ${photo.id}: $detail", e)
                photoDao.markUploadError(photo.id, detail)
                failCount++
            }
        }

        cleanupOldUploaded()

        return if (failCount > 0 && failCount == pending.size) Result.retry()
        else Result.success()
    }

    // Giống Xamarin DeleteItemsOverTime(): xoá ảnh đã upload quá 7 ngày để giải phóng dung lượng máy
    private suspend fun cleanupOldUploaded() {
        val threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val old = photoDao.getUploadedOlderThan(threshold)
        for (photo in old) {
            photo.pathLocal?.let { File(it).delete() }
            photoDao.delete(photo)
        }
        if (old.isNotEmpty()) Log.d(TAG, "Cleaned up ${old.size} old uploaded photos")
    }

    // Trích lỗi cụ thể để lưu vào DB — HTTP code + body (server thường trả lý do trong body)
    // cho HttpException, message gốc cho lỗi mạng (timeout, mất kết nối...)
    private fun describeUploadError(e: Exception): String = when (e) {
        is HttpException -> {
            val code = e.code()
            val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
                ?.take(300)
            "HTTP $code${if (!body.isNullOrBlank()) ": $body" else ""}"
        }
        is IOException -> "Lỗi mạng: ${e.message}"
        else -> e.message ?: e.javaClass.simpleName
    }

    companion object {
        const val KEY_PROGRESS = "progress_message"
        const val KEY_UPLOADED = "uploaded_count"
        const val KEY_TOTAL = "total_count"
        const val WORK_NAME = "photo_upload"

        fun enqueueImmediate(context: Context) {
            Log.d(TAG, "enqueueImmediate() called")
            val request = OneTimeWorkRequestBuilder<PhotoUploadWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(WORK_NAME)
                .build()
            // REPLACE — bấm "Upload ngay" phải luôn chạy lại, không bị kẹt vì request cũ
            // (KEEP sẽ no-op nếu còn 1 work cũ chưa kết thúc, ví dụ do mất mạng giữa lần upload trước)
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun schedulePeriodicUpload(context: Context) {
            val request = PeriodicWorkRequestBuilder<PhotoUploadWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(WORK_NAME)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "${WORK_NAME}_periodic", ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
