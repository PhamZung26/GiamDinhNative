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
    val selectedCleanMethodId: Int? = null,
    // Giống Xamarin (IsFilterJustClean): mặc định false — chỉ hiện container chưa vệ sinh,
    // bật lên để hiện thêm container vừa vệ sinh xong
    val isFilterJustClean: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class CleanContainersViewModel @Inject constructor(
    private val containerRepository: ContainerRepository,
    private val lookupRepository: LookupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CleanContainersUiState())
    val uiState = _uiState.asStateFlow()

    private var cleanMethodsLoaded = false

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                if (!cleanMethodsLoaded) {
                    val cleanMethods = runCatching { lookupRepository.getCleanMethods() }.getOrDefault(emptyList())
                    cleanMethodsLoaded = true
                    // Giống Xamarin: mặc định lọc theo phương án vệ sinh có tên chứa "quét"
                    val defaultId = _uiState.value.selectedCleanMethodId
                        ?: cleanMethods.firstOrNull { it.second.lowercase().contains("quét") }?.first
                    _uiState.update { it.copy(cleanMethods = cleanMethods, selectedCleanMethodId = defaultId) }
                }
                val containers = containerRepository.getDirtyContainersNotYetClean(
                    containerNo = _uiState.value.searchQuery.ifBlank { null },
                    cleanMethodId = _uiState.value.selectedCleanMethodId,
                    isFilterJustClean = _uiState.value.isFilterJustClean
                )
                _uiState.update { it.copy(isLoading = false, containers = containers) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        load()
    }

    fun onCleanMethodFilterChange(id: Int?) {
        _uiState.update { it.copy(selectedCleanMethodId = id) }
        load()
    }

    fun onFilterJustCleanChange(value: Boolean) {
        _uiState.update { it.copy(isFilterJustClean = value) }
        load()
    }
}
