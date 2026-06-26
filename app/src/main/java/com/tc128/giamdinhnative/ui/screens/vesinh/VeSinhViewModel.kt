package com.tc128.giamdinhnative.ui.screens.vesinh

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

data class VeSinhUiState(
    val container: Container? = null,
    val cleanMethodName: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class VeSinhViewModel @Inject constructor(
    private val containerRepository: ContainerRepository,
    private val lookupRepository: LookupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VeSinhUiState())
    val uiState = _uiState.asStateFlow()

    fun load(containerId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val cleanMethods = runCatching { lookupRepository.getCleanMethods() }.getOrDefault(emptyList())
                val container = containerRepository.getContainer(containerId)
                val cleanMethodName = cleanMethods.find { it.first == container.cleanMethodId }?.second
                    ?: container.cleanMethodName
                _uiState.update {
                    it.copy(isLoading = false, container = container, cleanMethodName = cleanMethodName)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
