package com.tc128.giamdinhnative.ui.screens.chamdiem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tc128.giamdinhnative.data.model.ChamDiem
import com.tc128.giamdinhnative.data.repository.ChamDiemRepository
import com.tc128.giamdinhnative.data.repository.ContainerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// 1 dòng hiển thị trên UI = ChamDiem gốc + trạng thái check/visible cục bộ (giống Check/IsVisible
// trong model Xamarin — không gửi lên server, chỉ tồn tại trong phiên làm việc hiện tại)
data class ChamDiemRow(
    val item: ChamDiem,
    val checked: Boolean = false,
    val visible: Boolean = true
)

data class ChamDiemUiState(
    val rows: List<ChamDiemRow> = emptyList(),
    val groupNames: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val tongDiem: Int = 0,
    val yearManufacture: Int = 0,
    val tuoi: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val currentGroupName: String? get() = groupNames.getOrNull(currentIndex)
    val currentGroupRows: List<ChamDiemRow>
        get() = rows.filter { it.item.nhom == currentGroupName }
}

@HiltViewModel
class ChamDiemViewModel @Inject constructor(
    private val chamDiemRepository: ChamDiemRepository,
    private val containerRepository: ContainerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChamDiemUiState())
    val uiState = _uiState.asStateFlow()

    fun load(containerId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val template = chamDiemRepository.getTemplate()
                // Giữ đúng thứ tự nhóm xuất hiện đầu tiên trong danh sách — giống GroupBy của C#
                val groupNames = template.mapNotNull { it.nhom }.distinct()
                val rows = template.map { ChamDiemRow(item = it) }

                val yearManufacture = runCatching { containerRepository.getContainer(containerId).yearManufacture }
                    .getOrNull() ?: 0
                val tuoi = if (yearManufacture > 0) Calendar.getInstance().get(Calendar.YEAR) - yearManufacture else 0

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        rows = rows,
                        groupNames = groupNames,
                        currentIndex = 0,
                        yearManufacture = yearManufacture,
                        tuoi = tuoi
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // Port từ Xamarin ChamDiem.xaml.cs CheckBox_CheckedChanged: chỉ tác động phạm vi nhóm hiện tại
    fun onCheckChanged(row: ChamDiemRow, checked: Boolean) {
        val state = _uiState.value
        val clicked = row.item

        val updated = state.rows.map { r ->
            when {
                r.item.id == clicked.id -> r.copy(checked = checked)
                r.item.nhom != clicked.nhom -> r
                clicked.diemSo == 0 ->
                    // Chọn mục 0 điểm → ẩn/hiện toàn bộ mục có điểm khác 0 trong nhóm
                    if (r.item.diemSo != 0) r.copy(visible = !checked) else r
                else ->
                    // Chọn mục có điểm → chỉ ẩn/hiện các mục cùng ChiTiet (loại trừ nhau)
                    if (r.item.chiTiet == clicked.chiTiet) r.copy(visible = !checked) else r
            }
        }

        // Tổng điểm tính trên TOÀN BỘ danh sách (mọi nhóm), không chỉ nhóm hiện tại,
        // và tính cả mục đang bị ẩn nếu vẫn đang checked — đúng hành vi Xamarin
        val tongDiem = updated.filter { it.checked }.sumOf { it.item.diemSo }

        _uiState.update { it.copy(rows = updated, tongDiem = tongDiem) }
    }

    fun truoc() {
        _uiState.update { if (it.currentIndex > 0) it.copy(currentIndex = it.currentIndex - 1) else it }
    }

    fun sau() {
        _uiState.update {
            if (it.currentIndex < it.groupNames.size - 1) it.copy(currentIndex = it.currentIndex + 1) else it
        }
    }
}
