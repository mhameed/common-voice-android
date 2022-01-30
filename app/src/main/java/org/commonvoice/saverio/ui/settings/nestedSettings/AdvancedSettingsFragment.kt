package org.commonvoice.saverio.ui.settings.nestedSettings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.CookieManager
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.core.view.isGone
import androidx.core.widget.addTextChangedListener
import org.commonvoice.saverio.FirstLaunch
import org.commonvoice.saverio.MainActivity
import org.commonvoice.saverio.R
import org.commonvoice.saverio.databinding.FragmentAdvancedSettingsBinding
import org.commonvoice.saverio.ui.dialogs.DialogInflater
import org.commonvoice.saverio.ui.dialogs.commonTypes.StandardDialog
import org.commonvoice.saverio.ui.dialogs.specificDialogs.IdentifyMeDialog
import org.commonvoice.saverio.ui.viewBinding.ViewBoundFragment
import org.commonvoice.saverio.utils.setupOnSwipeRight
import org.commonvoice.saverio_lib.preferences.*
import org.commonvoice.saverio_lib.repositories.StatsRepository
import org.commonvoice.saverio_lib.viewmodels.LoginViewModel
import org.commonvoice.saverio_lib.viewmodels.MainActivityViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class AdvancedSettingsFragment : ViewBoundFragment<FragmentAdvancedSettingsBinding>() {

    override fun inflate(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAdvancedSettingsBinding {
        return FragmentAdvancedSettingsBinding.inflate(layoutInflater, container, false)
    }

    private val mainPrefManager by inject<MainPrefManager>()
    private val settingsPrefManager by inject<SettingsPrefManager>()
    private val statsPrefManager by inject<StatsPrefManager>()
    private val listenPrefManager by inject<ListenPrefManager>()
    private val speakPrefManager by inject<SpeakPrefManager>()
    private val firstRunPrefManager by inject<FirstRunPrefManager>()
    private val logPrefManager by inject<LogPrefManager>()
    private val loginViewModel by viewModel<LoginViewModel>()
    private val mainViewModel by viewModel<MainActivityViewModel>()
    private val dialogInflater by inject<DialogInflater>()
    private val statsRepository by inject<StatsRepository>()

    private val defaultAPIServer = "https://commonvoice.mozilla.org/api/v1/"

    override fun onStart() {
        super.onStart()

        withBinding {
            buttonBackSettingsSubSectionAdvanced.setOnClickListener {
                activity?.onBackPressed()
            }

            if (mainPrefManager.areGesturesEnabled)
                nestedScrollSettingsAdvanced.setupOnSwipeRight(requireContext()) { activity?.onBackPressed() }

            switchGenericStatistics.setOnCheckedChangeListener { _, isChecked ->
                mainPrefManager.areGenericStats = isChecked
            }
            switchGenericStatistics.isChecked = mainPrefManager.areGenericStats

            switchAppUsageStatistics.setOnCheckedChangeListener { _, isChecked ->
                mainPrefManager.areAppUsageStatsEnabled = isChecked
            }
            switchAppUsageStatistics.isChecked = mainPrefManager.areAppUsageStatsEnabled

            switchSaveLogToFile.setOnCheckedChangeListener { _, isChecked ->
                logPrefManager.saveLogFile = isChecked
            }
            switchSaveLogToFile.isChecked = logPrefManager.saveLogFile

            switchHomeAds.text = getString(R.string.enable_ads_google_play_in_section).replace(
                "{{section_name}}",
                getString(R.string.title_home)
            )
            switchHomeAds.setOnCheckedChangeListener { _, isChecked ->
                mainPrefManager.showAdBanner = isChecked
            }
            switchHomeAds.isChecked = mainPrefManager.showAdBanner

            switchListenAds.text = getString(R.string.enable_ads_google_play_in_section).replace(
                "{{section_name}}",
                getString(R.string.settingsListen)
            )
            switchListenAds.setOnCheckedChangeListener { _, isChecked ->
                listenPrefManager.showAdBanner = isChecked
            }
            switchListenAds.isChecked = listenPrefManager.showAdBanner

            switchSpeakAds.text = getString(R.string.enable_ads_google_play_in_section).replace(
                "{{section_name}}",
                getString(R.string.settingsSpeak)
            )
            switchSpeakAds.setOnCheckedChangeListener { _, isChecked ->
                speakPrefManager.showAdBanner = isChecked
            }
            switchSpeakAds.isChecked = speakPrefManager.showAdBanner

            if (mainPrefManager.appSourceStore == "GPS") {
                settingsSectionAdvancedAds.isGone = false
            } else {
                settingsSectionAdvancedAds.isGone = true
            }

            buttonOpenTutorialAgain.setOnClickListener {
                Intent(requireContext(), FirstLaunch::class.java).also {
                    startActivity(it)
                    activity?.finish()
                }
            }

            buttonResetDefaultAPIServer.paintFlags = Paint.UNDERLINE_TEXT_FLAG
            buttonResetDefaultAPIServer.setOnClickListener {
                mainPrefManager.genericAPIUrl = defaultAPIServer
                textDestinationAPIServer.setText(mainPrefManager.genericAPIUrl)
                mainViewModel.clearDB()

                buttonCustomiseAPIServer.isGone = false
                advancedSubSectionDestinarioAPIServer.isGone = true
                buttonResetDefaultAPIServer.isGone = true
            }
            textDestinationAPIServer.addTextChangedListener {
                var valueTemp = textDestinationAPIServer.text.toString()
                if (valueTemp != "") {
                    if (valueTemp.get(valueTemp.length - 1).toString() != "/") {
                        valueTemp = valueTemp + "/"
                    }
                    mainPrefManager.genericAPIUrl = valueTemp
                    mainViewModel.clearDB()
                } else {
                    mainPrefManager.genericAPIUrl = defaultAPIServer
                }
            }
            textDestinationAPIServer.setText(mainPrefManager.genericAPIUrl)

            if (mainPrefManager.genericAPIUrl != defaultAPIServer) {
                buttonCustomiseAPIServer.isGone = true
                advancedSubSectionDestinarioAPIServer.isGone = false
                buttonResetDefaultAPIServer.isGone = false
            }

            buttonResetData.setOnClickListener {
                dialogInflater.show(requireContext(),
                    StandardDialog(
                        messageRes = R.string.text_are_you_sure_reset_app_data,
                        buttonTextRes = R.string.button_yes_sure,
                        onButtonClick = {
                            //Reset FirstRun
                            firstRunPrefManager.main = true
                            firstRunPrefManager.listen = true
                            firstRunPrefManager.speak = true

                            //Reset Settings
                            settingsPrefManager.isOfflineMode = true
                            settingsPrefManager.showReportIcon = false
                            settingsPrefManager.automaticallyCheckForUpdates = false
                            settingsPrefManager.latestVersion = ""
                            settingsPrefManager.isProgressBarColouredEnabled = true
                            settingsPrefManager.isLightThemeSentenceBoxSpeakListen = false
                            settingsPrefManager.showInfoIcon = false
                            settingsPrefManager.showContributionCriteriaIcon = true
                            settingsPrefManager.dailyGoalNotificationsHour = 17
                            settingsPrefManager.dailyGoalNotificationsHourSecond = -1
                            settingsPrefManager.dailyGoalNotificationsLastSentDate = ""
                            settingsPrefManager.dailyGoalNotificationsLastSentDateSecond = ""
                            settingsPrefManager.dailyGoalNotifications = true
                            settingsPrefManager.notifications = true
                            settingsPrefManager.wifiOnlyUpload = false
                            settingsPrefManager.wifiOnlyDownload = false

                            //Reset Stats
                            statsPrefManager.dailyGoalObjective = 0
                            statsPrefManager.reviewOnPlayStoreCounter = 0
                            statsPrefManager.buyMeACoffeeCounter = 0
                            statsPrefManager.checkAdsDisabledGPS = 0
                            statsPrefManager.todayValidated = 0
                            statsPrefManager.todayRecorded = 0
                            statsPrefManager.allTimeValidated = 0
                            statsPrefManager.allTimeRecorded = 0
                            statsPrefManager.allTimeLevel = 0
                            statsPrefManager.localValidated = 0
                            statsPrefManager.localRecorded = 0
                            statsPrefManager.localLevel = 0
                            statsPrefManager.daysInARow = 0
                            statsPrefManager.lastDateOpenedTheApp = null

                            //Reset Listen
                            listenPrefManager.requiredClipsCount = 50
                            listenPrefManager.isAutoPlayClipEnabled = true
                            listenPrefManager.isShowTheSentenceAtTheEnd = false
                            listenPrefManager.showAdBanner = true
                            listenPrefManager.showSpeedControl = false
                            listenPrefManager.audioSpeed = 1F
                            listenPrefManager.gesturesSwipeTop = "report"
                            listenPrefManager.gesturesSwipeBottom = ""
                            listenPrefManager.gesturesSwipeLeft = "skip"
                            listenPrefManager.gesturesSwipeRight = "back"
                            listenPrefManager.gesturesLongPress = ""
                            listenPrefManager.gesturesDoubleTap = ""

                            //Reset Speak
                            speakPrefManager.requiredSentencesCount = 50
                            speakPrefManager.playRecordingSoundIndicator = false
                            speakPrefManager.skipRecordingConfirmation = false
                            speakPrefManager.saveRecordingsOnDevice = false
                            speakPrefManager.showAdBanner = true
                            speakPrefManager.showSpeedControl = false
                            speakPrefManager.audioSpeed = 1F
                            speakPrefManager.gesturesSwipeTop = "report"
                            speakPrefManager.gesturesSwipeBottom = ""
                            speakPrefManager.gesturesSwipeLeft = "skip"
                            speakPrefManager.gesturesSwipeRight = "back"
                            speakPrefManager.gesturesLongPress = ""
                            speakPrefManager.gesturesDoubleTap = ""
                            speakPrefManager.pushToTalk = false

                            //Reset Main
                            mainPrefManager.language = "en"
                            mainPrefManager.genericAPIUrl = defaultAPIServer
                            mainPrefManager.tokenUserId = ""
                            mainPrefManager.tokenAuth = ""
                            mainPrefManager.showOfflineModeMessage = true
                            mainPrefManager.showReportWebsiteBugs = true
                            mainPrefManager.areGesturesEnabled = true
                            mainPrefManager.statsUserId = ""
                            mainPrefManager.areGenericStats = true
                            mainPrefManager.areAppUsageStatsEnabled = true
                            mainPrefManager.areAnimationsEnabled = true
                            mainPrefManager.areLabelsBelowMenuIcons = false
                            mainPrefManager.hasLanguageChanged = true
                            mainPrefManager.hasLanguageChanged2 = true
                            mainPrefManager.themeType = "light"
                            mainPrefManager.sessIdCookie = null
                            mainPrefManager.isLoggedIn = false
                            mainPrefManager.username = ""
                            mainPrefManager.textSize = 1.0F
                            mainPrefManager.showAdBanner = true

                            //Reset Log
                            logPrefManager.saveLogFile = false

                            CookieManager.getInstance().flush()
                            CookieManager.getInstance().removeAllCookies(null)
                            loginViewModel.clearDB()
                            mainViewModel.clearDB()

                            startActivity(Intent(requireContext(), MainActivity::class.java))
                        },
                        button2TextRes = R.string.button_cancel,
                        onButton2Click = {}
                    ))
            }
        }

        setupButtons()

        setTheme()
    }

    private fun setupButtons() {
        binding.buttonShowStringIdentifyMe.setOnClickListener {
            dialogInflater.show(
                requireContext(),
                IdentifyMeDialog(statsRepository.getUserId(), onCopyClick = {
                    requireContext()
                        .getSystemService<ClipboardManager>()
                        ?.setPrimaryClip(ClipData.newPlainText("", statsRepository.getUserId()))
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.copied_string),
                        Toast.LENGTH_LONG
                    ).show()
                })
            )
        }

        binding.buttonCustomiseAPIServer.setOnClickListener {
            dialogInflater.show(
                requireContext(),
                StandardDialog(
                    messageRes = R.string.message_customisation_api_server_text,
                    buttonTextRes = R.string.button_yes_sure,
                    onButtonClick = {
                        withBinding {
                            buttonCustomiseAPIServer.isGone = true
                            advancedSubSectionDestinarioAPIServer.isGone = false
                            buttonResetDefaultAPIServer.isGone = false
                            mainViewModel.clearDB()
                        }
                    },
                    button2TextRes = R.string.button_cancel,
                    onButton2Click = {}
                )
            )
        }
    }

    private fun setTheme() {
        withBinding {
            theme.setElement(layoutSettingsAdvanced)

            theme.setElements(requireContext(), settingsSectionAdvanced)
            theme.setElements(requireContext(), settingsSectionAdvancedStatistics)
            theme.setElements(requireContext(), settingsSectionAdvancedAds)
            theme.setElements(requireContext(), settingsSectionAdvancedDestinationAPIServer)
            theme.setElements(requireContext(), settingsSectionAdvancedResetAppData)
            theme.setElements(requireContext(), advancedSubSectionDestinarioAPIServer)

            theme.setElement(requireContext(), 3, settingsSectionAdvanced)
            theme.setElement(requireContext(), 3, settingsSectionAdvancedStatistics)
            theme.setElement(requireContext(), 3, settingsSectionAdvancedAds)
            theme.setElement(requireContext(), 3, settingsSectionAdvancedDestinationAPIServer)
            theme.setElement(requireContext(), 3, settingsSectionAdvancedResetAppData)

            theme.setElement(requireContext(), buttonCustomiseAPIServer)

            theme.setElement(
                requireContext(),
                3,
                advancedSubSectionDestinarioAPIServer,
                R.color.colorTabBackgroundInactive,
                R.color.colorTabBackgroundInactiveDT
            )

            theme.setTextView(
                requireContext(),
                textDestinationAPIServer,
                border = false,
                intern = true
            )

            theme.setTitleBar(requireContext(), titleSettingsSubSectionAdvanced, textSize = 20F)
        }
    }

}