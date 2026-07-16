package com.tc128.giamdinhnative.ui.screens.newitem

import android.content.Context
import android.util.Size
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Full-screen CameraX dialog cho OCR scan.
 * Dùng OnImageCapturedCallback — không qua system camera, không có màn hình xác nhận.
 * Trả về bytes thô + rotation ngay sau khi chụp.
 */
@Composable
fun OcrCameraDialog(
    onCapture: (bytes: ByteArray, rotationDegrees: Int, captureStartMs: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    // Đổi flash mode sẽ rebind camera (LaunchedEffect bên dưới key theo flashMode)
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }

    // zoomState là LiveData — đọc .value 1 lần ngay lúc bind xong thường ra giá trị placeholder
    // (min=max=1f) vì CameraX cần 1 nhịp nữa mới xác định xong range zoom thật (vd 0.5x/0.6x
    // ultra-wide). Phải observe để cập nhật khi giá trị thật về, không thì nút 0.5x không hiện.
    var minZoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(1f) }
    DisposableEffect(camera) {
        val zoomState = camera?.cameraInfo?.zoomState
        val observer = androidx.lifecycle.Observer<androidx.camera.core.ZoomState> { zs ->
            minZoomRatio = zs.minZoomRatio
            maxZoomRatio = zs.maxZoomRatio
        }
        zoomState?.observeForever(observer)
        onDispose { zoomState?.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    // Camera vật lý ultra-wide (0.5x) — null nếu máy không có/không phát hiện được
    val ultrawideCameraId = remember { com.tc128.giamdinhnative.util.findUltrawideCameraId(context) }
    var useUltrawide by remember { mutableStateOf(false) }
    var targetZoomAfterRebind by remember { mutableFloatStateOf(1f) }

    // Rebind mỗi khi đổi flash mode hoặc bật/tắt ultra-wide (cần preview đã sẵn sàng)
    LaunchedEffect(flashMode, previewView, useUltrawide) {
        val pv = previewView ?: return@LaunchedEffect
        bindOcrCamera(
            context, lifecycleOwner, pv, flashMode,
            ultrawideCameraId = if (useUltrawide) ultrawideCameraId else null
        ) { cam, capture ->
            camera = cam
            imageCapture = capture
            if (useUltrawide) {
                zoomRatio = 0.5f
            } else {
                cam.cameraControl.setZoomRatio(targetZoomAfterRebind)
                zoomRatio = targetZoomAfterRebind
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // CameraX preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { pv ->
                        pv.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        pv.scaleType = PreviewView.ScaleType.FILL_CENTER
                        previewView = pv
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            camera?.let { cam ->
                                val info = cam.cameraInfo.zoomState.value ?: return@let
                                val newZoom = (info.zoomRatio * zoom)
                                    .coerceIn(info.minZoomRatio, info.maxZoomRatio)
                                cam.cameraControl.setZoomRatio(newZoom)
                                zoomRatio = newZoom
                            }
                        }
                    }
            )

            // Hướng dẫn ở trên
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp, start = 24.dp, end = 24.dp),
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Hướng camera vào số container",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            // Nút đóng (góc trên trái)
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Đóng",
                    tint = Color.White
                )
            }

            // Nút flash (góc trên phải)
            IconButton(
                onClick = {
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                        ImageCapture.FLASH_MODE_ON  -> ImageCapture.FLASH_MODE_AUTO
                        else                        -> ImageCapture.FLASH_MODE_OFF
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON  -> Icons.Default.FlashOn
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                        else                         -> Icons.Default.FlashOff
                    },
                    contentDescription = "Flash",
                    tint = if (flashMode == ImageCapture.FLASH_MODE_ON) Color.Yellow else Color.White
                )
            }

            // Zoom label
            if (kotlin.math.abs(zoomRatio - 1f) > 0.05f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 112.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("%.1fx".format(zoomRatio), color = Color.White,
                        style = MaterialTheme.typography.labelMedium)
                }
            }

            // Zoom buttons (bên phải)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OcrZoomButton(icon = Icons.Default.Add, contentDescription = "Zoom in") {
                    camera?.let { cam ->
                        val newZoom = (zoomRatio + 0.5f).coerceAtMost(maxZoomRatio)
                        cam.cameraControl.setZoomRatio(newZoom)
                        zoomRatio = newZoom
                    }
                }
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("%.1fx".format(zoomRatio), color = Color.White, fontSize = 11.sp)
                }
                OcrZoomButton(icon = Icons.Default.Remove, contentDescription = "Zoom out") {
                    camera?.let { cam ->
                        val newZoom = (zoomRatio - 0.5f).coerceAtLeast(minZoomRatio)
                        cam.cameraControl.setZoomRatio(newZoom)
                        zoomRatio = newZoom
                    }
                }
            }

            // Zoom presets: 0.5x (bind camera vật lý ultra-wide nếu máy có), 1x, 2x, 3x
            run {
                val presets = buildList {
                    if (ultrawideCameraId != null) add(0.5f)
                    add(1f)
                    if (maxZoomRatio >= 2f) add(2f)
                    if (maxZoomRatio >= 3f) add(3f)
                }
                if (presets.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { preset ->
                            OcrZoomPresetChip(
                                value = preset,
                                selected = if (preset == 0.5f) useUltrawide
                                           else !useUltrawide && kotlin.math.abs(zoomRatio - preset) < 0.05f,
                                onClick = {
                                    if (preset == 0.5f) {
                                        useUltrawide = true
                                    } else if (useUltrawide) {
                                        targetZoomAfterRebind = preset
                                        useUltrawide = false
                                    } else {
                                        camera?.cameraControl?.setZoomRatio(preset)
                                        zoomRatio = preset
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Nút chụp (giữa dưới)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
            ) {
                OcrCaptureButton(
                    isCapturing = isCapturing,
                    onClick = {
                        val capture = imageCapture ?: return@OcrCaptureButton
                        val captureStartMs = System.currentTimeMillis() // T0: lúc bấm nút
                        isCapturing = true
                        capture.takePicture(
                            cameraExecutor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val bytes = image.planes[0].buffer.let { buf ->
                                        ByteArray(buf.remaining()).also { buf.get(it) }
                                    }
                                    val rotation = image.imageInfo.rotationDegrees
                                    image.close()

                                    scope.launch(Dispatchers.Main) {
                                        isCapturing = false
                                        onCapture(bytes, rotation, captureStartMs)
                                    }
                                }

                                override fun onError(exc: ImageCaptureException) {
                                    scope.launch(Dispatchers.Main) { isCapturing = false }
                                }
                            }
                        )
                    }
                )
            }
        }
    }
}

@OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
private fun bindOcrCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    flashMode: Int,
    ultrawideCameraId: String? = null,
    onReady: (Camera, ImageCapture) -> Unit
) {
    val future = ProcessCameraProvider.getInstance(context)
    future.addListener({
        val provider = future.get()
        val preview = Preview.Builder().build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        // MINIMIZE_LATENCY: tổ hợp stream đơn giản, HAL hỗ trợ phổ quát. Không dùng
        // ZERO_SHUTTER_LAG vì trên HyperOS/Redmi tổ hợp Preview + ImageCapture(ZSL) có thể bind
        // "thành công" nhưng HAL không đẩy frame → preview đen (không ném exception).
        val capture = ImageCapture.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 960),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                        )
                    )
                    .build()
            )
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(flashMode)
            .build()

        // 0.5x trên nhiều máy (đặc biệt Samsung) không truy cập được qua setZoomRatio < 1 của
        // camera logic chính — phải bind thẳng tới camera vật lý ultra-wide (xem CameraUtils.kt)
        val cameraSelector = if (ultrawideCameraId != null) {
            CameraSelector.Builder()
                .addCameraFilter { infos ->
                    infos.filter {
                        androidx.camera.camera2.interop.Camera2CameraInfo.from(it).cameraId == ultrawideCameraId
                    }
                }
                .build()
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        try {
            provider.unbindAll()
            val cam = provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, capture)
            onReady(cam, capture)

            val center = previewView.meteringPointFactory.createPoint(0.5f, 0.5f)
            val action = FocusMeteringAction.Builder(
                center, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
            ).setAutoCancelDuration(2, TimeUnit.SECONDS).build()
            cam.cameraControl.startFocusAndMetering(action)
        } catch (_: Exception) {}
    }, ContextCompat.getMainExecutor(context))
}

@Composable
private fun OcrZoomButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(Color.White.copy(alpha = 0.15f), CircleShape)
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun OcrZoomPresetChip(value: Float, selected: Boolean, onClick: () -> Unit) {
    val label = if (value < 1f) "%.1f".format(value) else "${value.toInt()}"
    Box(
        modifier = Modifier
            .size(if (selected) 36.dp else 30.dp)
            .clip(CircleShape)
            .background(if (selected) Color.White else Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "${label}x",
            color = if (selected) Color.Black else Color.White,
            fontSize = if (selected) 12.sp else 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OcrCaptureButton(isCapturing: Boolean, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.size(76.dp).border(3.dp, Color.White, CircleShape))
        if (isCapturing) {
            CircularProgressIndicator(
                modifier = Modifier.size(52.dp),
                color = Color.White,
                strokeWidth = 3.dp
            )
        } else {
            FilledIconButton(
                onClick = onClick,
                modifier = Modifier.size(62.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White)
            ) {}
        }
    }
}
