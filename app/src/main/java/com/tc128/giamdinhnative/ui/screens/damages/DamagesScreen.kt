package com.tc128.giamdinhnative.ui.screens.damages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.tc128.giamdinhnative.data.model.EorStatus
import com.tc128.giamdinhnative.data.model.ItemEOR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DamagesScreen(
    containerId: String,
    onBack: () -> Unit,
    onOpenCamera: ((containerId: String, itemEorId: Int) -> Unit)? = null,
    viewModel: DamagesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Reload mỗi khi quay lại màn hình (vd: sau khi chụp ảnh hư hỏng) — load() không đụng tới
    // showAddDialog/editingItem nên không mất trạng thái dialog đang mở
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(containerId, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.load(containerId)
        }
    }

    if (uiState.showAddDialog) {
        AddItemEorDialog(
            uiState = uiState,
            onDismiss = viewModel::dismissDialog,
            onSave = viewModel::saveItem,
            onComponentChange = viewModel::onComponentChange,
            onDamageCodeChange = viewModel::onDamageCodeChange,
            onRepairMethodChange = viewModel::onRepairMethodChange,
            onQtyChange = viewModel::onQtyChange,
            onLocationChange = viewModel::onLocationChange,
            onLengthChange = viewModel::onLengthChange,
            onWideChange = viewModel::onWideChange,
            onItemRepairKeywordChange = viewModel::onItemRepairKeywordChange,
            onItemRepairSelect = viewModel::onItemRepairSelect
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hư hỏng — $containerId (${uiState.items.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showAddDialog,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.items.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Chưa có mục hư hỏng", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    ItemEorCard(
                        item = item,
                        onDelete = { viewModel.deleteItem(item) },
                        onCamera = if (onOpenCamera != null && item.id > 0) {
                            { onOpenCamera(containerId, item.id) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemEorCard(
    item: ItemEOR,
    onDelete: () -> Unit,
    onCamera: (() -> Unit)?
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc muốn xóa mục hư hỏng này?") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Hủy") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.componentName ?: "Component #${item.componentId}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(item.status)
            }

            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LabelValue("Hư hỏng", item.damageCodeName ?: "—")
                LabelValue("Sửa chữa", item.repairMethodName ?: "—")
                LabelValue("SL", item.qty.toString())
            }

            if (item.length != null || item.wide != null) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    item.length?.let { LabelValue("Dài", "%.0f".format(it)) }
                    item.wide?.let { LabelValue("Rộng", "%.0f".format(it)) }
                }
            }

            item.location?.let {
                Spacer(Modifier.height(4.dp))
                LabelValue("Vị trí", it)
            }

            // Chi phí
            if (item.total != null && item.total > 0) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    item.labourHour?.let { LabelValue("Giờ công", "%.1f h".format(it)) }
                    item.total.let { LabelValue("Tổng", "%,.0f đ".format(it)) }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                onCamera?.let {
                    TextButton(onClick = it) {
                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ảnh")
                    }
                }
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Xóa")
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: EorStatus) {
    val (color, label) = when (status) {
        EorStatus.Pending -> MaterialTheme.colorScheme.secondary to "Chờ duyệt"
        EorStatus.Approval -> Color(0xFF4CAF50) to "Đã duyệt"
        EorStatus.Complete -> MaterialTheme.colorScheme.primary to "Hoàn thành"
        EorStatus.Cancel -> MaterialTheme.colorScheme.error to "Hủy"
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemEorDialog(
    uiState: DamagesUiState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onComponentChange: (Int) -> Unit,
    onDamageCodeChange: (Int) -> Unit,
    onRepairMethodChange: (Int) -> Unit,
    onQtyChange: (Int) -> Unit,
    onLocationChange: (String) -> Unit,
    onLengthChange: (Double?) -> Unit,
    onWideChange: (Double?) -> Unit,
    onItemRepairKeywordChange: (String) -> Unit,
    onItemRepairSelect: (com.tc128.giamdinhnative.data.model.ItemRepair) -> Unit
) {
    val item = uiState.editingItem ?: return
    var qtyText by remember { mutableStateOf(item.qty.toString()) }
    // Key theo itemRepairId — reset khi autocomplete tự điền, nhưng giữ nguyên khi người dùng tự gõ
    var lengthText by remember(item.itemRepairId) { mutableStateOf(item.length?.toInt()?.toString() ?: "") }
    var wideText by remember(item.itemRepairId) { mutableStateOf(item.wide?.toInt()?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm hư hỏng") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Autocomplete mẫu sửa chữa (ItemRepair) — chọn 1 mẫu tự điền các trường dưới
                ItemRepairAutocomplete(
                    keyword = uiState.itemRepairKeyword,
                    suggestions = uiState.itemRepairSuggestions,
                    components = uiState.components,
                    repairMethods = uiState.repairMethods,
                    onKeywordChange = onItemRepairKeywordChange,
                    onSelect = onItemRepairSelect
                )
                // Component
                DropdownSelector(
                    label = "Bộ phận",
                    selectedId = item.componentId,
                    options = uiState.components.map { it.id to (it.nameVn ?: it.codeName) },
                    onSelect = onComponentChange
                )
                // Damage code
                DropdownSelector(
                    label = "Hư hỏng",
                    selectedId = item.damageCodeId,
                    options = uiState.damageCodes.map { it.id to it.codeName },
                    onSelect = onDamageCodeChange
                )
                // Repair method
                DropdownSelector(
                    label = "Sửa chữa",
                    selectedId = item.repairMethodId,
                    options = uiState.repairMethods.map { it.id to it.codeName },
                    onSelect = onRepairMethodChange
                )
                // Qty
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { v ->
                        qtyText = v
                        v.toIntOrNull()?.let { onQtyChange(it) }
                    },
                    label = { Text("Số lượng") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // Length / Wide (kích thước hư hỏng) — tự điền khi chọn mẫu sửa chữa, vẫn sửa được tay
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = lengthText,
                        onValueChange = { v ->
                            lengthText = v
                            onLengthChange(v.toDoubleOrNull())
                        },
                        label = { Text("Dài") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = wideText,
                        onValueChange = { v ->
                            wideText = v
                            onWideChange(v.toDoubleOrNull())
                        },
                        label = { Text("Rộng") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Location
                OutlinedTextField(
                    value = item.location ?: "",
                    onValueChange = onLocationChange,
                    label = { Text("Vị trí") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = item.componentId != null && item.damageCodeId != null
            ) { Text("Lưu") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemRepairAutocomplete(
    keyword: String,
    suggestions: List<com.tc128.giamdinhnative.data.model.ItemRepair>,
    components: List<com.tc128.giamdinhnative.data.model.Component>,
    repairMethods: List<com.tc128.giamdinhnative.data.model.RepairMethod>,
    onKeywordChange: (String) -> Unit,
    onSelect: (com.tc128.giamdinhnative.data.model.ItemRepair) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = suggestions.isNotEmpty(),
        onExpandedChange = {}
    ) {
        OutlinedTextField(
            value = keyword,
            onValueChange = onKeywordChange,
            label = { Text("Tìm mẫu sửa chữa") },
            placeholder = { Text("VD: FPP WW") },
            singleLine = true,
            // Ascii → hầu hết bộ gõ (Laban Key, Gboard, Unikey...) tự tắt telex/composing tiếng Việt
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = suggestions.isNotEmpty(),
            onDismissRequest = {}
        ) {
            suggestions.forEach { repair ->
                val componentName = components.find { it.id == repair.componentId }?.let { it.nameVn ?: it.codeName } ?: "?"
                val repairMethodName = repairMethods.find { it.id == repair.repairMethodId }?.codeName ?: "?"
                val size = listOfNotNull(
                    repair.length?.let { "D${it}" },
                    repair.wide?.let { "R${it}" }
                ).joinToString("×").ifEmpty { null }
                DropdownMenuItem(
                    text = { Text(listOfNotNull("$componentName • $repairMethodName", size).joinToString(" • ")) },
                    onClick = { onSelect(repair) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSelector(
    label: String,
    selectedId: Int?,
    options: List<Pair<Int, String>>,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.find { it.first == selectedId }?.second ?: ""
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
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
