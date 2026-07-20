package com.tc128.giamdinhnative.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

/**
 * Xem ảnh toàn màn hình dùng chung cho nhiều màn hình (camera thumbnail, thư viện ảnh, ...).
 * Hỗ trợ pinch-zoom + pan + double-tap trên từng ảnh, vuốt trái/phải để chuyển ảnh kế/trước
 * (tự tắt vuốt chuyển ảnh khi đang zoom để không xung đột gesture).
 *
 * @param models danh sách ảnh — mỗi phần tử là bất kỳ model Coil hỗ trợ (File, String URL, ...)
 * @param initialIndex ảnh hiển thị đầu tiên
 */
@Composable
fun ZoomableImagePagerDialog(
    models: List<Any>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    if (models.isEmpty()) return
    val startPage = initialIndex.coerceIn(0, models.lastIndex)
    val pagerState = rememberPagerState(initialPage = startPage) { models.size }
    var isZoomed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isZoomed,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                ZoomableImagePage(
                    model = models[page],
                    isActivePage = page == pagerState.currentPage,
                    onZoomChanged = { zoomed -> if (page == pagerState.currentPage) isZoomed = zoomed }
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.White)
            }

            if (models.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${pagerState.currentPage + 1} / ${models.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Nút Prev/Next tường minh — ẩn khi đang zoom vì lúc đó cần cả 2 tay để pan ảnh
                if (!isZoomed) {
                    if (pagerState.currentPage > 0) {
                        NavArrowButton(
                            icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Ảnh trước",
                            modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)
                        ) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        }
                    }
                    if (pagerState.currentPage < models.lastIndex) {
                        NavArrowButton(
                            icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Ảnh kế tiếp",
                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
                        ) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavArrowButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White)
    }
}

@Composable
private fun ZoomableImagePage(
    model: Any,
    isActivePage: Boolean,
    onZoomChanged: (Boolean) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    fun resetZoom() {
        scale = 1f
        offset = Offset.Zero
        onZoomChanged(false)
    }

    // Reset zoom khi ảnh này không còn là trang đang xem (rời trang qua vuốt) — tránh giữ trạng
    // thái zoom cũ khi quay lại trang này lần sau.
    LaunchedEffect(isActivePage) {
        if (!isActivePage) resetZoom()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 6f)
                    scale = newScale
                    offset = if (newScale <= 1f) Offset.Zero else offset + pan
                    onZoomChanged(newScale > 1f)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1f) resetZoom() else { scale = 3f; onZoomChanged(true) }
                })
            }
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}
