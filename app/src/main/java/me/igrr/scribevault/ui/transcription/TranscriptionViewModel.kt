package me.igrr.scribevault.ui.transcription

import android.app.Application
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.igrr.scribevault.data.ocr.OpenAIOcrProcessor
import me.igrr.scribevault.data.preferences.UserPreferencesRepository
import me.igrr.scribevault.domain.ocr.OcrProcessor
import me.igrr.scribevault.domain.ocr.OcrResult

class TranscriptionViewModel : ViewModel() {

    private val _progressText = MutableLiveData("Preparing…")
    val progressText: LiveData<String> = _progressText

    private val _progressFraction = MutableLiveData(0f)
    val progressFraction: LiveData<Float> = _progressFraction

    private val _completedResult = MutableLiveData<OcrResult?>()
    val completedResult: LiveData<OcrResult?> = _completedResult

    private var job: Job? = null

    fun startTranscription(app: Application, imageUris: List<Uri>) {
        if (job != null) return
        val openAi = OpenAIOcrProcessor(app, UserPreferencesRepository(app))
        job = viewModelScope.launch {
            val session = openAi.startSession()
            val total = imageUris.size.coerceAtLeast(1)
            val builder = StringBuilder()
            var pageIndex = 0
            for (uri in imageUris) {
                pageIndex += 1
                _progressText.postValue("Submitting page $pageIndex of $total…")
                val pageText = session?.transcribePage(uri, pageIndex)
                if (!pageText.isNullOrBlank()) {
                    if (builder.isNotEmpty()) builder.append("\n\n")
                    builder.append(pageText)
                }
                _progressFraction.postValue(pageIndex.toFloat() / total.toFloat())
            }
            // Generate single title and tags for the combined content
            val combinedText = builder.toString()
            val (title, tags) = session?.generateTitleAndTags() ?: ("" to emptyList())
            val combined = OcrResult(
                content = combinedText,
                tags = tags,
                title = title.ifEmpty { "Notes" }
            )
            _completedResult.postValue(combined)
        }
    }

    fun onNavigated() {
        _completedResult.value = null
        job = null
    }
}


