package com.tc128.giamdinhnative.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.tc128.giamdinhnative.BuildConfig
import com.tc128.giamdinhnative.data.remote.AppUpdateInfo
import com.tc128.giamdinhnative.data.remote.ApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kiểm tra & tải bản cập nhật mới — port từ Xamarin UpdateService/AboutViewModel
 * (api/updatev2/latest, DownloadManager tải APK), có thêm bước tự mở trình cài đặt
 * sau khi tải xong (Xamarin chỉ tải về Downloads, người dùng phải tự mở thủ công).
 */
@Singleton
class UpdateChecker @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) {
    val currentVersionCode: Int get() = BuildConfig.VERSION_CODE
    val currentVersionName: String get() = BuildConfig.VERSION_NAME

    /** Trả về AppUpdateInfo nếu server có bản mới hơn version hiện tại, null nếu không có/lỗi */
    suspend fun checkForUpdate(): AppUpdateInfo? {
        val info = runCatching { apiService.getLatestAppUpdate() }.getOrNull() ?: return null
        return if (info.versionCode > currentVersionCode) info else null
    }

    /** Tải APK qua DownloadManager, tự mở trình cài đặt khi tải xong */
    fun downloadAndInstall(apkUrl: String, versionName: String) {
        val fileName = "GiamDinhNative_$versionName.apk"
        val apkDir = File(context.getExternalFilesDir(null), "apk").apply { mkdirs() }
        val destFile = File(apkDir, fileName)
        if (destFile.exists()) destFile.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Đang tải bản cập nhật $versionName")
            .setDescription("GiamDinh Native")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    context.unregisterReceiver(this)
                    if (destFile.exists()) openInstaller(destFile)
                }
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
    }

    private fun openInstaller(apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
