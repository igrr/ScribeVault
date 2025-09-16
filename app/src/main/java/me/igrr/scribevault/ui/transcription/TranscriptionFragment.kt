package me.igrr.scribevault.ui.transcription

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import me.igrr.scribevault.databinding.FragmentTranscriptionBinding
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class TranscriptionFragment : Fragment() {

    private var _binding: FragmentTranscriptionBinding? = null
    private val binding get() = _binding!!
    private val args: TranscriptionFragmentArgs by navArgs()
    private val viewModel: TranscriptionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTranscriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uris: List<Uri> = args.imageUris.map { Uri.parse(it) }
        viewModel.startTranscription(requireActivity().application, uris)

        viewModel.progressText.observe(viewLifecycleOwner) { text ->
            binding.textProgress.text = text
        }

        viewModel.progressFraction.observe(viewLifecycleOwner) { fraction ->
            binding.progressBar.progress = (fraction * 100).toInt()
        }

        viewModel.completedResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                val action = TranscriptionFragmentDirections.actionTranscriptionFragmentToFilingFragment(it)
                findNavController().navigate(action)
                viewModel.onNavigated()
            }
        }

        // Apply system bar insets as padding to keep progress UI clear of status/gesture bars
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = sys.top, bottom = sys.bottom)
            insets
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


