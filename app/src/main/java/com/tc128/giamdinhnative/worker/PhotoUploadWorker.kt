package com.tc128.giamdinhnative.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.tc128.giamdinhnative.data.local.PhotoDao
import com.tc128.giamdinhnative.data.remote.ApiService
import com.tc128.giamdinhnative.data.remote.OcrService
import com.tc128.giamdinhnative.session.SessionManager
import com.tc128.giamdinhnative.util.UploadNotifications
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
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
        Log.d(TAG, "doWork() started (attempt=$runAttemptCount)")
        val token = sessionManager.getToken()
        if (token.isNullOrBlank()) {
            Log.d(TAG, "No token — skip upload")
            return Result.success()
        }

        // Nếu nhiều ảnh → chạy foreground để KHÔNG bị cắt bởi giới hạn ~10 phút / HyperOS kill.
        val totalToUpload = runCatching { photoDao.countPendingUpload() }.getOrDefault(0)
        if (totalToUpload > FOREGROUND_THRESHOLD) {
            runCatching { setForeground(buildForegroundInfo(0, totalToUpload)) }
                .onFailure { Log.w(TAG, "setForeground failed", it) }
        }

        val plain = "text/plain".toMediaTypeOrNull()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

        var totalSuccess = 0
        var totalFail = 0
        var uploadedSoFar = 0
        var stalledRounds = 0

        // Rút cạn TẤT CẢ ảnh chờ upload theo từng lô (LIMIT 10) tới khi hết. Nếu 1 lô lỗi hết
        // (mạng chập chờn / NAS quá tải nhất thời) thì KHÔNG dừng hẳn — chờ ngắn rồi thử lại vài
        // lần; hết kiên nhẫn mới thoát và để phần cuối quyết định retry (tự chạy lại), tránh cảnh
        // "upload được một ít rồi dừng, phải bấm lại".
        while (!isStopped) {
            val pending = photoDao.getPendingUpload(sessionManager.cachedMaxUploadCount)
            if (pending.isEmpty()) break

            var batchProgress = 0

            for (photo in pending) {
                if (isStopped) break
                val file = photo.pathLocal?.let { File(it) }
                if (file == null || !file.exists()) {
                    photoDao.delete(photo)   // file local đã mất — dọn record
                    batchProgress++
                    continue
                }

                // containerNumber lưu ID số container dạng String (vd "16687")
                val containerNumericId = photo.containerNumber?.toIntOrNull()
                if (containerNumericId == null) {
                    Log.w(TAG, "Photo ${photo.id} has no numeric containerId, skip")
                    photoDao.markUploadError(photo.id, "containerId không hợp lệ")
                    totalFail++
                    continue
                }

                try {
                    setProgress(workDataOf(
                        KEY_PROGRESS to "Đang upload ${file.name}",
                        KEY_UPLOADED to uploadedSoFar,
                        KEY_TOTAL to totalToUpload
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

                    // 2xx → chỉ đánh dấu isUploaded (giống Xamarin), dọn file sau 7 ngày.
                    photoDao.markUploaded(photo.id)
                    batchProgress++
                    totalSuccess++
                    uploadedSoFar++

                    // Cập nhật tiến độ notification foreground
                    if (totalToUpload > FOREGROUND_THRESHOLD) {
                        runCatching { setForeground(buildForegroundInfo(uploadedSoFar, totalToUpload)) }
                    }

                    // Ảnh quét seal: gửi thêm 1 bản lên server OCR nội bộ (best-effort)
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
                    totalFail++
                }
            }

            if (batchProgress == 0) {
                // Lô này không tiến triển (đều lỗi mạng tạm) — chờ rồi thử lại, chưa bỏ cuộc ngay
                stalledRounds++
                if (stalledRounds >= MAX_STALLED_ROUNDS) break
                delay(STALL_RETRY_DELAY_MS)
            } else {
                stalledRounds = 0
            }
        }

        cleanupOldUploaded()

        // Còn ảnh pending (lô lỗi kéo dài / bị hệ thống thu hồi) → tự chạy lại (retry) thay vì bắt
        // người dùng bấm tay. Chỉ bỏ cuộc (success → chờ periodic 15') nếu đã thử nhiều lần mà
        // KHÔNG upload thêm được gì (tránh retry vô hạn khi có lỗi cố hữu).
        val stillPending = runCatching { photoDao.countPendingUpload() > 0 }.getOrDefault(false)
        return when {
            !stillPending -> Result.success()
            totalSuccess > 0 || runAttemptCount < MAX_RUN_ATTEMPTS -> Result.retry()
            else -> Result.success()
        }
    }

    private fun buildForegroundInfo(done: Int, total: Int): ForegroundInfo {
        val notif = UploadNotifications.build(applicationContext, "Đang tải ảnh lên máy chủ", done, total)
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ForegroundInfo(
                UploadNotifications.NOTIF_ID_UPLOAD,
                notif,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(UploadNotifications.NOTIF_ID_UPLOAD, notif)
        }
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

        private const val FOREGROUND_THRESHOLD = 10   // >10 ảnh → chạy foreground
        private const val MAX_STALLED_ROUNDS = 3      // số lần thử lại 1 lô lỗi trong cùng phiên
        private const val STALL_RETRY_DELAY_MS = 4000L
        private const val MAX_RUN_ATTEMPTS = 8        // số lần WorkManager retry trước khi nhường periodic

        fun enqueueImmediate(context: Context) {
            Log.d(TAG, "enqueueImmediate() called")
            val request = OneTimeWorkRequestBuilder<PhotoUploadWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                // LINEAR 15s để retry nhanh, không giãn theo cấp số nhân khi rút cạn nhiều đợt
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.SECONDS)
                .addTag(WORK_NAME)
                .build()
            // KEEP — khi đã có 1 worker "photo_upload" đang chạy/chờ, các lần bấm thêm là no-op
            // (worker đang chạy vốn quét hết ảnh pending). Tránh sinh worker thứ hai chạy song song
            // hoặc hủy worker đang gửi dở giữa chừng → chống upload trùng khi bấm nhiều lần.
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
