package me.igrr.scribevault.ui.capture

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CaptureViewModel : ViewModel() {

    private val _capturedUris = MutableLiveData<List<Uri>>(emptyList())
    val capturedUris: LiveData<List<Uri>> = _capturedUris

    fun addCapturedUri(uri: Uri) {
        val updated = _capturedUris.value.orEmpty().toMutableList()
        updated.add(uri)
        _capturedUris.value = updated
    }

    fun removeAt(position: Int) {
        val updated = _capturedUris.value.orEmpty().toMutableList()
        if (position in updated.indices) {
            updated.removeAt(position)
            _capturedUris.value = updated
        }
    }

    fun clearAll() {
        _capturedUris.value = emptyList()
    }
}