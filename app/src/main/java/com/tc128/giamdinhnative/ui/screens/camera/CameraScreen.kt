package com.tc128.giamdinhnative.ui.screens.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaActionSound
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.tc128.giamdinhnative.ui.components.ZoomableImagePagerDialog
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val TAG = "CameraScreen"

private data class PendingImage(
    val bytes: ByteArray,
    val rotationDegrees: Int,
    val t0Press: Long,       // thời điểm bấm nút
    val t1Captured: Long,    // onCaptureSuccess gọi (sensor xong)
    val t2BytesRead: Long,   // đọc bytes + image.close() xong
    val onSaved: (String, String) -> Unit  // (path, timingMessage)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    containerId: String,
    itemEorId: Int? = null,
    onBack: () -> Unit,
    onPhotoCaptured: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val pipeline = remember { Channel<PendingImage>(capacity = Channel.UNLIMITED) }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Camera vật lý ultra-wide (0.5x) — null nếu máy không có/không phát hiện được
    val ultrawideCameraId = remember { com.tc128.giamdinhnative.util.findUltrawideCameraId(context) }
    var useUltrawide by remember { mutableStateOf(false) }
    // Zoom muốn áp dụng cho camera chính sau khi rebind xong (chỉ cần khi đang chuyển từ ultra-wide về)
    var targetZoomAfterRebind by remember { mutableFloatStateOf(1f) }

    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var isCapturing by remember { mutableStateOf(false) }
    var showFocusRing by remember { mutableStateOf(false) }
    var focusOffset by remember { mutableStateOf(Offset.Zero) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var showCaptureFlash by remember { mutableStateOf(false) }

    // zoomState là LiveData — đọc .value 1 lần ngay lúc bind xong thường ra giá trị placeholder
    // (min=max=1f) vì CameraX cần 1 nhịp nữa mới xác định xong range zoom thật của thiết bị (vd
    // 0.5x/0.6x ultra-wide). Phải observe để cập nhật lại khi giá trị thật về, không thì nút
    // zoom 0.5x sẽ không bao giờ hiện ra dù máy có hỗ trợ.
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

    var processingCount by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    var lastPhotoPath by remember { mutableStateOf<String?>(null) }
    // Toàn bộ ảnh đã chụp trong phiên này — chạm thumbnail mở lại xem, next/pre giữa các ảnh
    val capturedPaths = remember { mutableStateListOf<String>() }
    var showImageViewer by remember { mutableStateOf(false) }
    var thumbnailAnimTrigger by remember { mutableIntStateOf(0) }
    val thumbnailScale by animateFloatAsState(
        targetValue = if (thumbnailAnimTrigger % 2 == 1) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "thumbScale"
    )

    // Tiếng tách khi chụp — giống camera gốc Samsung
    val shutterSound = remember { MediaActionSound().also { it.load(MediaActionSound.SHUTTER_CLICK) } }
    DisposableEffect(Unit) { onDispose { shutterSound.release() } }

    // Worker pipeline chạy suốt vòng đời màn hình trên IO thread
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            for (pending in pipeline) {
                try {
                    val t3 = System.currentTimeMillis()
                    val path = compressAndSave(context, pending.bytes, pending.rotationDegrees)
                    val t4 = System.currentTimeMillis()

                    val tSensor  = pending.t1Captured - pending.t0Press
                    val tRead    = pending.t2BytesRead - pending.t1Captured
                    val tSave    = t4 - t3
                    val tTotal   = t4 - pending.t0Press
                    val msg = "Đã lưu ảnh  (${tTotal}ms)"

                    withContext(Dispatchers.Main) {
                        processingCount = (processingCount - 1).coerceAtLeast(0)
                        pending.onSaved(path, msg)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Pipeline process error", e)
                    withContext(Dispatchers.Main) {
                        processingCount = (processingCount - 1).coerceAtLeast(0)
                    }
                }
            }
        }
    }

    // ĐÃ THỬ dùng torch (đèn pin bật liên tục) thay cho FLASH_MODE_ON để tránh pha pre-flash đo
    // sáng — nhanh hơn nhưng ảnh bị cháy sáng vì bỏ qua bước AE precapture chuẩn của CameraX
    // (thứ đang cho kết quả đúng sáng ở chế độ AUTO). Quay lại dùng FLASH_MODE_ON nguyên bản.
    LaunchedEffect(flashMode) { imageCapture?.flashMode = flashMode }

    // Rebind sang camera vật lý ultra-wide khi bật 0.5x, rebind lại camera chính khi tắt
    // (bỏ qua lần đầu — factory của AndroidView đã tự bind camera chính rồi)
    LaunchedEffect(useUltrawide, previewView) {
        val pv = previewView ?: return@LaunchedEffect
        if (camera == null) return@LaunchedEffect // lần bind đầu do factory lo, tránh bind đôi
        bindCamera(
            context, lifecycleOwner, pv, flashMode,
            ultrawideCameraId = if (useUltrawide) ultrawideCameraId else null,
            onCameraReady = { cam, capture ->
                camera = cam
                imageCapture = capture
                if (useUltrawide) {
                    zoomRatio = 0.5f
                } else {
                    cam.cameraControl.setZoomRatio(targetZoomAfterRebind)
                    zoomRatio = targetZoomAfterRebind
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            pipeline.close()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(containerId, style = MaterialTheme.typography.titleMedium)
                        itemEorId?.let {
                            Text("EOR #$it", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                            ImageCapture.FLASH_MODE_ON  -> ImageCapture.FLASH_MODE_OFF
                            else                        -> ImageCapture.FLASH_MODE_AUTO
                        }
                    }) {
                        Icon(
                            imageVector = when (flashMode) {
                                ImageCapture.FLASH_MODE_ON  -> Icons.Default.FlashOn
                                ImageCapture.FLASH_MODE_OFF -> Icons.Default.FlashOff
                                else                        -> Icons.Default.FlashAuto
                            },
                            contentDescription = "Flash",
                            tint = if (flashMode == ImageCapture.FLASH_MODE_ON) Color.Yellow else Color.White
                        )
                    }
                }
            )
        },
        containerColor = Color.Black,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Camera Preview ──────────────────────────────────────────────
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { pv ->
                        // COMPATIBLE (TextureView) tương thích rộng hơn PERFORMANCE (SurfaceView)
                        // — tránh màn hình đen trên Redmi/Snapdragon mid-range
                        pv.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        pv.scaleType = PreviewView.ScaleType.FILL_CENTER
                        previewView = pv
                        bindCamera(ctx, lifecycleOwner, pv, flashMode,
                            ultrawideCameraId = null,
                            onCameraReady = { cam, capture ->
                                camera = cam
                                imageCapture = capture
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { tap ->
                            camera?.let { cam ->
                                val pv = previewView ?: return@detectTapGestures
                                val point = pv.meteringPointFactory.createPoint(tap.x, tap.y)
                                val action = FocusMeteringAction.Builder(
                                    point,
                                    FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                )
                                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                    .build()
                                cam.cameraControl.startFocusAndMetering(action)
                                focusOffset = tap
                                showFocusRing = true
                                scope.launch { delay(1200); showFocusRing = false }
                            }
                        }
                    }
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

            // ── Snackbar ở trên đầu ─────────────────────────────────────────
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color.Black.copy(alpha = 0.8f),
                    contentColor = Color.White
                )
            }

            // ── Capture flash blink ─────────────────────────────────────────
            AnimatedVisibility(
                visible = showCaptureFlash,
                enter = fadeIn(animationSpec = tween(30)),
                exit = fadeOut(animationSpec = tween(150)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.6f)))
            }

            // ── Focus ring ──────────────────────────────────────────────────
            AnimatedVisibility(
                visible = showFocusRing,
                enter = fadeIn() + scaleIn(initialScale = 1.4f),
                exit = fadeOut(animationSpec = tween(300)),
                modifier = Modifier
                    .offset(
                        x = with(LocalContext.current) { focusOffset.x.dp - 40.dp },
                        y = with(LocalContext.current) { focusOffset.y.dp - 40.dp }
                    )
                    .size(80.dp)
            ) {
                Box(Modifier.fillMaxSize().border(2.dp, Color.Yellow, RoundedCornerShape(4.dp)))
            }

            // ── Zoom label ──────────────────────────────────────────────────
            if (kotlin.math.abs(zoomRatio - 1f) > 0.05f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("%.1fx".format(zoomRatio), color = Color.White,
                        style = MaterialTheme.typography.labelMedium)
                }
            }

            // ── Zoom buttons (bên phải) ─────────────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ZoomButton(icon = Icons.Default.Add, contentDescription = "Zoom in") {
                    if (useUltrawide) {
                        // Từ 0.5x bấm + → rời ultra-wide, quay lại camera chính ở 1x
                        targetZoomAfterRebind = 1f
                        useUltrawide = false
                    } else {
                        camera?.let { cam ->
                            val newZoom = (zoomRatio + 0.5f).coerceAtMost(maxZoomRatio)
                            cam.cameraControl.setZoomRatio(newZoom)
                            zoomRatio = newZoom
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("%.1fx".format(zoomRatio), color = Color.White, fontSize = 11.sp)
                }
                ZoomButton(icon = Icons.Default.Remove, contentDescription = "Zoom out") {
                    if (!useUltrawide && ultrawideCameraId != null && zoomRatio - 0.5f < 1f) {
                        // Sắp xuống dưới 1x mà máy có ultra-wide vật lý → chuyển sang 0.5x thật
                        // (setZoomRatio < 1 trên camera chính không hoạt động trên nhiều máy)
                        useUltrawide = true
                    } else {
                        camera?.let { cam ->
                            val newZoom = (zoomRatio - 0.5f).coerceAtLeast(minZoomRatio)
                            cam.cameraControl.setZoomRatio(newZoom)
                            zoomRatio = newZoom
                        }
                    }
                }
            }

            // ── Processing counter (góc trái dưới) ─────────────────────────
            if (processingCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 120.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Text("$processingCount ảnh đang lưu", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // ── Zoom presets: 0.5x (bind camera vật lý ultra-wide nếu máy có), 1x, 2x, 3x ──
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
                            .padding(bottom = 112.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { preset ->
                            ZoomPresetChip(
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

            // ── Bottom controls: Back | Capture ────────────────────────────
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 40.dp, start = 32.dp, end = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nút Back ở dưới
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Nút chụp ở giữa
                CaptureButton(
                    isCapturing = isCapturing,
                    onClick = {
                        val capture = imageCapture ?: return@CaptureButton
                        val t0 = System.currentTimeMillis()    // T0: bấm nút
                        isCapturing = true
                        shutterSound.play(MediaActionSound.SHUTTER_CLICK)
                        showCaptureFlash = true
                        scope.launch { delay(200); showCaptureFlash = false }

                        capture.takePicture(
                            cameraExecutor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val t1 = System.currentTimeMillis()    // T1: sensor xong
                                    val bytes = image.planes[0].buffer.let { buf ->
                                        ByteArray(buf.remaining()).also { buf.get(it) }
                                    }
                                    val rotation = image.imageInfo.rotationDegrees
                                    image.close()
                                    val t2 = System.currentTimeMillis()    // T2: đọc bytes xong

                                    scope.launch(Dispatchers.Main) {
                                        isCapturing = false
                                        processingCount++
                                    }

                                    pipeline.trySend(
                                        PendingImage(bytes, rotation, t0, t1, t2) { finalPath, timingMsg ->
                                            onPhotoCaptured(finalPath)
                                            lastPhotoPath = finalPath
                                            capturedPaths.add(finalPath)
                                            thumbnailAnimTrigger++
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = timingMsg,
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        }
                                    )
                                }

                                override fun onError(exc: ImageCaptureException) {
                                    Log.e(TAG, "Capture failed", exc)
                                    scope.launch(Dispatchers.Main) { isCapturing = false }
                                }
                            }
                        )
                    }
                )

                // Thumbnail ảnh vừa chụp — góc phải, giống camera Samsung. Chạm để xem lại toàn màn
                // hình, có zoom + next/pre giữa các ảnh đã chụp trong phiên này.
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .scale(thumbnailScale)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = if (lastPhotoPath != null) 2.dp else 0.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(Color.White.copy(alpha = 0.1f))
                        .then(
                            if (lastPhotoPath != null) Modifier.clickable { showImageViewer = true }
                            else Modifier
                        )
                ) {
                    if (lastPhotoPath != null) {
                        AsyncImage(
                            model = java.io.File(lastPhotoPath!!),
                            contentDescription = "Ảnh vừa chụp",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            if (showImageViewer && capturedPaths.isNotEmpty()) {
                ZoomableImagePagerDialog(
                    models = capturedPaths.map { java.io.File(it) },
                    initialIndex = capturedPaths.lastIndex,
                    onDismiss = { showImageViewer = false }
                )
            }
        }
    }
}

// ── Camera binding ────────────────────────────────────────────────────────────

@OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
private fun bindCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    flashMode: Int,
    ultrawideCameraId: String?,
    onCameraReady: (Camera, ImageCapture) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

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

        run {
            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            // MINIMIZE_LATENCY: tổ hợp stream đơn giản, được HAL hỗ trợ phổ quát. Không dùng
            // ZERO_SHUTTER_LAG vì trên HyperOS/Redmi tổ hợp Preview + ImageCapture(ZSL) có thể
            // bind "thành công" nhưng HAL không đẩy frame → preview đen (không ném exception nên
            // fallback theo exception không cứu được). Đánh đổi: nút chụp chậm hơn ~200-500ms.
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(flashMode)
                .setJpegQuality(90)
                .build()
            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageCapture
                )
                startContinuousAutoFocus(camera, previewView)
                onCameraReady(camera, imageCapture)
            } catch (e: Exception) {
                Log.e(TAG, "bindCamera failed", e)
            }
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun startContinuousAutoFocus(camera: Camera, previewView: PreviewView) {
    val factory = previewView.meteringPointFactory
    val centerPoint = factory.createPoint(0.5f, 0.5f)
    val action = FocusMeteringAction.Builder(
        centerPoint,
        FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
    )
        .setAutoCancelDuration(2, TimeUnit.SECONDS)
        .build()
    camera.cameraControl.startFocusAndMetering(action)
}

// ── Image processing pipeline ─────────────────────────────────────────────────

private fun compressAndSave(context: Context, bytes: ByteArray, rotationDegrees: Int): String {
    // Bước 1: đọc kích thước gốc (không decode pixel — cực nhanh)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

    // Bước 2: tính inSampleSize để decode thẳng ở kích thước gần đích (1280)
    // → tránh load full 12MP (~48MB RAM) vào memory rồi mới scale.
    // Phải ≥ maxDim cuối cùng (ImageResizer.resizeFile = 1280) để bước resize chính xác
    // sau đó không bị giới hạn bởi ảnh đã bị cắt nét từ bước lưu nhanh này.
    val sampleSize = calcSampleSize(bounds.outWidth, bounds.outHeight, maxDim = 1280)
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.RGB_565   // 2 bytes/pixel thay vì 4 → nhanh hơn 2x
    }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)

    // Bước 3: rotate nếu cần
    val rotated = if (rotationDegrees != 0) {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            .also { if (it !== bitmap) bitmap.recycle() }
    } else bitmap

    // Bước 4: ghi file
    val file = createOutputFile(context)
    FileOutputStream(file).use { out ->
        rotated.compress(Bitmap.CompressFormat.JPEG, 82, out)
    }
    rotated.recycle()

    return file.absolutePath
}

private fun calcSampleSize(width: Int, height: Int, maxDim: Int): Int {
    val raw = maxOf(width, height)
    var size = 1
    while (raw / (size * 2) >= maxDim) size *= 2
    return size
}

private fun createOutputFile(context: Context): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault())
        .format(System.currentTimeMillis())
    val dir = context.externalCacheDir ?: context.cacheDir
    return File(dir, "IMG_${timestamp}.jpg")
}

// ── UI Components ─────────────────────────────────────────────────────────────

@Composable
private fun ZoomButton(
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
private fun ZoomPresetChip(value: Float, selected: Boolean, onClick: () -> Unit) {
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
private fun CaptureButton(isCapturing: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isCapturing) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "captureScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.scale(scale)
    ) {
        Box(modifier = Modifier.size(80.dp).border(4.dp, Color.White, CircleShape))
        FilledIconButton(
            onClick = onClick,
            enabled = !isCapturing,
            modifier = Modifier.size(66.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.White,
                disabledContainerColor = Color.White.copy(alpha = 0.5f)
            )
        ) { }
    }
}
