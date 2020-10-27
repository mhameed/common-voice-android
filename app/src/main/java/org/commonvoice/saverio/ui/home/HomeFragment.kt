package org.commonvoice.saverio.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.annotation.AnimRes
import androidx.core.view.isGone
import androidx.core.view.isVisible
import org.commonvoice.saverio.BuildConfig
import org.commonvoice.saverio.MainActivity
import org.commonvoice.saverio.R
import org.commonvoice.saverio.databinding.FragmentHomeBinding
import org.commonvoice.saverio.ui.viewBinding.ViewBoundFragment
import org.commonvoice.saverio_lib.preferences.MainPrefManager
import org.commonvoice.saverio_lib.viewmodels.HomeViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class HomeFragment : ViewBoundFragment<FragmentHomeBinding>() {

    override fun inflate(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(layoutInflater, container, false)
    }

    private val homeViewModel: HomeViewModel by viewModel()

    private val mainPrefManager: MainPrefManager by inject()

    override fun onStart() {
        super.onStart()

        //TODO fix this mess once MainActivity is fixed

        (activity as? MainActivity)?.let {
            it.dashboard_selected = false

            it.checkUserLoggedIn()

            if (it.logged) {
                val textLoggedIn = binding.textLoggedUsername
                textLoggedIn.isGone = false
                textLoggedIn.isVisible = true
                textLoggedIn.text = it.getHiUsernameLoggedIn()

                binding.buttonHomeLogin.setText(R.string.button_home_profile)

                binding.buttonHomeLogin.setOnClickListener {
                    (activity as? MainActivity)?.let { main ->
                        main.openProfileSection()
                        main.checkUserLoggedIn()
                    }
                }
            } else {
                binding.buttonHomeLogin.setOnClickListener {
                    (activity as? MainActivity)?.let { main ->
                        main.openProfileSection()
                        main.checkUserLoggedIn()
                    }
                }
            }

            it.checkConnection()

            it.checkNewVersionAvailable()

            it.reviewOnPlayStore()
        }

        binding.buttonSpeak.setOnClickListener {
            (activity as? MainActivity)?.openSpeakSection()
        }

        binding.buttonListen.setOnClickListener {
            (activity as? MainActivity)?.openListenSection()
        }


        setTheme(requireContext())

        startAnimation(binding.buttonSpeak, R.anim.zoom_out)
        startAnimation(binding.buttonListen, R.anim.zoom_out)
    }

    override fun onResume() {
        super.onResume()

        homeViewModel.postStats(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, MainActivity.SOURCE_STORE)
    }

    fun setTheme(view: Context) = withBinding {
        theme.setElement(view, 3, homeSectionCVAndroid)
        theme.setElement(view, 3, homeSectionLoginSignup)
        theme.setElement(
            view,
            textCommonVoiceAndroid,
            background = false
        )
        theme.setElement(
            view,
            textLoggedUsername,
            background = false
        )
        theme.setElement(view, buttonHomeLogin)
        theme.setElement(layoutHome)
    }

}