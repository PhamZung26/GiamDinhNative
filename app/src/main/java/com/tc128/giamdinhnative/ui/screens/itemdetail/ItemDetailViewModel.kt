package com.tc128.giamdinhnative.ui.screens.itemdetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tc128.giamdinhnative.data.model.Container
import com.tc128.giamdinhnative.data.model.FastFill
import com.tc128.giamdinhnative.data.remote.OcrService
import com.tc128.giamdinhnative.data.remote.toUserMessage
import com.tc128.giamdinhnative.data.repository.ContainerRepository
import com.tc128.giamdinhnative.data.repository.LookupRepository
import com.tc128.giamdinhnative.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class ItemDetailUiState(
    val container: Container? = null,

    val grades: List<Pair<Int, String>> = emptyList(),
    val sizes: List<Pair<Int, String>> = emptyList(),
    val opts: List<Pair<Int, String>> = emptyList(),
    val cleanMethods: List<Pair<Int, String>> = emptyList(),
    val fastFills: List<FastFill> = emptyList(),

    val selectedGradeId: Int? = null,
    val selectedGrade: String? = null,
    val selectedSizeId: Int? = null,
    val selectedSize: String? = null,
    val selectedOptId: Int? = null,
    val selectedOpt: String? = null,
    val selectedCleanMethodId: Int? = null,
    val selectedCleanMethod: String? = null,

    val containerNumber: String = "",
    val seal: String = "",
    val yearManufacture: String = "",
    val tinhTrang: String = "",
    val remark: String = "",
    val isDamage: Boolean = false,
    val isNeedClean: Boolean = false,

    val isEditing: Boolean = false,
    // true ngay khi bất kỳ trường nào bị sửa (không chỉ khi mở khoá Size/Opt/Số cont) — dùng để
    // chặn reload-khi-resume ghi đè mất dữ liệu đang nhập dở (vd: sửa Seal rồi đi chụp ảnh rồi
    // quay lại). Khác với isEditing (chỉ điều khiển khoá Size/Opt/Số cont).
    val isDirty: Boolean = false,
    val isLoading: Boolean = true, // true ngay từ đầu — tránh chớp khung "rỗng" trước khi load() chạy xong
    val isSaving: Boolean = false,
    val isScanningSeal: Boolean = false,
    val saveMessage: String? = null,
    val error: String? = null,
    val requiresLogin: Boolean = false  // true khi token hết hạn
)

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    private val containerRepository: ContainerRepository,
    private val lookupRepository: LookupRepository,
    private val ocrService: OcrService,
    private val photoRepository: PhotoRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemDetailUiState())
    val uiState = _uiState.asStateFlow()

    fun load(containerId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load lookups và container song song để lookups hiện ngay dù container có lỗi
            val gradesD      = async { runCatching { lookupRepository.getGrades() } }
            val sizesD       = async { runCatching { lookupRepository.getSizes() } }
            val optsD        = async { runCatching { lookupRepository.getOpts() } }
            val cleanMethodsD = async { runCatching { lookupRepository.getCleanMethods() } }
            val fastFillsD   = async { runCatching { lookupRepository.getFastFills() } }
            val containerD   = async { runCatching { containerRepository.getContainer(containerId) } }

            val grades       = gradesD.await().getOrDefault(emptyList())
            val sizes        = sizesD.await().getOrDefault(emptyList())
            val opts         = optsD.await().getOrDefault(emptyList())
            val cleanMethods = cleanMethodsD.await().getOrDefault(emptyList())
            val fastFills    = fastFillsD.await().getOrDefault(emptyList())
            val containerResult = containerD.await()
            val container    = containerResult.getOrNull()

            val isAuth401 = containerResult.exceptionOrNull()
                ?.let { it is HttpException && it.code() == 401 } == true

            // Resolve tên từ lookup list — phòng khi API không embed nested objects
            fun List<Pair<Int, String>>.nameFor(id: Int?) =
                if (id != null) find { it.first == id }?.second else null

            _uiState.update {
                it.copy(
                    isLoading = false,
                    container = container,
                    grades = grades,
                    sizes = sizes,
                    opts = opts,
                    cleanMethods = cleanMethods,
                    fastFills = fastFills,
                    selectedGradeId = container?.gradeId,
                    selectedGrade = grades.nameFor(container?.gradeId) ?: container?.gradeName,
                    selectedSizeId = container?.sizeId,
                    selectedSize = sizes.nameFor(container?.sizeId) ?: container?.sizeName,
                    selectedOptId = container?.optId,
                    selectedOpt = opts.nameFor(container?.optId) ?: container?.optName,
                    selectedCleanMethodId = container?.cleanMethodId,
                    selectedCleanMethod = cleanMethods.nameFor(container?.cleanMethodId) ?: container?.cleanMethodName,
                    containerNumber = container?.containerNumber ?: "",
                    seal = container?.seal ?: "",
                    yearManufacture = container?.yearManufacture?.toString() ?: "",
                    tinhTrang = container?.tinhTrang ?: "",
                    remark = container?.remark ?: "",
                    isDamage = container?.isDamage ?: false,
                    isNeedClean = container?.isDirty ?: false,
                    isDirty = false,
                    error = if (isAuth401) null
                            else containerResult.exceptionOrNull()?.message,
                    requiresLogin = isAuth401
                )
            }
        }
    }

    fun toggleEditing() = _uiState.update { it.copy(isEditing = !it.isEditing) }

    fun onGradeChange(id: Int) {
        val name = _uiState.value.grades.find { it.first == id }?.second ?: ""
        val isDamage = name.uppercase().trimStart().startsWith("D")
        _uiState.update { it.copy(selectedGradeId = id, selectedGrade = name, isDamage = isDamage, isDirty = true) }
    }

    fun onSizeChange(id: Int) {
        val name = _uiState.value.sizes.find { it.first == id }?.second
        _uiState.update { it.copy(selectedSizeId = id, selectedSize = name, isDirty = true) }
    }

    fun onOptChange(id: Int) {
        val name = _uiState.value.opts.find { it.first == id }?.second
        _uiState.update { it.copy(selectedOptId = id, selectedOpt = name, isDirty = true) }
    }

    fun onCleanMethodChange(id: Int) {
        val name = _uiState.value.cleanMethods.find { it.first == id }?.second ?: ""
        val upper = name.uppercase()
        val isNeedClean = upper.contains("NƯỚC") || upper.contains("CÔNG")
        _uiState.update { it.copy(selectedCleanMethodId = id, selectedCleanMethod = name, isNeedClean = isNeedClean, isDirty = true) }
    }

    // Port từ Xamarin NhapNhanhSelectedCommand: chọn 1 mục "Nhập nhanh" tự nối CodeName vào Tình trạng,
    // rồi chạy ChuyenGradeBocTem() để tự chuyển Grade qua hậu tố "T" nếu tình trạng có "BÓC TEM"
    fun onFastFillSelected(fastFill: FastFill) {
        _uiState.update { state ->
            val newTinhTrang = (state.tinhTrang + ", " + fastFill.codeName).replace("- ,", "-")
            state.copy(tinhTrang = newTinhTrang, isDirty = true)
        }
        chuyenGradeBocTem()
    }

    private fun chuyenGradeBocTem() {
        val state = _uiState.value
        if (!state.tinhTrang.uppercase().contains("BÓC TEM")) return
        val currentGradeName = state.selectedGrade ?: return
        if (currentGradeName.endsWith("T", ignoreCase = true)) return
        val gradeLetter = currentGradeName.take(1)
        val targetGrade = state.grades.find { it.second.equals(gradeLetter + "T", ignoreCase = true) }
        if (targetGrade != null) {
            val isDamage = targetGrade.second.uppercase().startsWith("D")
            _uiState.update {
                it.copy(selectedGradeId = targetGrade.first, selectedGrade = targetGrade.second, isDamage = isDamage)
            }
        }
    }

    fun onContainerNumberChange(v: String) = _uiState.update {
        it.copy(containerNumber = v.uppercase().filter(Char::isLetterOrDigit).take(11), isDirty = true)
    }
    fun onSealChange(v: String) = _uiState.update { it.copy(seal = v, isDirty = true) }
    fun onYearChange(v: String) = _uiState.update { it.copy(yearManufacture = v.filter(Char::isDigit).take(4), isDirty = true) }
    fun onTinhTrangChange(v: String) = _uiState.update { it.copy(tinhTrang = v, isDirty = true) }
    fun onRemarkChange(v: String) = _uiState.update { it.copy(remark = v, isDirty = true) }
    fun onIsDamageChange(v: Boolean) = _uiState.update { it.copy(isDamage = v, isDirty = true) }
    fun onIsNeedCleanChange(v: Boolean) = _uiState.update { it.copy(isNeedClean = v, isDirty = true) }
    fun dismissSaveMessage() = _uiState.update { it.copy(saveMessage = null) }
    fun dismissError() = _uiState.update { it.copy(error = null) }

    // Port từ Xamarin GiamDinhViewModel.Scanseal() — chụp 1 ảnh, gửi OCR.space, điền kết quả vào Seal
    fun scanSeal(bytes: ByteArray, rotationDegrees: Int) {
        val container = _uiState.value.container
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningSeal = true, error = null) }
            try {
                val result = ocrService.scanSeal(bytes, rotationDegrees)
                // Giống Xamarin Scanseal() -> photoService.TakePic(): ảnh quét seal cũng được lưu
                // làm ảnh container (status Available), không chỉ dùng để OCR rồi bỏ.
                // containerNumber của PhotoEntity lưu ID số (dạng String) — khớp convention dùng
                // ở CameraViewModel/PhotoUploadWorker, KHÔNG dùng container.containerNumber (số cont thật)
                container?.let {
                    photoRepository.saveLocal(
                        containerNumber = it.id.toString(),
                        itemEorId = null,
                        filePath = result.filePath,
                        status = "Available",
                        isSeal = true,
                        sealNumber = result.sealNo.ifBlank { null }
                    )
                }
                _uiState.update {
                    it.copy(
                        isScanningSeal = false,
                        seal = result.sealNo.ifBlank { it.seal },
                        isDirty = it.isDirty || result.sealNo.isNotBlank(),
                        saveMessage = if (result.sealNo.isNotBlank()) "Đã nhận dạng được số chì: ${result.sealNo}" else "Không nhận dạng được số chì"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isScanningSeal = false, error = e.message) }
            }
        }
    }

    // Giống Xamarin SaveData(): chỉ lưu các trường đã chỉnh sửa, không tự đổi StatusOfContainer
    fun save() {
        chuyenGradeBocTem()
        val state = _uiState.value
        val container = state.container ?: return
        copyTinhTrangToClipboard()
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                containerRepository.updateContainer(
                    containerId = container.id,
                    containerNumber = state.containerNumber.ifBlank { null },
                    gradeId = state.selectedGradeId,
                    sizeId = state.selectedSizeId,
                    optId = state.selectedOptId,
                    cleanMethodId = state.selectedCleanMethodId,
                    remark = state.remark.ifBlank { null },
                    tinhTrang = state.tinhTrang.ifBlank { null },
                    seal = state.seal.ifBlank { null },
                    yearManufacture = state.yearManufacture.toIntOrNull(),
                    isDamage = state.isDamage,
                    isDirty = state.isNeedClean
                )
                _uiState.update { it.copy(isSaving = false, isDirty = false, saveMessage = "Lưu dữ liệu thành công") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.toUserMessage()) }
            }
        }
    }

    // Nút copy tay cạnh ô Tình trạng — giống StartIconCommand="{Binding CopyToClipboardCommand}" của Xamarin.
    // Từ Android 13 (API 33) hệ thống tự hiện overlay "Copied to clipboard" mỗi khi setPrimaryClip(),
    // nên chỉ cần tự hiện snackbar của app trên các bản cũ hơn — tránh hiện 2 thông báo trùng nhau.
    fun onCopyTinhTrangClicked() {
        copyTinhTrangToClipboard()
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            _uiState.update { it.copy(saveMessage = "Đã sao chép tình trạng") }
        }
    }

    // Port từ Xamarin SendTextToClipboard()/Container.TinhTrangRaw(): copy tình trạng dạng raw
    // vào clipboard — gọi tự động lúc save() và có thể gọi tay qua nút copy cạnh ô Tình trạng
    private fun copyTinhTrangToClipboard() {
        val state = _uiState.value
        if (state.containerNumber.isBlank()) return
        val raw = listOf(
            state.containerNumber,
            state.selectedSize ?: "",
            state.selectedGrade ?: "",
            state.seal,
            state.selectedCleanMethod ?: "",
            state.tinhTrang,
            if (state.isDamage) "DM" else "AV"
        ).joinToString(" - ")

        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("TinhTrang", raw))
    }
}
