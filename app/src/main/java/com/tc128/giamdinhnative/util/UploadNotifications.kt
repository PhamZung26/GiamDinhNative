package com.tc128.giamdinhnative.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Notification cho các worker đồng bộ ảnh chạy foreground (upload/resize hàng loạt).
 * Foreground service giúp worker KHÔNG bị cắt bởi giới hạn ~10 phút của WorkManager và chống
 * HyperOS/Redmi kill background khi upload số lượng lớn (vd 500 ảnh).
 */
object UploadNotifications {
    const val CHANNEL_ID = "photo_sync"
    const val NOTIF_ID_UPLOAD = 4201
    const val NOTIF_ID_RESIZE = 4202

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java) ?: return
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Đồng bộ ảnh",
                    NotificationManager.IMPORTANCE_LOW  // không kêu/rung
                ).apply { description = "Tiến độ tải ảnh container lên máy chủ" }
                mgr.createNotificationChannel(channel)
            }
        }
    }

    /**
     * @param done số ảnh đã xong, @param total tổng (nếu total<=0 → progress bar indeterminate)
     */
    fun build(context: Context, title: String, done: Int, total: Int): Notification {
        ensureChannel(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(title)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        if (total > 0) {
            builder.setContentText("$done/$total")
            builder.setProgress(total, done, false)
        } else {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }
}
