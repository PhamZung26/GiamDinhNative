package com.tc128.giamdinhnative.data.remote

import android.net.Uri
import com.tc128.giamdinhnative.util.ImageResizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class OcrResponse(
    @SerialName("container_number") val containerNumber: String? = null,
    @SerialName("size_type") val sizeType: String? = null,
    @SerialName("seal_number") val sealNumber: String? = null,
    @SerialName("is_confirmed") val isConfirmed: String? = null,
    @SerialName("processing_time") val processingTime: Double? = null
)

/** Kết quả OCR kèm timing chi tiết từng bước (đơn vị ms) */
data class OcrTimedResult(
    val response: OcrResponse,
    val resizeMs: Long,   // resize + xoay EXIF
    val uploadMs: Long,   // toàn bộ HTTP round-trip (bao gồm server processing)
    val serverMs: Long    // server xử lý (từ processingTime trong response)
) {
    val networkMs: Long get() = uploadMs - serverMs  // thời gian mạng thuần (không tính server)
}

data class SealScanResult(val sealNo: String, val filePath: String)

@Singleton
class OcrService @Inject constructor(
    private val imageResizer: ImageResizer,
    private val okHttpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Nhận bytes thô từ CameraX, trả về kết quả kèm timing chi tiết */
    suspend fun scanBytes(bytes: ByteArray, rotationDegrees: Int): OcrTimedResult =
        withContext(Dispatchers.IO) {
            val t0 = System.currentTimeMillis()
            val resized = imageResizer.resizeBytes(bytes, rotationDegrees)
            val resizeMs = System.currentTimeMillis() - t0

            val t1 = System.currentTimeMillis()
            val response = upload(resized)
            val uploadMs = System.currentTimeMillis() - t1

            val serverMs = ((response.processingTime ?: 0.0) * 1000).toLong()
            OcrTimedResult(response, resizeMs, uploadMs, serverMs)
        }

    suspend fun scanImage(imageUri: Uri): OcrTimedResult = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()
        val resized = imageResizer.resizeToBytes(imageUri)
        val resizeMs = System.currentTimeMillis() - t0

        val t1 = System.currentTimeMillis()
        val response = upload(resized)
        val uploadMs = System.currentTimeMillis() - t1

        val serverMs = ((response.processingTime ?: 0.0) * 1000).toLong()
        OcrTimedResult(response, resizeMs, uploadMs, serverMs)
    }

    private fun upload(bytes: ByteArray): OcrResponse {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "scan.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
            .build()
        val request = Request.Builder()
            .url("https://ocr.phamdung.uk/extractinfo/")
            .post(requestBody)
            .build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("OCR lỗi: ${response.code}")
        val body = response.body?.string() ?: throw IOException("Không có kết quả OCR")
        return json.decodeFromString(body)
    }

    /**
     * Quét số seal qua ocr.space (port từ Xamarin PhotoService.ScanSealByOCRSpace).
     * Xamarin gọi TakePic() trước khi OCR nên ảnh quét seal cũng được lưu thành ảnh container —
     * trả kèm đường dẫn file đã lưu để caller gọi PhotoRepository.saveLocal().
     */
    suspend fun scanSeal(bytes: ByteArray, rotationDegrees: Int): SealScanResult = withContext(Dispatchers.IO) {
        val resized = imageResizer.resizeBytes(bytes, rotationDegrees)
        val savedFile = imageResizer.writeJpegFile(resized)
        val base64 = Base64.getEncoder().encodeToString(resized)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("language", "eng")
            .addFormDataPart("isOverlayRequired", "false")
            .addFormDataPart("iscreatesearchablepdf", "false")
            .addFormDataPart("issearchablepdfhidetextlayer", "false")
            .addFormDataPart("filetype", "jpg")
            .addFormDataPart("OCREngine", "2")
            .addFormDataPart("base64Image", "data:image/jpeg;base64,$base64")
            .build()
        val request = Request.Builder()
            .url("https://api.ocr.space/parse/image")
            .addHeader("apikey", "K84768557488957")
            .post(requestBody)
            .build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("OCR lỗi: ${response.code}")
        val body = response.body?.string() ?: throw IOException("Không có kết quả OCR")

        val parsedText = Json.parseToJsonElement(body).jsonObject["ParsedResults"]
            ?.jsonArray?.firstOrNull()?.jsonObject?.get("ParsedText")
            ?.jsonPrimitive?.content ?: ""

        // Giống Xamarin: chọn dòng dài nhất có chứa số và không chứa "/" hoặc "."
        val sealNo = parsedText.split("\n")
            .filter { line -> line.any { it.isDigit() } && !line.contains("/") && !line.contains(".") }
            .maxByOrNull { it.length }
            ?.trim()
            ?: ""

        SealScanResult(sealNo, savedFile.absolutePath)
    }

    /**
     * Gửi 1 bản ảnh seal lên server OCR nội bộ (cùng project scan số container) để tập hợp
     * dữ liệu huấn luyện model seal riêng — mục đích thay dần ocr.space. Không trả về kết quả
     * nhận diện gì cả, chỉ lưu lại; lỗi ở đây không được làm fail luồng upload ảnh chính.
     */
    suspend fun uploadSealForTraining(file: java.io.File, sealNumber: String?, containerNo: String?) =
        withContext(Dispatchers.IO) {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.asRequestBody("image/jpeg".toMediaType()))
                .apply {
                    sealNumber?.let { addFormDataPart("seal_number", it) }
                    containerNo?.let { addFormDataPart("container_no", it) }
                }
                .build()
            val request = Request.Builder()
                .url("https://ocr.phamdung.uk/uploadseal/")
                .post(requestBody)
                .build()
            val response = okHttpClient.newCall(request).execute()
            response.close()
            if (!response.isSuccessful) throw IOException("Upload seal lỗi: ${response.code}")
        }

    // Ánh xạ mã size từ OCR sang prefix CodeName trong catalog Size — port nguyên từ
    // Xamarin NhapContViewModel.UpdateSizeSelected(). Trả về prefix để caller dùng
    // sizes.find { it.codeName.startsWith(prefix) }, KHÔNG phải mã ISO chuẩn.
    fun mapSizeCode(ocrSizeType: String?): String? {
        val upper = ocrSizeType?.uppercase()?.trim() ?: return null
        return when {
            upper.startsWith("22G") -> "220"
            upper.startsWith("25G") -> "250"
            upper.startsWith("42G") -> "420"
            upper.startsWith("45G") -> "450"
            upper.startsWith("45R") -> "453"
            upper.startsWith("42R") -> "423"
            upper.startsWith("25R") -> "253"
            upper.startsWith("L5G") -> "L50"
            upper.startsWith("L5R") -> "L53"
            else -> null
        }
    }
}
