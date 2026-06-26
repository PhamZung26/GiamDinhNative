package com.tc128.giamdinhnative.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageResizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Resize ảnh từ content URI → ByteArray JPEG.
     * Dùng cho OCR upload.
     */
    fun resizeToBytes(imageUri: Uri, maxDim: Int = 1280): ByteArray {
        val resolver = context.contentResolver
        val rotation = resolver.openInputStream(imageUri)?.use { readExifRotation(it) } ?: 0f

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(imageUri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        val sampleSize = calcSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val sampled = resolver.openInputStream(imageUri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Không đọc được ảnh từ URI")

        return compressToBytes(sampled, rotation, maxDim)
    }

    /**
     * Resize file ảnh và ghi đè lên chính file đó.
     * Dùng cho batch resize trước khi upload.
     * @return kích thước file sau resize (bytes)
     */
    fun resizeFile(file: File, maxDim: Int = 1280): Long {
        val rotation = file.inputStream().use { readExifRotation(it) }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        file.inputStream().use { BitmapFactory.decodeStream(it, null, bounds) }

        val sampleSize = calcSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val sampled = file.inputStream().use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Không đọc được file: ${file.path}")

        val bytes = compressToBytes(sampled, rotation, maxDim)
        FileOutputStream(file).use { it.write(bytes) }
        return file.length()
    }

    /**
     * Resize bytes thô từ CameraX (không cần URI/File).
     * rotationDegrees từ ImageProxy.imageInfo.rotationDegrees.
     */
    fun resizeBytes(bytes: ByteArray, rotationDegrees: Int, maxDim: Int = 1280): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        val sampleSize = calcSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val sampled = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: error("Không decode được ảnh từ bytes")

        return compressToBytes(sampled, rotationDegrees.toFloat(), maxDim)
    }

    /** Ghi 1 mảng bytes JPEG đã resize ra file trong cache dir — dùng để lưu lại ảnh quét (seal/cont) làm ảnh container */
    fun writeJpegFile(bytes: ByteArray): File {
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss_SSS", java.util.Locale.getDefault())
            .format(System.currentTimeMillis())
        val dir = context.externalCacheDir ?: context.cacheDir
        val file = File(dir, "IMG_$timestamp.jpg")
        FileOutputStream(file).use { it.write(bytes) }
        return file
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private fun readExifRotation(stream: java.io.InputStream): Float {
        val exif = ExifInterface(stream)
        return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90  -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }

    private fun calcSampleSize(width: Int, height: Int, maxDim: Int): Int {
        val rawMax = maxOf(width, height)
        var size = 1
        while (rawMax / (size * 2) >= maxDim) size *= 2
        return size
    }

    private fun compressToBytes(sampled: Bitmap, rotation: Float, maxDim: Int): ByteArray {
        // Xoay đúng chiều theo EXIF
        val oriented = if (rotation != 0f) {
            val matrix = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(sampled, 0, 0, sampled.width, sampled.height, matrix, true)
                .also { if (it !== sampled) sampled.recycle() }
        } else sampled

        // Scale chính xác về maxDim (inSampleSize chỉ dùng lũy thừa 2)
        val scale = maxDim.toFloat() / maxOf(oriented.width, oriented.height)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                oriented,
                (oriented.width * scale).toInt(),
                (oriented.height * scale).toInt(),
                true
            ).also { if (it !== oriented) oriented.recycle() }
        } else oriented

        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            scaled.recycle()
            out.toByteArray()
        }
    }
}
