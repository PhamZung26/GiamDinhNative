package com.tc128.giamdinhnative.data.repository

import android.content.Context
import android.util.Log
import com.tc128.giamdinhnative.data.local.PhotoDao
import com.tc128.giamdinhnative.data.local.PhotoEntity
import com.tc128.giamdinhnative.data.model.Photo
import com.tc128.giamdinhnative.data.remote.ApiService
import com.tc128.giamdinhnative.util.MediaStoreSaver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoRepository @Inject constructor(
    private val photoDao: PhotoDao,
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) {
    suspend fun saveLocal(
        containerNumber: String?,
        itemEorId: Int?,
        filePath: String,
        status: String = "Available",
        isSeal: Boolean = false,
        sealNumber: String? = null
    ): Long {
        val entity = PhotoEntity(
            containerNumber = containerNumber,
            itemEorId = itemEorId,
            pathLocal = filePath,
            status = status,
            isSeal = isSeal,
            sealNumber = sealNumber
        )
        val id = photoDao.insert(entity)
        backupToGallery(containerNumber, filePath)
        return id
    }

    // Sao 1 bản vào Pictures công khai của hệ điều hành — lấy lại được ảnh qua app Gallery
    // nếu app bị lỗi/mất dữ liệu local, độc lập với pipeline resize/upload nội bộ của app
    private suspend fun backupToGallery(containerNumber: String?, filePath: String) = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) return@withContext
        val displayName = "${containerNumber ?: "unknown"}_${file.name}"
        MediaStoreSaver.saveToGallery(context, file, displayName)
    }

    suspend fun getByContainer(containerNumber: String): List<PhotoEntity> =
        photoDao.getByContainer(containerNumber)

    suspend fun getByItemEor(itemEorId: Int): List<PhotoEntity> =
        photoDao.getByItemEor(itemEorId)

    suspend fun getPendingResize(): List<PhotoEntity> =
        photoDao.getPendingResize()

    suspend fun markResized(id: Long) =
        photoDao.markResized(id)

    suspend fun getPendingUpload(): List<PhotoEntity> =
        photoDao.getPendingUpload()

    suspend fun getAllPending(): List<PhotoEntity> =
        photoDao.getAllPending()

    suspend fun delete(photo: PhotoEntity) = photoDao.delete(photo)

    suspend fun markUploaded(id: Long) = photoDao.markUploaded(id)

    suspend fun markSealUploaded(id: Long) = photoDao.markSealUploaded(id)

    suspend fun getUploadedOlderThan(thresholdMillis: Long): List<PhotoEntity> =
        photoDao.getUploadedOlderThan(thresholdMillis)

    suspend fun getFromServer(containerId: Int): List<Photo> =
        runCatching { apiService.getPhotosByContainer(containerId) }
            .onFailure { Log.e("PhotoRepository", "getFromServer($containerId) failed", it) }
            .getOrDefault(emptyList())
}
