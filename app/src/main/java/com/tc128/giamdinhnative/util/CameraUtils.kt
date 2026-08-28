package com.tc128.giamdinhnative.util

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build

/**
 * Tìm camera vật lý ultra-wide (góc rộng/0.5x) ở mặt sau máy.
 *
 * CameraX's CameraSelector.DEFAULT_BACK_CAMERA chỉ trỏ tới camera logic chính. Trên nhiều máy —
 * đặc biệt Samsung/Xiaomi(HyperOS) — HAL không expose ultra-wide qua continuous zoom-ratio của
 * camera logic đó, nên phải bind trực tiếp tới ID camera vật lý ultra-wide mới truy cập được.
 *
 * Cách tìm: gom tất cả camera mặt sau — cả camera logic trong [cameraIdList] LẪN các physical
 * sub-camera của chúng (getPhysicalCameraIds, API 28+; nhiều máy chỉ expose ultra-wide ở đây, không
 * nằm trong cameraIdList). Lấy tiêu cự nhỏ nhất mỗi camera; mốc "chính" là camera có tiêu cự LỚN
 * nhất (không giả định id "0"); ultra-wide là camera có tiêu cự < 80% mốc đó.
 */
fun findUltrawideCameraId(context: Context): String? {
    return try {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // id camera mặt sau -> tiêu cự nhỏ nhất của nó
        val backFocals = LinkedHashMap<String, Float>()

        fun minFocalOf(id: String): Float? = runCatching {
            manager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.minOrNull()
        }.getOrNull()

        for (id in manager.cameraIdList) {
            val chars = runCatching { manager.getCameraCharacteristics(id) }.getOrNull() ?: continue
            if (chars.get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_BACK) continue

            minFocalOf(id)?.let { backFocals[id] = it }

            // Physical sub-camera (multi-camera logic) — ultra-wide thường nằm ở đây trên Redmi/HyperOS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                runCatching { chars.physicalCameraIds }.getOrNull()?.forEach { physId ->
                    minFocalOf(physId)?.let { f -> backFocals.putIfAbsent(physId, f) }
                }
            }
        }

        if (backFocals.size < 2) return null

        // Mốc "chính" = camera mặt sau có tiêu cự LỚN nhất; ultra-wide = tiêu cự nhỏ nhất
        val mainFocal = backFocals.values.max()
        val widest = backFocals.minByOrNull { it.value } ?: return null

        // Ultra-wide phải nhỏ hơn rõ rệt (< 80%) mốc chính — tránh nhầm do sai số đo
        if (widest.value < mainFocal * 0.8f) widest.key else null
    } catch (_: Exception) {
        null
    }
}
