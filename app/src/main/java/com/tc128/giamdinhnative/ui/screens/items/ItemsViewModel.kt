package com.tc128.giamdinhnative.ui.screens.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tc128.giamdinhnative.data.model.Container
import com.tc128.giamdinhnative.data.repository.ContainerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ItemsUiState(
    val containers: List<Container> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true, // true ngay từ đầu — tránh chớp khung "rỗng" trước khi load() chạy xong
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class ItemsViewModel @Inject constructor(
    private val containerRepository: ContainerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemsUiState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        // Không load ở đây — ItemsScreen gọi refresh() qua repeatOnLifecycle(RESUMED),
        // tự fire ngay lần đầu vào màn hình nên load ở init{} nữa sẽ bị trùng (chớp 2 lần)
        _searchQuery
            .debounce(400)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isBlank()) loadContainers()
                else searchContainers(query)
            }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        if (_uiState.value.searchQuery.isBlank()) loadContainers()
        else searchContainers(_uiState.value.searchQuery)
    }

    fun onSearchChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _searchQuery.value = query
    }

    private fun loadContainers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, hasMore = true) }
            try {
                val containers = containerRepository.getContainers()
                // hasMore = true nếu server trả về data; false chỉ khi list rỗng
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        containers = containers,
                        hasMore = containers.isNotEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore || state.searchQuery.isNotBlank()) return
        val lastId = state.containers.lastOrNull()?.id?.takeIf { it > 0 } ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val more = containerRepository.getMoreContainers(lastId)
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        containers = it.containers + more,
                        // Dừng khi server trả về list rỗng
                        hasMore = more.isNotEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    private fun searchContainers(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val results = containerRepository.searchContainers(query)
                _uiState.update { it.copy(isLoading = false, containers = results, hasMore = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
