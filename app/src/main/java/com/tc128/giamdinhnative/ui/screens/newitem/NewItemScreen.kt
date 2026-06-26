package com.tc128.giamdinhnative.ui.screens.newitem

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun NewItemScreen(
    onBack: () -> Unit,
    onCreated: (containerId: String) -> Unit,
    viewModel: NewItemViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    var showOcrCamera by remember { mutableStateOf(false) }

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    // Reload danh mục Size/Opt mỗi khi quay lại màn hình — không đụng tới các trường form đang nhập
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadLookups()
        }
    }

    LaunchedEffect(uiState.savedContainerId) {
        uiState.savedContainerId?.let { onCreated(it) }
    }

    // Snackbar kết quả OCR + timing, kèm rung nhẹ, tự ẩn sau 2s
    LaunchedEffect(uiState.ocrTimingMessage) {
        uiState.ocrTimingMessage?.let { msg ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val job = launch {
                snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Indefinite)
            }
            delay(2000)
            job.cancel()
            snackbarHostState.currentSnackbarData?.dismiss()
            viewModel.dismissOcrTiming()
        }
    }

    // OCR Camera dialog (CameraX, không có màn hình xác nhận)
    if (showOcrCamera) {
        OcrCameraDialog(
            onCapture = { bytes, rotation, captureStartMs ->
                showOcrCamera = false
                viewModel.startOcrScanFromBytes(bytes, rotation, captureStartMs)
            },
            onDismiss = { showOcrCamera = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 52.dp,
                title = { Text("Tạo container mới", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    TextButton(
                        onClick = viewModel::save,
                        enabled = !uiState.isSaving && !uiState.isLoadingLookups && !uiState.isScanning
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Tạo", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(bottom = 90.dp),
                    content = { Text(data.visuals.message, softWrap = true) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Số container ───────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.containerNumber,
                onValueChange = viewModel::onContainerNumberChange,
                label = { Text("Số container *") },
                placeholder = { Text("VD: TCKU1234567") },
                isError = uiState.containerNumberError != null,
                supportingText = {
                    if (uiState.containerNumberError != null) {
                        Text(uiState.containerNumberError!!, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("${uiState.containerNumber.length}/11",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                trailingIcon = {
                    if (uiState.containerNumber.length == 11 && uiState.containerNumberError == null) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF16A34A))
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    // Ascii — tắt telex/composing tiếng Việt của hầu hết bộ gõ (Laban Key, Gboard, Unikey...)
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done
                ),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // ── Lookups ────────────────────────────────────────────────────
            if (uiState.isLoadingLookups) {
                Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else {
                FilterableLookupDropdown(
                    label = "Size *",
                    selected = uiState.selectedSizeName,
                    options = uiState.sizes,
                    onSelect = viewModel::onSizeChange
                )
                FilterableLookupDropdown(
                    label = "Operator *",
                    selected = uiState.selectedOptName,
                    options = uiState.opts,
                    onSelect = viewModel::onOptChange
                )
            }

            // ── Error banner ───────────────────────────────────────────────
            uiState.error?.let { error ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Nút hành động (body) ───────────────────────────────────────
            // Scan trái — Tạo phải: thuận cả tay trái lẫn tay phải
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Nút Scan OCR (trái)
                OutlinedButton(
                    onClick = {
                        if (cameraPermission.status.isGranted) {
                            showOcrCamera = true
                        } else {
                            cameraPermission.launchPermissionRequest()
                        }
                    },
                    enabled = !uiState.isScanning,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    if (uiState.isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Đang scan...")
                    } else {
                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Scan OCR")
                    }
                }

                // Nút Tạo container (phải)
                Button(
                    onClick = viewModel::save,
                    enabled = !uiState.isSaving && !uiState.isScanning,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Đang tạo...")
                    } else {
                        Text("Tạo container", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// Vừa gõ để lọc vừa chọn từ danh sách — giống dxe:ComboBoxEdit IsFilterEnabled="True" của Xamarin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterableLookupDropdown(
    label: String,
    selected: String,
    options: List<Pair<Int, String>>,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // Đồng bộ lại khi selected đổi từ ngoài (vd: OCR tự điền Size) nhưng vẫn giữ
    // được nội dung người dùng đang gõ trong lúc họ chưa chọn xong
    var query by remember(selected) { mutableStateOf(selected) }

    val filtered = remember(query, options) {
        if (query.isBlank()) options
        else options.filter { it.second.contains(query, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; expanded = true },
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        if (filtered.isNotEmpty()) {
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                filtered.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            onSelect(id)
                            query = name
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
