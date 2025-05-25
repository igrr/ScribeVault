package me.igrr.scribevault.ui.onboarding

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import me.igrr.scribevault.R
import me.igrr.scribevault.databinding.FragmentOnboardingBinding

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: OnboardingViewModel

    private val openDocumentTree = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Take persistent URI permission
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                viewModel.setSelectedVaultUri(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(OnboardingViewModel::class.java)
        viewModel.initialize(requireContext())

        setupVaultSelection()
        setupGetStartedButton()
        observeViewModel()
    }

    private fun setupVaultSelection() {
        binding.buttonSelectVault.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            }
            openDocumentTree.launch(intent)
        }
    }

    private fun setupGetStartedButton() {
        binding.buttonGetStarted.setOnClickListener {
            // Validate and save API key if provided
            val apiKey = binding.editTextApiKey.text.toString()
            if (apiKey.isNotEmpty()) {
                viewModel.validateAndSaveApiKey(apiKey)
            }

            // Complete onboarding
            viewModel.completeOnboarding()
        }
    }

    private fun observeViewModel() {
        viewModel.vaultValidationError.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.apiKeyValidationError.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.selectedVaultUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                Toast.makeText(requireContext(), "Vault selected successfully!", Toast.LENGTH_SHORT).show()
                binding.buttonSelectVault.text = "Vault Selected ✓"
            }
        }

        viewModel.onboardingComplete.observe(viewLifecycleOwner) { isComplete ->
            if (isComplete) {
                Toast.makeText(requireContext(), "Onboarding completed!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_onboardingFragment_to_captureFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 