package com.tc128.giamdinhnative.ui.screens.newitem

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tc128.giamdinhnative.data.remote.OcrService
import com.tc128.giamdinhnative.data.remote.toUserMessage
import com.tc128.giamdinhnative.data.repository.ContainerRepository
import com.tc128.giamdinhnative.data.repository.LookupRepository
import com.tc128.giamdinhnative.data.repository.PhotoRepository
import com.tc128.giamdinhnative.util.ImageResizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewItemUiState(
    val containerNumber: String = "",
    val containerNumberError: String? = null,

    val selectedSizeId: Int? = null,
    val selectedSizeName: String = "",
    val selectedOptId: Int? = null,
    val selectedOptName: String = "",

    val sizes: List<Pair<Int, String>> = emptyList(),
    val opts: List<Pair<Int, String>> = emptyList(),

    val isLoadingLookups: Boolean = true, // true ngay từ đầu — tránh chớp khung "rỗng" trước khi load() chạy xong
    val isSaving: Boolean = false,
    val isScanning: Boolean = false,

    val savedContainerId: String? = null,
    val error: String? = null,
    val ocrTimingMessage: String? = null, // snackbar: kết quả OCR + timing, tự ẩn sau 2s

    // Ảnh chụp lúc quét OCR, lưu tạm — nếu người dùng tạo container với đúng số đã nhận diện
    // (không sửa tay) thì gắn ảnh này làm ảnh AV của container đó luôn, không cần chụp lại
    val scannedPhotoPath: String? = null,
    val scannedContainerNumber: String? = null
)

@HiltViewModel
class NewItemViewModel @Inject constructor(
    private val containerRepository: ContainerRepository,
    private val lookupRepository: LookupRepository,
    private val ocrService: OcrService,
    private val imageResizer: ImageResizer,
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewItemUiState())
    val uiState = _uiState.asStateFlow()

    // Không load ở đây — NewItemScreen gọi loadLookups() qua repeatOnLifecycle(RESUMED),
    // tự fire ngay lần đầu vào màn hình nên load ở init{} nữa sẽ bị trùng (chớp 2 lần)

    fun loadLookups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLookups = true) }
            try {
                val sizes = lookupRepository.getSizes()
                val opts = lookupRepository.getOpts()
                _uiState.update { it.copy(isLoadingLookups = false, sizes = sizes, opts = opts) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingLookups = false, error = e.message) }
            }
        }
    }

    fun onContainerNumberChange(value: String) {
        // Không ép cứng 11 ký tự — nhiều hãng có số container không theo chuẩn ISO 6346.
        // Giới hạn rộng (16) chỉ để chặn nhập bậy, không phải để chuẩn hoá định dạng.
        val filtered = value.uppercase().filter { it.isLetterOrDigit() }.take(16)
        val error = if (filtered.length == 11) validateIso6346(filtered) else null
        _uiState.update { it.copy(containerNumber = filtered, containerNumberError = error) }
    }

    fun onSizeChange(id: Int) {
        val name = _uiState.value.sizes.find { it.first == id }?.second ?: ""
        _uiState.update { it.copy(selectedSizeId = id, selectedSizeName = name) }
    }

    fun onOptChange(id: Int) {
        val name = _uiState.value.opts.find { it.first == id }?.second ?: ""
        _uiState.update { it.copy(selectedOptId = id, selectedOptName = name) }
    }

    fun dismissOcrTiming() = _uiState.update { it.copy(ocrTimingMessage = null) }

    /**
     * Gọi từ OcrCameraDialog.
     * captureStartMs = System.currentTimeMillis() lúc người dùng bấm nút chụp (T0).
     * Pipeline: T0 → [sensor capture] → onCaptureSuccess → [coroutine launch] → [resize] → [upload] → done
     */
    fun startOcrScanFromBytes(bytes: ByteArray, rotationDegrees: Int, captureStartMs: Long) {
        val coroutineStartMs = System.currentTimeMillis() // T1: sau sensor capture
        _uiState.update {
            it.copy(
                isScanning = true,
                error = null,
                containerNumber = "",
                containerNumberError = null,
                selectedSizeId = null,
                selectedSizeName = "",
                scannedPhotoPath = null,
                scannedContainerNumber = null
            )
        }

        // Lưu tạm ảnh ở 1 coroutine RIÊNG, chạy song song không chờ — không được làm chậm
        // tốc độ nhận diện OCR (coroutine OCR bên dưới chạy độc lập, không await job này)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resized = imageResizer.resizeBytes(bytes, rotationDegrees)
                val file = imageResizer.writeJpegFile(resized)
                _uiState.update { it.copy(scannedPhotoPath = file.absolutePath) }
            } catch (_: Exception) {
                // Lưu tạm thất bại không ảnh hưởng luồng quét chính — bỏ qua, không chặn gì cả
            }
        }

        viewModelScope.launch {
            try {
                val timed = ocrService.scanBytes(bytes, rotationDegrees)
                val totalMs = System.currentTimeMillis() - captureStartMs
                val captureMs = coroutineStartMs - captureStartMs  // thời gian sensor + callback
                applyOcrResult(timed, totalMs, captureMs)
            } catch (e: Exception) {
                _uiState.update { it.copy(isScanning = false, error = "Scan lỗi: ${e.message}") }
            }
        }
    }

    fun startOcrScan(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, error = null) }
            val t0 = System.currentTimeMillis()
            try {
                val timed = ocrService.scanImage(imageUri)
                val totalMs = System.currentTimeMillis() - t0
                applyOcrResult(timed, totalMs, captureMs = null)
            } catch (e: Exception) {
                _uiState.update { it.copy(isScanning = false, error = "Scan lỗi: ${e.message}") }
            }
        }
    }

    private fun applyOcrResult(
        timed: com.tc128.giamdinhnative.data.remote.OcrTimedResult,
        totalMs: Long,
        captureMs: Long?
    ) {
        val result = timed.response
        val rawNumber = result.containerNumber
            ?.uppercase()?.filter { it.isLetterOrDigit() }?.take(11) ?: ""
        val numError = if (rawNumber.length == 11) validateIso6346(rawNumber) else null

        val mappedCode = ocrService.mapSizeCode(result.sizeType)
        val matchedSize = if (mappedCode != null) {
            _uiState.value.sizes.find { (_, name) ->
                name.uppercase().trim().startsWith(mappedCode.uppercase().trim())
            }
        } else null

        val formBanner = buildString {
            if (rawNumber.isNotEmpty()) append("Số cont: $rawNumber")
            if (mappedCode != null) { if (isNotEmpty()) append(" • "); append("Size: $mappedCode") }
            if (matchedSize == null && mappedCode != null) append(" (chưa khớp)")
        }.ifEmpty { "OCR không nhận diện được" }

        // Snackbar: kết quả OCR + timing từng bước, gộp chung 1 thông báo
        val timingSnackbar = buildString {
            append(formBanner)
            append("  |  Tổng: ${totalMs}ms")
            if (captureMs != null) append("  |  Capture: ${captureMs}ms")
            append("  |  Resize: ${timed.resizeMs}ms")
            append("  |  Upload: ${timed.uploadMs}ms")
            append("  |  Server: ${timed.serverMs}ms")
            append("  |  Network: ${timed.networkMs}ms")
        }

        _uiState.update { state ->
            state.copy(
                isScanning = false,
                containerNumber = rawNumber.ifEmpty { state.containerNumber },
                containerNumberError = if (rawNumber.isNotEmpty()) numError else state.containerNumberError,
                selectedSizeId = matchedSize?.first ?: state.selectedSizeId,
                selectedSizeName = matchedSize?.second ?: state.selectedSizeName,
                ocrTimingMessage = timingSnackbar,
                scannedContainerNumber = rawNumber.ifEmpty { state.scannedContainerNumber }
            )
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.containerNumber.isBlank()) {
            _uiState.update { it.copy(error = "Vui lòng nhập số container") }
            return
        }
        // Sai chuẩn ISO 6346 (cấu trúc hoặc checksum) chỉ hiện cảnh báo, KHÔNG chặn lưu —
        // nhiều hãng tàu có số container không theo chuẩn quốc tế.
        if (state.containerNumber.length == 11) {
            _uiState.update { it.copy(containerNumberError = validateIso6346(state.containerNumber)) }
        }
        if (state.selectedOptId == null) {
            _uiState.update { it.copy(error = "Vui lòng chọn Operator") }
            return
        }
        if (state.selectedSizeId == null) {
            _uiState.update { it.copy(error = "Vui lòng chọn Size") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val created = containerRepository.createContainer(
                    containerNumber = state.containerNumber,
                    sizeId = state.selectedSizeId,
                    optId = state.selectedOptId
                )
                // Nếu số container lúc tạo vẫn khớp đúng số OCR nhận diện được (người dùng không
                // sửa tay) → gắn luôn ảnh đã chụp lúc quét làm ảnh AV của container vừa tạo,
                // không cần chụp lại
                if (state.scannedPhotoPath != null &&
                    state.scannedContainerNumber != null &&
                    state.containerNumber == state.scannedContainerNumber
                ) {
                    photoRepository.saveLocal(
                        containerNumber = created.id.toString(),
                        itemEorId = null,
                        filePath = state.scannedPhotoPath,
                        status = "Available"
                    )
                }
                // Dùng numeric id để navigate sang detail (API: api/container/{id})
                _uiState.update { it.copy(isSaving = false, savedContainerId = created.id.toString()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.toUserMessage()) }
            }
        }
    }

    /**
     * ISO 6346 check digit: AAAU999999C
     * Values: digits = face value; letters skip multiples of 11 (A=10,B=12,...Z=38)
     * sum = Σ(value[i] * 2^i) for i=0..9; check = sum%11 (10→0)
     */
    private fun validateIso6346(number: String): String? {
        if (number.length != 11) return "Phải đủ 11 ký tự"
        val owner = number.take(3)
        val category = number[3]
        val serial = number.substring(4, 10)
        val checkChar = number[10]

        if (!owner.all { it.isLetter() }) return "3 ký tự đầu phải là chữ"
        if (category !in listOf('U', 'J', 'Z')) return "Ký tự thứ 4 phải là U, J hoặc Z"
        if (!serial.all { it.isDigit() }) return "Ký tự 5–10 phải là số"
        if (!checkChar.isDigit()) return "Ký tự thứ 11 phải là số"

        val charValues = mapOf(
            '0' to 0, '1' to 1, '2' to 2, '3' to 3, '4' to 4,
            '5' to 5, '6' to 6, '7' to 7, '8' to 8, '9' to 9,
            'A' to 10, 'B' to 12, 'C' to 13, 'D' to 14, 'E' to 15,
            'F' to 16, 'G' to 17, 'H' to 18, 'I' to 19, 'J' to 20,
            'K' to 21, 'L' to 23, 'M' to 24, 'N' to 25, 'O' to 26,
            'P' to 27, 'Q' to 28, 'R' to 29, 'S' to 30, 'T' to 31,
            'U' to 32, 'V' to 34, 'W' to 35, 'X' to 36, 'Y' to 37, 'Z' to 38
        )

        val sum = number.take(10).mapIndexed { i, c ->
            (charValues[c] ?: 0) * (1 shl i)
        }.sum()

        val remainder = sum % 11
        val expected = if (remainder == 10) 0 else remainder
        val actual = checkChar.digitToInt()

        return if (actual != expected) "Check digit không hợp lệ (đúng: $expected)" else null
    }
}
