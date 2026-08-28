package com.tc128.giamdinhnative.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

// Hai hàm TÁCH RIÊNG cho hai loại cửa sổ — không được gộp lại:
// - Màn hình thường (trong Activity) dùng composition local WindowInsets của Compose, luôn đúng vì
//   LocalView.current ở đây LÀ view gốc dùng chung toàn app.
// - Màn hình trong Dialog cần đọc inset thủ công vì Dialog là một Window riêng, composition local
//   WindowInsets luôn trả 0 ở đó. Nếu dùng cách đọc thủ công (ViewCompat.setOnApplyWindowInsetsListener)
//   cho màn hình thường thì sẽ ghi đè mất listener nội bộ của Compose gắn trên view gốc dùng chung,
//   làm hỏng insets phản ứng của TOÀN BỘ app cho tới khi rời màn hình (onDispose không khôi phục lại
//   được listener gốc của Compose) — tuyệt đối không tái sử dụng chéo hai hàm này.

/** Chiều cao thanh điều hướng cho màn hình thường (trong Activity), có sàn an toàn tối thiểu. */
@Composable
fun rememberNavigationBarBottomDp(minDp: Dp = 16.dp): Dp {
    val density = LocalDensity.current
    val insetPx = WindowInsets.navigationBars.getBottom(density)
    return with(density) { insetPx.toDp() }.coerceAtLeast(minDp)
}

/** Chiều cao thanh điều hướng cho màn hình chạy trong Dialog (Window riêng), có sàn an toàn tối thiểu. */
@Composable
fun rememberDialogNavigationBarBottomDp(minDp: Dp = 16.dp): Dp {
    val dialogView = LocalView.current
    val density = LocalDensity.current
    var navBarBottom by remember { mutableStateOf(0.dp) }
    DisposableEffect(dialogView) {
        (dialogView.parent as? DialogWindowProvider)?.window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
        }
        ViewCompat.setOnApplyWindowInsetsListener(dialogView) { _, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            navBarBottom = with(density) { b.toDp() }
            insets
        }
        ViewCompat.requestApplyInsets(dialogView)
        onDispose { ViewCompat.setOnApplyWindowInsetsListener(dialogView, null) }
    }
    return navBarBottom.coerceAtLeast(minDp)
}
