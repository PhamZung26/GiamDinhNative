package com.tc128.giamdinhnative.ui.screens.cleancontainers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tc128.giamdinhnative.data.model.Container
import com.tc128.giamdinhnative.data.repository.ContainerRepository
import com.tc128.giamdinhnative.data.repository.LookupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CleanContainersUiState(
    val containers: List<Container> = emptyList(),
    val cleanMethods: List<Pair<Int, String>> = emptyList(),
    val searchQuery: String = "",
    // Nhiều loại phương án vệ sinh được chọn cùng lúc (hiển thị gộp)
    val selectedCleanMethodIds: Set<Int> = emptySet(),
    // Giống Xamarin (IsFilterJustClean): mặc định false — chỉ hiện container chưa vệ sinh,
    // bật lên để hiện thêm container vừa vệ sinh xong
    val isFilterJustClean: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class CleanContainersViewModel @Inject constructor(
    private val containerRepository: ContainerRepository,
    private val lookupRepository: LookupRepository,
    private val sessionManager: com.tc128.giamdinhnative.session.SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CleanContainersUiState())
    val uiState = _uiState.asStateFlow()

    private var cleanMethodsLoaded = false
    // Người dùng đã tự đổi bộ lọc trong màn này chưa? Nếu chưa → mỗi lần vào màn hình sẽ đọc lại
    // cấu hình mặc định (để thay đổi ở tab Thông tin có hiệu lực ngay). Nếu đã tự lọc tay → giữ.
    private var manualOverride = false

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                if (!cleanMethodsLoaded) {
                    val cleanMethods = runCatching { lookupRepository.getCleanMethods() }.getOrDefault(emptyList())
                    cleanMethodsLoaded = true
                    _uiState.update { it.copy(cleanMethods = cleanMethods) }
                }
                // Đọc LẠI cấu hình mặc định mỗi lần vào (trừ khi đã tự lọc tay) — để cấu hình ở tab
                // Thông tin có hiệu lực ngay. Chưa cấu hình → phương án chứa "quét" (giữ hành vi cũ).
                if (!manualOverride) {
                    val cleanMethods = _uiState.value.cleanMethods
                    val configured = runCatching { sessionManager.getDefaultCleanMethodIds() }.getOrDefault(emptySet())
                    val validIds = cleanMethods.map { it.first }.toSet()
                    val defaults = configured.filter { it in validIds }.toSet().ifEmpty {
                        cleanMethods.firstOrNull { it.second.lowercase().contains("quét") }?.first
                            ?.let { setOf(it) } ?: emptySet()
                    }
                    _uiState.update { it.copy(selectedCleanMethodIds = defaults) }
                }
                _uiState.update { it.copy(containers = fetchMerged(), isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // Backend lọc theo 1 cleanMethodId → gọi từng loại đã chọn rồi gộp (loại trùng theo id container)
    private suspend fun fetchMerged(): List<Container> {
        val state = _uiState.value
        val query = state.searchQuery.ifBlank { null }
        if (state.selectedCleanMethodIds.isEmpty()) return emptyList()
        val all = LinkedHashMap<Int, Container>()
        for (methodId in state.selectedCleanMethodIds) {
            val list = runCatching {
                containerRepository.getDirtyContainersNotYetClean(
                    containerNo = query,
                    cleanMethodId = methodId,
                    isFilterJustClean = state.isFilterJustClean
                )
            }.getOrDefault(emptyList())
            for (c in list) all.putIfAbsent(c.id, c)
        }
        return all.values.sortedByDescending { it.id }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        load()
    }

    // Bật/tắt 1 loại vệ sinh trong bộ lọc hiện tại (đa chọn) — đánh dấu đã lọc tay
    fun onToggleCleanMethod(id: Int) {
        manualOverride = true
        _uiState.update {
            val set = it.selectedCleanMethodIds.toMutableSet()
            if (!set.add(id)) set.remove(id)
            it.copy(selectedCleanMethodIds = set)
        }
        load()
    }

    fun onFilterJustCleanChange(value: Boolean) {
        _uiState.update { it.copy(isFilterJustClean = value) }
        load()
    }
}
