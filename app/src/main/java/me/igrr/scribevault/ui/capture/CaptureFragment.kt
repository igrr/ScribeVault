package me.igrr.scribevault.ui.capture

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.navigation.fragment.findNavController
import me.igrr.scribevault.databinding.FragmentCaptureBinding
import java.io.File

class CaptureFragment : Fragment() {

    private var _binding: FragmentCaptureBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CaptureViewModel
    private lateinit var adapter: CapturedPagesAdapter
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { saved: Boolean ->
            if (saved) {
                Log.d("CaptureFragment", "Captured image URI: $photoUri")
                // Post-process in background, then add to list
                viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                    val processed = postProcessCapturedImage(photoUri)
                    viewModel.addCapturedUri(processed)
                }
            } else {
                Log.d("CaptureFragment", "Image capture cancelled by user.")
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCaptureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(CaptureViewModel::class.java)

        adapter = CapturedPagesAdapter { position ->
            viewModel.removeAt(position)
        }
        binding.recyclerThumbnails.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerThumbnails.adapter = adapter
        binding.recyclerThumbnails.clipToPadding = false
        // Add spacing between grid items
        val spacingPx = (16 * resources.displayMetrics.density).toInt()
        if (binding.recyclerThumbnails.itemDecorationCount == 0) {
            binding.recyclerThumbnails.addItemDecoration(GridSpacingItemDecoration(2, spacingPx))
        }

        // Apply window insets as padding so content does not go under status bar / rounded corners
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = sys.top, bottom = sys.bottom)
            // Also give the RecyclerView some side padding for curved edges
            binding.recyclerThumbnails.updatePadding(left = spacingPx, right = spacingPx)
            insets
        }

        viewModel.capturedUris.observe(viewLifecycleOwner) { uris ->
            adapter.submitList(uris)
            binding.buttonTranscribeAll.isEnabled = uris.isNotEmpty()
        }

        binding.buttonCaptureImage.setOnClickListener {
            photoFile = File(
                requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "note_${System.currentTimeMillis()}.jpg"
            )
            photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            takePicture.launch(photoUri)
        }

        binding.buttonTranscribeAll.setOnClickListener {
            val uris = viewModel.capturedUris.value.orEmpty().map { it.toString() }.toTypedArray()
            val action = CaptureFragmentDirections.actionCaptureFragmentToTranscriptionFragment(uris)
            findNavController().navigate(action)
        }
    }

    private suspend fun postProcessCapturedImage(originalUri: Uri): Uri {
        return withContext(Dispatchers.IO) {
            val destFile = File(
                requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "note_${System.currentTimeMillis()}_proc.jpg"
            )
            val ok = me.igrr.scribevault.utils.ImageUtils.downscaleToMaxLongSide(
                requireContext(),
                originalUri,
                destFile,
                maxLongSidePx = 2048,
                quality = 80
            )
            if (ok) {
                // Remove original large file
                try { photoFile.delete() } catch (_: Exception) {}
                FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    destFile
                )
            } else {
                originalUri
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 