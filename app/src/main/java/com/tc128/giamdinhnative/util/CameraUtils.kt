package com.tc128.giamdinhnative.util

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

/**
 * Tìm camera vật lý ultra-wide (góc rộng/0.5x) ở mặt sau máy.
 *
 * CameraX's CameraSelector.DEFAULT_BACK_CAMERA chỉ trỏ tới camera logic chính (thường id "0").
 * Trên nhiều máy — đặc biệt dòng Samsung — HAL không expose ultra-wide qua continuous zoom-ratio
 * của camera logic đó (zoomState.minZoomRatio báo về 1.0 dù máy có ultra-wide thật), nên phải
 * bind trực tiếp tới ID camera vật lý ultra-wide mới truy cập được — không thể chỉ gọi
 * setZoomRatio() với giá trị nhỏ hơn 1.
 *
 * Heuristic: trong các camera mặt sau, camera nào có tiêu cự (focal length) ngắn nhất rõ rệt
 * so với camera chính (id "0") chính là ultra-wide.
 */
fun findUltrawideCameraId(context: Context): String? {
    return try {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var mainFocal: Float? = null
        var widestId: String? = null
        var widestFocal = Float.MAX_VALUE

        for (id in manager.cameraIdList) {
            val chars = manager.getCameraCharacteristics(id)
            if (chars.get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_BACK) continue
            val minFocal = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.minOrNull() ?: continue

            if (id == "0") mainFocal = minFocal
            if (minFocal < widestFocal) {
                widestFocal = minFocal
                widestId = id
            }
        }

        val main = mainFocal ?: return null
        // Ultra-wide phải có tiêu cự rõ rệt nhỏ hơn camera chính (không phải cùng 1 camera,
        // không phải chênh lệch do sai số đo) — ngưỡng 80% tiêu cự camera chính
        if (widestId != null && widestId != "0" && widestFocal < main * 0.8f) widestId else null
    } catch (_: Exception) {
        null
    }
}
