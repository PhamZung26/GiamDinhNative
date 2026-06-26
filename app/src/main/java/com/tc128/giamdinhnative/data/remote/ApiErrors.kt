package com.tc128.giamdinhnative.data.remote

import retrofit2.HttpException

/**
 * Trích message lỗi thật từ response body khi backend trả BadRequest(string)/(object), vd:
 * "Container ABC123 đã bị khóa Gate In." — ASP.NET serialize message string thành JSON string
 * literal (có dấu nháy kép bao quanh), nên cần bóc dấu nháy ra để hiển thị sạch cho người dùng.
 * Mặc định (không phải HttpException, hoặc không đọc được body) trả về message gốc.
 */
fun Throwable.toUserMessage(): String? {
    if (this !is HttpException) return message
    val raw = runCatching { response()?.errorBody()?.string() }.getOrNull()?.trim()
    if (raw.isNullOrBlank()) return message
    val unwrapped = if (raw.length >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) {
        raw.substring(1, raw.length - 1).replace("\\\"", "\"")
    } else {
        raw
    }
    return unwrapped
}
