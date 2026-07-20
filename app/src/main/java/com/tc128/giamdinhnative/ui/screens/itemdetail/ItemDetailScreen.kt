package com.tc128.giamdinhnative.ui.screens.itemdetail

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.tc128.giamdinhnative.data.model.StatusOfContainer
import com.tc128.giamdinhnative.ui.screens.items.statusColor
import com.tc128.giamdinhnative.ui.components.autoBringIntoViewOnFocus
import com.tc128.giamdinhnative.ui.screens.newitem.OcrCameraDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ItemDetailScreen(
    containerId: Int,
    onBack: () -> Unit,
    onOpenImages: () -> Unit,
    onOpenDamages: () -> Unit,
    onOpenChamDiem: () -> Unit = {},
    onCameraDM: () -> Unit = {},
    onCameraAV: () -> Unit = {},
    onCameraVS: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: ItemDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSealScanDialog by remember { mutableStateOf(false) }
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    if (showSealScanDialog) {
        OcrCameraDialog(
            onCapture = { bytes, rotationDegrees, _ ->
                showSealScanDialog = false
                viewModel.scanSeal(bytes, rotationDegrees)
            },
            onDismiss = { showSealScanDialog = false }
        )
    }

    BackHandler { onBack() }

    // Vuốt từ mép trái sang phải để back (cho cả chế độ 3-button nav)
    var swipeStartX by remember { mutableFloatStateOf(0f) }

    // Reload mỗi khi quay lại màn hình (vd: sau khi chụp ảnh/sửa hư hỏng), nhưng không reload
    // khi đang có thay đổi chưa lưu (isDirty) để không mất dữ liệu người dùng vừa nhập — set bởi
    // MỌI thao tác sửa trường (Seal, Tình trạng, Grade, ...), không chỉ khi mở khoá Size/Opt
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(containerId, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (!uiState.isDirty) viewModel.load(containerId)
        }
    }

    // Token hết hạn → về màn hình login
    LaunchedEffect(uiState.requiresLogin) {
        if (uiState.requiresLogin) onLogout()
    }

    LaunchedEffect(uiState.saveMessage) {
        uiState.saveMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.dismissSaveMessage()
        }
    }

    val status = uiState.container?.statusOfContainer
    val sColor = statusColor(status)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> swipeStartX = offset.x },
                    onHorizontalDrag = { _, dragAmount ->
                        // Vuốt từ mép trái (startX < 60px) sang phải > 80px → back
                        if (swipeStartX < 60f && dragAmount > 0) {
                            swipeStartX = Float.MAX_VALUE  // ngăn trigger nhiều lần
                            onBack()
                        }
                    }
                )
            }
    ) {
    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 52.dp,
                title = {
                    Column {
                        Text(
                            uiState.container?.containerNumber ?: "...",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (status != null) {
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .background(sColor.copy(alpha = 0.9f), RoundedCornerShape(3.dp))
                                )
                                Text(
                                    status.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    // Chụp ảnh vệ sinh — giống Xamarin TakePicCommandVS (cleaner icon trên toolbar)
                    IconButton(onClick = onCameraVS) {
                        Icon(Icons.Default.LocalCarWash, contentDescription = "Chụp ảnh vệ sinh")
                    }
                    // Toggle khoá số container / size / operator
                    IconButton(onClick = viewModel::toggleEditing) {
                        Icon(
                            if (uiState.isEditing) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = if (uiState.isEditing) "Khoá" else "Mở khoá"
                        )
                    }
                    // Lưu — chỉ một nút lưu duy nhất, không có bước xác nhận hoàn thành giám định
                    IconButton(onClick = { viewModel.save() }, enabled = !uiState.isSaving) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "Lưu")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            // Surface chỉ bao khít vùng 4 nút — phần đệm tránh thanh điều hướng 3 nút hệ thống
            // (navigationBarsPadding) tách riêng ra ngoài, không tô màu, để không bị nhìn nhầm
            // thành "1 khối nút to" như khi đặt chung trong Surface có màu/elevation.
            Column {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Thứ tự giống Xamarin: DM (ảnh hư hỏng) - Ảnh - Điểm - AV (ảnh available)
                    BottomActionButton(
                        icon = Icons.Default.CameraAlt,
                        label = "DM",
                        color = Color(0xFFD97706),
                        modifier = Modifier.weight(1f),
                        onClick = onCameraDM
                    )
                    BottomActionButton(
                        icon = Icons.Default.Image,
                        label = "Hình ảnh",
                        color = Color(0xFF0891B2),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenImages
                    )
                    BottomActionButton(
                        icon = Icons.Default.Checklist,
                        label = "Chấm điểm",
                        color = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenChamDiem
                    )
                    BottomActionButton(
                        icon = Icons.Default.CameraAlt,
                        label = "AV",
                        color = Color(0xFF16A34A),
                        modifier = Modifier.weight(1f),
                        onClick = onCameraAV
                    )
                }
            }
            Spacer(Modifier.navigationBarsPadding().fillMaxWidth())
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        // Chỉ hiện spinner toàn màn khi CHƯA có container nào để hiện — màn hình reload mỗi lần
        // quay lại (vd: sau khi xem ảnh/chấm điểm), nếu không tách riêng thì form đang hiện sẽ
        // bị thay bằng spinner mỗi lần resume, gây chớp.
        if (uiState.isLoading && uiState.container == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                // enableEdgeToEdge() làm adjustResize (manifest) không còn tự đẩy nội dung lên khi
                // bàn phím mở — phải tự chừa chỗ bằng imePadding(), kết hợp verticalScroll để
                // trường đang nhập cuộn lên khỏi vùng bị bàn phím che
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Số container (readonly) ──────────────────────────────────────
            OutlinedTextField(
                value = uiState.containerNumber,
                onValueChange = viewModel::onContainerNumberChange,
                readOnly = !uiState.isEditing,
                label = { Text("Số container") },
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().autoBringIntoViewOnFocus()
            )

            // ── Size | Opt ───────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LookupDropdown(
                    label = "Size",
                    selected = uiState.selectedSize,
                    options = uiState.sizes,
                    enabled = uiState.isEditing,
                    onSelect = viewModel::onSizeChange,
                    modifier = Modifier.weight(1f)
                )
                LookupDropdown(
                    label = "Operator",
                    selected = uiState.selectedOpt,
                    options = uiState.opts,
                    enabled = uiState.isEditing,
                    onSelect = viewModel::onOptChange,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Grade chips ──────────────────────────────────────────────────
            ChipGroupSection(
                title = "Grade",
                items = uiState.grades,
                selectedId = uiState.selectedGradeId,
                enabled = true,
                onSelect = viewModel::onGradeChange
            )

            // ── Clean method chips ───────────────────────────────────────────
            ChipGroupSection(
                title = "Phương pháp vệ sinh",
                items = uiState.cleanMethods,
                selectedId = uiState.selectedCleanMethodId,
                enabled = true,
                onSelect = viewModel::onCleanMethodChange
            )

            // ── Năm SX ─────────────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.yearManufacture,
                onValueChange = viewModel::onYearChange,
                readOnly = false,
                label = { Text("Năm SX") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.4f).autoBringIntoViewOnFocus()
            )

            // ── Seal — chiếm trọn hàng để hiển thị đủ ký tự, số seal thường dài 8-11 ký tự ──
            OutlinedTextField(
                value = uiState.seal,
                onValueChange = viewModel::onSealChange,
                readOnly = false,
                label = { Text("Seal") },
                singleLine = true,
                trailingIcon = {
                    if (uiState.isScanningSeal) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = {
                            if (cameraPermission.status.isGranted) {
                                showSealScanDialog = true
                            } else {
                                cameraPermission.launchPermissionRequest()
                            }
                        }) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Quét số seal")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().autoBringIntoViewOnFocus()
            )

            // ── Nhập nhanh (FastFill) — chọn 1 mục sẽ tự nối CodeName vào Tình trạng ─────────
            FastFillDropdown(
                fastFills = uiState.fastFills,
                onSelect = viewModel::onFastFillSelected
            )

            // ── Tình trạng (Condition) ───────────────────────────────────────
            OutlinedTextField(
                value = uiState.tinhTrang,
                onValueChange = viewModel::onTinhTrangChange,
                label = { Text("Tình trạng") },
                placeholder = { Text("Tình trạng container hiển thị trên EIR") },
                trailingIcon = {
                    IconButton(onClick = viewModel::onCopyTinhTrangClicked) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy tình trạng")
                    }
                },
                minLines = 3,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().autoBringIntoViewOnFocus()
            )

            // ── Remark ───────────────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.remark,
                onValueChange = viewModel::onRemarkChange,
                label = { Text("Ghi chú") },
                placeholder = { Text("Những dữ liệu này không hiển thị trên EIR") },
                minLines = 2,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().autoBringIntoViewOnFocus()
            )

            // ── IsDamage | IsNeedClean ───────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CheckField(
                    label = "Hư hỏng",
                    checked = uiState.isDamage,
                    enabled = true,
                    onCheckedChange = viewModel::onIsDamageChange,
                    modifier = Modifier.weight(1f)
                )
                CheckField(
                    label = "Cần vệ sinh",
                    checked = uiState.isNeedClean,
                    enabled = true,
                    onCheckedChange = viewModel::onIsNeedCleanChange,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Error ────────────────────────────────────────────────────────
            uiState.error?.let { err ->
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        err,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    )
                }
            }
        }
    }
    } // end Box
}

// ── Chip group ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroupSection(
    title: String,
    items: List<Pair<Int, String>>,
    selectedId: Int?,
    enabled: Boolean,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        if (items.isEmpty()) {
            Text("—", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items.forEach { (id, name) ->
                    FilterChip(
                        selected = selectedId == id,
                        onClick = { if (enabled) onSelect(id) },
                        label = { Text(name, fontSize = 13.sp) },
                        enabled = enabled || selectedId == id,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}

// ── Lookup dropdown ──────────────────────────────────────────────────────────

// Nhập nhanh — chỉ là 1 thao tác "chọn để nối vào Tình trạng", không giữ giá trị đã chọn
// (giống dxe:ComboBoxEdit SelectedItem={Binding NhapNhanhSelected} của Xamarin — selection chỉ là trigger)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FastFillDropdown(
    fastFills: List<com.tc128.giamdinhnative.data.model.FastFill>,
    onSelect: (com.tc128.giamdinhnative.data.model.FastFill) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Nhập nhanh") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            fastFills.forEach { fastFill ->
                DropdownMenuItem(
                    text = { Text(fastFill.codeName) },
                    onClick = { onSelect(fastFill); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LookupDropdown(
    label: String,
    selected: String?,
    options: List<Pair<Int, String>>,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { if (enabled) ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        if (enabled) {
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = { onSelect(id); expanded = false }
                    )
                }
            }
        }
    }
}

// ── Check field ──────────────────────────────────────────────────────────────

@Composable
private fun CheckField(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (checked) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

// ── Bottom action button ─────────────────────────────────────────────────────

@Composable
private fun BottomActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(color.copy(alpha = 0.5f))
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        modifier = modifier.height(44.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = color)
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = color,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
