package me.igrr.scribevault.ui.onboarding

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.igrr.scribevault.data.preferences.UserPreferencesRepository

class OnboardingViewModel : ViewModel() {
    private val _selectedVaultUri = MutableLiveData<Uri?>()
    val selectedVaultUri: LiveData<Uri?> = _selectedVaultUri

    private val _vaultValidationError = MutableLiveData<String?>()
    val vaultValidationError: LiveData<String?> = _vaultValidationError

    private val _apiKeyValidationError = MutableLiveData<String?>()
    val apiKeyValidationError: LiveData<String?> = _apiKeyValidationError

    private val _onboardingComplete = MutableLiveData<Boolean>()
    val onboardingComplete: LiveData<Boolean> = _onboardingComplete

    private var context: Context? = null
    private var preferencesRepository: UserPreferencesRepository? = null

    fun initialize(context: Context) {
        this.context = context.applicationContext
        this.preferencesRepository = UserPreferencesRepository(context.applicationContext)
    }

    fun setSelectedVaultUri(uri: Uri?) {
        viewModelScope.launch {
            if (uri == null) {
                _selectedVaultUri.value = null
                _vaultValidationError.value = "No vault selected"
                return@launch
            }

            val context = context ?: run {
                _vaultValidationError.value = "Context not initialized"
                return@launch
            }

            // Validate that this is an Obsidian vault by checking for .obsidian folder
            try {
                val documentFile = DocumentFile.fromTreeUri(context, uri)
                val obsidianFolder = documentFile?.findFile(".obsidian")
                
                if (obsidianFolder?.exists() == true) {
                    _selectedVaultUri.value = uri
                    _vaultValidationError.value = null
                } else {
                    _selectedVaultUri.value = null
                    _vaultValidationError.value = "Selected directory is not an Obsidian vault"
                }
            } catch (e: Exception) {
                _selectedVaultUri.value = null
                _vaultValidationError.value = "Error validating vault: ${e.message}"
            }
        }
    }

    fun validateAndSaveApiKey(apiKey: String) {
        viewModelScope.launch {
            // Stub validation - just check length for now
            if (apiKey.length < 8) {
                _apiKeyValidationError.value = "API key must be at least 8 characters long"
                return@launch
            }

            _apiKeyValidationError.value = null
            preferencesRepository?.saveApiKey(apiKey)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val vaultUri = _selectedVaultUri.value
            if (vaultUri == null) {
                _vaultValidationError.value = "Please select a vault first"
                return@launch
            }

            // Save vault URI
            preferencesRepository?.saveVaultUri(vaultUri)

            // Mark onboarding as complete
            preferencesRepository?.setOnboardingCompleted()
            _onboardingComplete.value = true
        }
    }
} 