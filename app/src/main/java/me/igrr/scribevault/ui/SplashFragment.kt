package me.igrr.scribevault.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.igrr.scribevault.R
import me.igrr.scribevault.data.preferences.UserPreferencesRepository

class SplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userPreferencesRepository = UserPreferencesRepository(requireContext().applicationContext)
        lifecycleScope.launch {
            val isOnboardingCompleted = userPreferencesRepository.isOnboardingCompleted.first()
            val navController = findNavController()
            // Ensure that the NavController is ready by trying to access currentDestination
            // This can help prevent issues if navigation is attempted too early.
            if (navController.currentDestination?.id == R.id.splashFragment) {
                if (isOnboardingCompleted) {
                    navController.navigate(R.id.action_splashFragment_to_captureFragment)
                } else {
                    navController.navigate(R.id.action_splashFragment_to_onboardingFragment)
                }
            }
        }
    }
} 