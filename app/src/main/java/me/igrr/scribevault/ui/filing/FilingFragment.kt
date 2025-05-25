package me.igrr.scribevault.ui.filing

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import me.igrr.scribevault.R
import me.igrr.scribevault.databinding.FragmentFilingBinding

class FilingFragment : Fragment() {

    private var _binding: FragmentFilingBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: FilingViewModel
    private val args: FilingFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[FilingViewModel::class.java]

        setupUI()
        observeViewModel()
        initializeData()
    }

    private fun setupUI() {
        // Set up text watchers for title and content
        binding.editTextTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateTitle(s.toString())
            }
        })

        binding.editTextContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateContent(s.toString())
            }
        })

        // Set up folder selection button
        binding.buttonSelectFolder.setOnClickListener {
            showFolderPicker()
        }

        // Set up file note button
        binding.buttonFileNote.setOnClickListener {
            viewModel.fileNote()
        }
    }

    private fun observeViewModel() {
        viewModel.title.observe(viewLifecycleOwner) { title ->
            if (binding.editTextTitle.text.toString() != title) {
                binding.editTextTitle.setText(title)
            }
        }

        viewModel.content.observe(viewLifecycleOwner) { content ->
            if (binding.editTextContent.text.toString() != content) {
                binding.editTextContent.setText(content)
            }
        }

        viewModel.selectedFolder.observe(viewLifecycleOwner) { folder ->
            binding.buttonSelectFolder.text = "Select Folder: $folder"
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.buttonFileNote.isEnabled = !isLoading
            binding.buttonFileNote.text = if (isLoading) "Filing..." else "File Note"
        }

        viewModel.filingSuccess.observe(viewLifecycleOwner) { filingSuccess ->
            filingSuccess?.let {
                Toast.makeText(requireContext(), "Note filed successfully!", Toast.LENGTH_SHORT).show()
                
                // Launch Obsidian with the filed note
                launchObsidianIntent(it.vaultName, it.filePath)
                
                // Navigate back to CaptureFragment
                findNavController().popBackStack(R.id.captureFragment, false)
                
                viewModel.onFilingSuccessHandled()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun launchObsidianIntent(vaultName: String, filePath: String) {
        try {
            // URI encode the file path for proper handling of special characters
            // Uri.encode() is better for URI paths than URLEncoder which is for form data
            val encodedFilePath = Uri.encode(filePath, "/")
            
            // Construct the Obsidian URI
            val obsidianUri = "obsidian://open?vault=$vaultName&file=$encodedFilePath"
            
            // Create intent to open Obsidian
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(obsidianUri))
            
            // Try to launch the intent directly - let the system handle resolution
            startActivity(intent)
            
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(requireContext(), "Obsidian app not found. Please install Obsidian.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to open in Obsidian: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeData() {
        val ocrResult = args.ocrResult

        if (ocrResult == null) {
            Toast.makeText(requireContext(), "OCR processing failed", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        viewModel.initializeData(ocrResult)
    }

    private fun showFolderPicker() {
        val availableFolders = viewModel.availableFolders.value ?: return
        val currentSelection = viewModel.selectedFolder.value ?: "Work/Inbox"

        val dialog = VaultFolderPickerDialog(
            availableFolders = availableFolders,
            currentSelection = currentSelection,
            onFolderSelected = { selectedFolder ->
                viewModel.updateSelectedFolder(selectedFolder)
            }
        )

        dialog.show(parentFragmentManager, VaultFolderPickerDialog.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 