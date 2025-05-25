package me.igrr.scribevault.ui.imagereview

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import me.igrr.scribevault.databinding.FragmentImageReviewBinding

class ImageReviewFragment : Fragment() {

    private var _binding: FragmentImageReviewBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ImageReviewViewModel
    private val args: ImageReviewFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImageReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(ImageReviewViewModel::class.java)

        val imageUri = Uri.parse(args.imageUriString)
        viewModel.setImageUri(imageUri)

        viewModel.imageUri.observe(viewLifecycleOwner) {
            it?.let {
                binding.imageViewReview.setImageURI(it)
            }
        }

        binding.buttonProcessOcr.setOnClickListener {
            // Initiate OCR Processing
            setLoadingState(true) // Show loading indicator
            viewModel.processImageWithOcr()
        }

        viewModel.navigateToFiling.observe(viewLifecycleOwner) { ocrResult ->
            ocrResult?.let {
                setLoadingState(false) // Hide loading indicator
                val action = ImageReviewFragmentDirections.actionImageReviewFragmentToFilingFragment(
                    it
                )
                findNavController().navigate(action)
                viewModel.onNavigationToFilingDone() // Reset navigation trigger
            }
        }

        viewModel.ocrResult.observe(viewLifecycleOwner) { result ->
             if (result == null && viewModel.navigateToFiling.value == null) {
                // OCR might have failed, and we are not navigating yet
                setLoadingState(false) // Hide loading indicator
                Toast.makeText(context, "OCR processing failed or returned no result.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        // TODO: Implement a more sophisticated loading indicator (e.g., ProgressBar)
        binding.buttonProcessOcr.isEnabled = !isLoading
        if (isLoading) {
            binding.buttonProcessOcr.text = "Processing..."
        } else {
            binding.buttonProcessOcr.text = "Process with OCR"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 