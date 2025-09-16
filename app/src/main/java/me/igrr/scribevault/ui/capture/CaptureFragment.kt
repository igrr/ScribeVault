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
import androidx.navigation.fragment.findNavController
import me.igrr.scribevault.databinding.FragmentCaptureBinding
import java.io.File

class CaptureFragment : Fragment() {

    private var _binding: FragmentCaptureBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CaptureViewModel
    private lateinit var photoUri: Uri

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { saved: Boolean ->
            if (saved) {
                val action = CaptureFragmentDirections.actionCaptureFragmentToImageReviewFragment(photoUri.toString())
                Log.d("CaptureFragment", "Captured image URI: $photoUri")
                findNavController().navigate(action)
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

        binding.buttonCaptureImage.setOnClickListener {
            val photoFile = File(
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 