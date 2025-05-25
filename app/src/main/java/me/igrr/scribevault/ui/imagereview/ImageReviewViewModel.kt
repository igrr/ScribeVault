package me.igrr.scribevault.ui.imagereview

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.igrr.scribevault.data.ocr.OpenAIOcrProcessor
import me.igrr.scribevault.domain.ocr.OcrProcessor
import me.igrr.scribevault.data.preferences.UserPreferencesRepository
import me.igrr.scribevault.domain.ocr.OcrResult

// Updated ViewModel to extend AndroidViewModel for accessing application context.
class ImageReviewViewModel(application: Application) : AndroidViewModel(application) {

    // Instantiate OpenAIOcrProcessor with the application context and a new UserPreferencesRepository
    private val ocrProcessor: OcrProcessor = OpenAIOcrProcessor(getApplication(), UserPreferencesRepository(getApplication()))

    private val _imageUri = MutableLiveData<Uri?>()
    val imageUri: LiveData<Uri?> = _imageUri

    private val _ocrResult = MutableLiveData<OcrResult?>()
    val ocrResult: LiveData<OcrResult?> = _ocrResult

    private val _navigateToFiling = MutableLiveData<OcrResult?>()
    val navigateToFiling: LiveData<OcrResult?> = _navigateToFiling

    fun setImageUri(uri: Uri) {
        _imageUri.value = uri
    }

    fun processImageWithOcr() {
        _imageUri.value?.let {
            viewModelScope.launch {
                val result = ocrProcessor.processImage(it)
                _ocrResult.value = result
                if (result != null) {
                    _navigateToFiling.value = result
                }
            }
        }
    }

    fun onNavigationToFilingDone() {
        _navigateToFiling.value = null
    }

    // TODO: Add logic for initiating OCR and preparing data for FilingFragment
} 