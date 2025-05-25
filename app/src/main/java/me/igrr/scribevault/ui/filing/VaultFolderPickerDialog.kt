package me.igrr.scribevault.ui.filing

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class VaultFolderPickerDialog(
    private val availableFolders: List<String>,
    private val currentSelection: String,
    private val onFolderSelected: (String) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val selectedIndex = availableFolders.indexOf(currentSelection).takeIf { it >= 0 } ?: 0
        
        return AlertDialog.Builder(requireContext())
            .setTitle("Select Folder")
            .setSingleChoiceItems(
                availableFolders.toTypedArray(),
                selectedIndex
            ) { dialog, which ->
                val selectedFolder = availableFolders[which]
                onFolderSelected(selectedFolder)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

    companion object {
        const val TAG = "VaultFolderPickerDialog"
    }
} 