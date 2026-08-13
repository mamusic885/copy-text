package com.example.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.OcrLanguage
import com.example.ocr.OcrEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OcrUiState(
    val selectedImageUri: Uri? = null,
    val extractedText: String = "",
    val selectedLanguage: OcrLanguage = OcrLanguage.AUTO,
    val isScanning: Boolean = false,
    val errorMessage: String? = null,
    val userNotice: String? = null,
    val showImagePreviewModal: Boolean = false,
    val wordCount: Int = 0,
    val charCount: Int = 0,
    val lineCount: Int = 0
)

class OcrViewModel : ViewModel() {

    private val ocrEngine = OcrEngine()

    private val _uiState = MutableStateFlow(OcrUiState())
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    var tempCameraUri: Uri? = null

    fun selectLanguage(context: Context, language: OcrLanguage) {
        if (_uiState.value.selectedLanguage == language) return
        
        _uiState.update { it.copy(selectedLanguage = language) }

        // Re-scan current image if present
        _uiState.value.selectedImageUri?.let { uri ->
            processImage(context, uri)
        }
    }

    fun processImage(context: Context, uri: Uri) {
        _uiState.update {
            it.copy(
                selectedImageUri = uri,
                isScanning = true,
                errorMessage = null,
                userNotice = null
            )
        }

        viewModelScope.launch {
            try {
                val currentLanguage = _uiState.value.selectedLanguage
                val textResult = ocrEngine.recognizeText(context, uri, currentLanguage)

                val trimmedText = textResult.trim()
                if (trimmedText.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            extractedText = "",
                            isScanning = false,
                            userNotice = "No readable text found in this image. Try taking a clearer photo or switching language mode.",
                            wordCount = 0,
                            charCount = 0,
                            lineCount = 0
                        )
                    }
                } else {
                    updateStats(trimmedText)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        errorMessage = e.localizedMessage ?: "Failed to extract text from image. Please try again."
                    )
                }
            }
        }
    }

    fun updateExtractedText(newText: String) {
        updateStats(newText)
    }

    private fun updateStats(text: String) {
        val chars = text.length
        val words = if (text.isBlank()) 0 else text.trim().split("\\s+".toRegex()).size
        val lines = if (text.isBlank()) 0 else text.lines().size

        _uiState.update {
            it.copy(
                extractedText = text,
                isScanning = false,
                wordCount = words,
                charCount = chars,
                lineCount = lines
            )
        }
    }

    fun copyToClipboard(context: Context): Boolean {
        val text = _uiState.value.extractedText
        if (text.isBlank()) return false

        return try {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Extracted Text", text)
            clipboardManager.setPrimaryClip(clip)
            _uiState.update { it.copy(userNotice = "Copied complete text to clipboard!") }
            true
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Could not copy text: ${e.localizedMessage}") }
            false
        }
    }

    fun shareText(context: Context) {
        val text = _uiState.value.extractedText
        if (text.isBlank()) return

        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(intent, "Share Extracted Text")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Failed to open share dialog.") }
        }
    }

    fun scanAgain() {
        _uiState.update {
            OcrUiState(selectedLanguage = it.selectedLanguage)
        }
    }

    fun clearText() {
        _uiState.update {
            it.copy(
                extractedText = "",
                wordCount = 0,
                charCount = 0,
                lineCount = 0
            )
        }
    }

    fun toggleImagePreviewModal(show: Boolean) {
        _uiState.update { it.copy(showImagePreviewModal = show) }
    }

    fun dismissUserNotice() {
        _uiState.update { it.copy(userNotice = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        ocrEngine.close()
    }
}
