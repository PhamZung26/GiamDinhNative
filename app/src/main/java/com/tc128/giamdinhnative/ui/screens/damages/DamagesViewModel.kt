package com.tc128.giamdinhnative.ui.screens.damages

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tc128.giamdinhnative.data.model.Component
import com.tc128.giamdinhnative.data.model.DamageCode
import com.tc128.giamdinhnative.data.model.ItemEOR
import com.tc128.giamdinhnative.data.model.ItemEorRef
import com.tc128.giamdinhnative.data.model.ItemRepair
import com.tc128.giamdinhnative.data.model.Payer
import com.tc128.giamdinhnative.data.model.RepairMethod
import com.tc128.giamdinhnative.data.repository.ItemEorRepository
import com.tc128.giamdinhnative.data.repository.LookupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DamagesUiState(
    val items: List<ItemEOR> = emptyList(),
    val components: List<Component> = emptyList(),
    val damageCodes: List<DamageCode> = emptyList(),
    val repairMethods: List<RepairMethod> = emptyList(),
    val itemRepairs: List<ItemRepair> = emptyList(),
    val itemRepairKeyword: String = "",
    val itemRepairSuggestions: List<ItemRepair> = emptyList(),
    val isLoading: Boolean = true, // true ngay từ đầu — tránh chớp khung "rỗng" trước khi load() chạy xong
    val showAddDialog: Boolean = false,
    val editingItem: ItemEOR? = null,
    val error: String? = null
)

@HiltViewModel
class DamagesViewModel @Inject constructor(
    private val itemEorRepository: ItemEorRepository,
    private val lookupRepository: LookupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DamagesUiState())
    val uiState = _uiState.asStateFlow()

    private var containerId: String = ""

    // Inverted index dựng 1 lần khi load xong, thay cho quét tuyến tính mỗi keystroke:
    // - tokenIndex: token (1 từ trong componentCode/repairMethodCode, lowercase) → các ItemRepair chứa token đó.
    //   Tra đúng token là O(1) hash lookup; nếu không trúng tuyệt đối thì fallback quét "contains" nhưng chỉ
    //   trên số TOKEN PHÂN BIỆT (thường rất nhỏ so với số dòng catalog vì nhiều dòng dùng chung code), không phải trên n dòng.
    // - numericIndex: giá trị Length/Wide → các ItemRepair có giá trị đó (exact match, O(1)).
    private data class IndexedItemRepair(
        val repair: ItemRepair,
        val componentCode: String,
        val repairMethodCode: String
    )
    private var itemRepairIndex: List<IndexedItemRepair> = emptyList()
    private var tokenIndex: Map<String, List<IndexedItemRepair>> = emptyMap()
    private var numericIndex: Map<Int, List<IndexedItemRepair>> = emptyMap()
    private var itemRepairSearchJob: Job? = null

    fun load(containerId: String) {
        this.containerId = containerId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val numericId = containerId.toIntOrNull() ?: 0
                val items = itemEorRepository.getByContainer(numericId)
                val components = lookupRepository.getComponents()
                val damageCodes = lookupRepository.getDamageCodes()
                val repairMethods = lookupRepository.getRepairMethods()
                val itemRepairs = try {
                    lookupRepository.getItemRepairs()
                } catch (e: Exception) {
                    Log.e("DamagesViewModel", "Failed to load itemRepairs", e)
                    emptyList()
                }
                val componentsById = components.associateBy { it.id }
                val repairMethodsById = repairMethods.associateBy { it.id }
                itemRepairIndex = itemRepairs.map { repair ->
                    IndexedItemRepair(
                        repair = repair,
                        componentCode = componentsById[repair.componentId]?.codeName ?: "",
                        repairMethodCode = repairMethodsById[repair.repairMethodId]?.codeName ?: ""
                    )
                }
                buildSearchIndexes()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = items,
                        components = components,
                        damageCodes = damageCodes,
                        repairMethods = repairMethods,
                        itemRepairs = itemRepairs
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun showAddDialog() = _uiState.update {
        it.copy(
            showAddDialog = true,
            editingItem = ItemEOR(containerId = containerId.toIntOrNull() ?: 0, payer = Payer.U),
            itemRepairKeyword = "",
            itemRepairSuggestions = emptyList()
        )
    }

    fun dismissDialog() = _uiState.update {
        it.copy(showAddDialog = false, editingItem = null, itemRepairKeyword = "", itemRepairSuggestions = emptyList())
    }

    // Tách componentCode/repairMethodCode của từng dòng catalog thành token (theo khoảng trắng),
    // build inverted index 1 lần. Số token phân biệt thường nhỏ hơn nhiều số dòng catalog
    // (nhiều dòng dùng chung 1 component/repairMethod), nên cả fallback contains-scan cũng rẻ hơn quét n dòng.
    private fun buildSearchIndexes() {
        val tokenBuilder = mutableMapOf<String, MutableList<IndexedItemRepair>>()
        val numericBuilder = mutableMapOf<Int, MutableList<IndexedItemRepair>>()
        itemRepairIndex.forEach { indexed ->
            "${indexed.componentCode} ${indexed.repairMethodCode}"
                .lowercase()
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }
                .distinct()
                .forEach { token -> tokenBuilder.getOrPut(token) { mutableListOf() }.add(indexed) }
            indexed.repair.length?.let { numericBuilder.getOrPut(it) { mutableListOf() }.add(indexed) }
            indexed.repair.wide?.let { numericBuilder.getOrPut(it) { mutableListOf() }.add(indexed) }
        }
        tokenIndex = tokenBuilder
        numericIndex = numericBuilder
    }

    // Tra 1 từ khóa qua index: số → numericIndex (O(1) exact); chữ → exact token (O(1)),
    // fallback contains-scan trên tokenIndex.keys (chỉ quét số token phân biệt, không phải số dòng catalog).
    private fun lookupKeyword(keyword: String): Set<IndexedItemRepair> {
        val asNumber = keyword.toIntOrNull()
        if (asNumber != null) return numericIndex[asNumber]?.toSet() ?: emptySet()
        val lower = keyword.lowercase()
        tokenIndex[lower]?.let { return it.toSet() }
        return tokenIndex.entries
            .asSequence()
            .filter { it.key.contains(lower) }
            .flatMap { it.value.asSequence() }
            .toSet()
    }

    // Lọc giống MAUI: tách từ khóa theo khoảng trắng, AND logic giữa các từ (giao các kết quả).
    fun onItemRepairKeywordChange(keyword: String) {
        _uiState.update { it.copy(itemRepairKeyword = keyword) }
        itemRepairSearchJob?.cancel()
        itemRepairSearchJob = viewModelScope.launch {
            delay(150) // debounce — tránh lọc trên mỗi keystroke khi gõ nhanh
            val keywords = keyword.trim().split(" ").filter { it.isNotBlank() }
            val suggestions = if (keywords.isEmpty()) emptyList() else withContext(Dispatchers.Default) {
                keywords.map { lookupKeyword(it) }
                    .reduceOrNull { a, b -> a intersect b }
                    .orEmpty()
                    .asSequence()
                    .take(30) // chặn render quá nhiều gợi ý khi catalog lớn
                    .map { it.repair }
                    .toList()
            }
            _uiState.update { it.copy(itemRepairSuggestions = suggestions) }
        }
    }

    // Chọn 1 mẫu sửa chữa → tự điền Component/RepairMethod/Length/Wide/STS + lưu ItemRepairID
    fun onItemRepairSelect(repair: ItemRepair) {
        val state = _uiState.value
        val component = state.components.find { it.id == repair.componentId }
        val repairMethod = state.repairMethods.find { it.id == repair.repairMethodId }
        val componentLabel = component?.let { it.nameVn ?: it.codeName }
        val repairMethodLabel = repairMethod?.codeName
        _uiState.update {
            it.copy(
                editingItem = it.editingItem?.copy(
                    componentId = repair.componentId,
                    component = component?.toRef(),
                    repairMethodId = repair.repairMethodId,
                    repairMethod = repairMethod?.toRef(),
                    itemRepairId = repair.id,
                    length = repair.length?.toDouble(),
                    wide = repair.wide?.toDouble(),
                    sts = repair.sts
                ),
                itemRepairKeyword = listOfNotNull(componentLabel, repairMethodLabel).joinToString(" "),
                itemRepairSuggestions = emptyList()
            )
        }
    }

    fun onComponentChange(componentId: Int) {
        val component = _uiState.value.components.find { it.id == componentId }
        _uiState.update { it.copy(editingItem = it.editingItem?.copy(componentId = componentId, component = component?.toRef())) }
    }

    fun onDamageCodeChange(id: Int) {
        val damageCode = _uiState.value.damageCodes.find { it.id == id }
        _uiState.update { it.copy(editingItem = it.editingItem?.copy(damageCodeId = id, damageCode = damageCode?.toRef())) }
    }

    fun onRepairMethodChange(id: Int) {
        val repairMethod = _uiState.value.repairMethods.find { it.id == id }
        _uiState.update { it.copy(editingItem = it.editingItem?.copy(repairMethodId = id, repairMethod = repairMethod?.toRef())) }
    }

    fun onLengthChange(length: Double?) {
        _uiState.update { it.copy(editingItem = it.editingItem?.copy(length = length)) }
    }

    fun onWideChange(wide: Double?) {
        _uiState.update { it.copy(editingItem = it.editingItem?.copy(wide = wide)) }
    }

    fun onQtyChange(qty: Int) {
        _uiState.update { it.copy(editingItem = it.editingItem?.copy(qty = qty)) }
    }

    fun onLocationChange(location: String) {
        _uiState.update { it.copy(editingItem = it.editingItem?.copy(location = location)) }
    }

    fun saveItem() {
        val item = _uiState.value.editingItem ?: return
        viewModelScope.launch {
            try {
                itemEorRepository.save(item)
                dismissDialog()
                load(containerId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteItem(item: ItemEOR) {
        viewModelScope.launch {
            try {
                itemEorRepository.delete(item.id)
                load(containerId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}

private fun Component.toRef() = ItemEorRef(id, codeName, nameVn)
private fun DamageCode.toRef() = ItemEorRef(id, codeName, name)
private fun RepairMethod.toRef() = ItemEorRef(id, codeName, name)
