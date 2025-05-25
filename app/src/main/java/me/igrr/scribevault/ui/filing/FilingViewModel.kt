package me.igrr.scribevault.ui.filing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import me.igrr.scribevault.data.preferences.UserPreferencesRepository
import me.igrr.scribevault.data.vault.VaultManager
import me.igrr.scribevault.domain.ocr.OcrResult

data class FilingSuccess(
    val vaultName: String,
    val filePath: String
)

class FilingViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = UserPreferencesRepository(getApplication())
    private val vaultManager = VaultManager(getApplication(), preferencesRepository)

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    private val _content = MutableLiveData<String>()
    val content: LiveData<String> = _content

    private val _selectedFolder = MutableLiveData<String>()
    val selectedFolder: LiveData<String> = _selectedFolder

    private val _availableFolders = MutableLiveData<List<String>>()
    val availableFolders: LiveData<List<String>> = _availableFolders

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _filingSuccess = MutableLiveData<FilingSuccess?>()
    val filingSuccess: LiveData<FilingSuccess?> = _filingSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var ocrResult: OcrResult? = null

    init {
        // Set default folder
        _selectedFolder.value = "Work/Inbox"
        // Load available folders (dummy for now)
        loadAvailableFolders()
    }

    fun initializeData(ocrResult: OcrResult) {
        this.ocrResult = ocrResult
        
        _title.value = ocrResult.title
        _content.value = ocrResult.content
    }

    fun updateTitle(newTitle: String) {
        _title.value = newTitle
    }

    fun updateContent(newContent: String) {
        _content.value = newContent
    }

    fun updateSelectedFolder(folder: String) {
        _selectedFolder.value = folder
    }

    private fun loadAvailableFolders() {
        // For now, return a dummy list of folders
        // Later this will read the actual vault structure
        _availableFolders.value = listOf(
            "Work/Inbox",
            "Personal/Journal",
            "Personal/Ideas",
            "Work/Meeting Notes",
            "Research/Papers"
        )
    }

    fun fileNote() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val currentTitle = _title.value
                val currentContent = _content.value
                val currentFolder = _selectedFolder.value
                val currentOcrResult = ocrResult

                if (currentTitle.isNullOrBlank()) {
                    _errorMessage.value = "Note title cannot be empty"
                    _isLoading.value = false
                    return@launch
                }

                if (currentContent.isNullOrBlank()) {
                    _errorMessage.value = "Note content cannot be empty"
                    _isLoading.value = false
                    return@launch
                }

                if (currentFolder.isNullOrBlank()) {
                    _errorMessage.value = "Please select a folder"
                    _isLoading.value = false
                    return@launch
                }

                if (currentOcrResult == null) {
                    _errorMessage.value = "Missing OCR data"
                    _isLoading.value = false
                    return@launch
                }

                // Generate note filename
                val sanitizedTitle = sanitizeFilename(currentTitle)
                val noteFilename = "$sanitizedTitle.md"

                // File the note using VaultManager
                val success = vaultManager.fileNote(
                    folderPath = currentFolder,
                    noteFilename = noteFilename,
                    content = currentContent
                )

                if (success) {
                    // Get vault name for Obsidian URI
                    val vaultName = extractVaultName()
                    val filePath = if (currentFolder.isNotBlank()) {
                        "$currentFolder/$noteFilename"
                    } else {
                        noteFilename
                    }
                    
                    _filingSuccess.value = FilingSuccess(vaultName, filePath)
                } else {
                    _errorMessage.value = "Failed to file note. Please check vault permissions."
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error filing note: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun extractVaultName(): String {
        return try {
            val vaultUri = preferencesRepository.vaultUri.first()
            if (vaultUri != null) {
                // Extract vault name from the URI path - typically the last segment
                val pathSegments = vaultUri.path?.split("/")?.filter { it.isNotBlank() }
                pathSegments?.lastOrNull() ?: "Unknown"
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun sanitizeFilename(filename: String): String {
        // Remove or replace characters that aren't valid in filenames
        return filename
            .replace(Regex("[<>:\"/\\\\|?*]"), "_")
            .trim('_')
            .take(50) // Limit length
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun onFilingSuccessHandled() {
        _filingSuccess.value = null
    }
} 