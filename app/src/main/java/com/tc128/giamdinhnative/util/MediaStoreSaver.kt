package com.tc128.giamdinhnative.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File

private const val TAG = "MediaStoreSaver"
private const val ALBUM_NAME = "GiamDinh Native"

/**
 * Sao lưu ảnh đã chụp vào bộ sưu tập ảnh công khai của hệ điều hành (Pictures/GiamDinh Native),
 * để người dùng vẫn lấy lại được ảnh qua app Ảnh/Gallery nếu app gặp lỗi mất dữ liệu local —
 * khớp hành vi Xamarin (TakePic lưu trực tiếp vào GetExternalStoragePublicDirectory(DirectoryPictures)).
 */
object MediaStoreSaver {

    fun saveToGallery(context: Context, sourceFile: File, displayName: String): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStoreQ(context, sourceFile, displayName)
            } else {
                saveViaLegacyPublicDir(context, sourceFile, displayName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save photo to gallery: $displayName", e)
            null
        }
    }

    private fun saveViaMediaStoreQ(context: Context, sourceFile: File, displayName: String): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM_NAME")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { out -> sourceFile.inputStream().use { it.copyTo(out) } }
            ?: return null
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    // API 26-28 (trước scoped storage): ghi trực tiếp vào thư mục Pictures công khai,
    // rồi báo MediaScanner để ảnh hiện ngay trong Gallery
    private fun saveViaLegacyPublicDir(context: Context, sourceFile: File, displayName: String): Uri? {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            ALBUM_NAME
        )
        if (!dir.exists()) dir.mkdirs()
        val destFile = File(dir, displayName)
        sourceFile.inputStream().use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        }
        MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
        return Uri.fromFile(destFile)
    }
}
