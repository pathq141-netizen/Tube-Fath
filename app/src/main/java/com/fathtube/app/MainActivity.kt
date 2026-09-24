package com.fathtube.app

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonParser
import dagger.hilt.android.AndroidEntryPoint
import com.fathtube.app.BuildConfig
import com.fathtube.app.data.local.AppUiModePreferences
import com.fathtube.app.data.local.LocalDataManager
import com.fathtube.app.data.recommendation.NanzNeuroEngine
import com.fathtube.app.discord.DiscordPresenceRuntime
import com.fathtube.app.network.AppProxyManager
import com.fathtube.app.platform.AppUiMode
import com.fathtube.app.platform.AppUiRoot
import com.fathtube.app.platform.DeviceFormFactorDetector
import com.fathtube.app.player.BackgroundPlaybackPolicy
import com.fathtube.app.player.GlobalPlayerState
import com.fathtube.app.player.LifecyclePlaybackPreferences
import com.fathtube.app.player.MemoryPressurePolicy
import com.fathtube.app.player.PictureInPictureHelper
import com.fathtube.app.ui.NanzApp
import com.fathtube.app.ui.components.ProvideVideoCardState
import com.fathtube.app.ui.components.UpdateDialog
import com.fathtube.app.ui.components.shared.ProvideChannelGroupLabels
import com.fathtube.app.ui.components.shared.ProvideDateDisplaySettings
import com.fathtube.app.ui.screens.CrashReporterScreen
import com.fathtube.app.ui.theme.CustomThemePalettes
import com.fathtube.app.ui.theme.NanzTheme
import com.fathtube.app.ui.theme.ThemeMode
import com.fathtube.app.ui.theme.ThemeVariant
import com.fathtube.app.ui.tv.NanzTvApp
import com.fathtube.app.ui.utils.ProvideWindowSizeClass
import com.fathtube.app.updater.ApkUpdateHelper
import com.fathtube.app.utils.AppLanguageManager
import com.fathtube.app.utils.NanzCrashHandler
import com.fathtube.app.utils.UpdateInfo
import com.fathtube.app.utils.UpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

private const val PORTRAIT_REEL_ASPECT_RATIO = 9f / 16f

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val _deeplinkVideoId = mutableStateOf<String?>(null)
    val deeplinkVideoId: State<String?> = _deeplinkVideoId

    private val _isDeeplinkShort = mutableStateOf(false)
    val isDeeplinkShort: State<Boolean> = _isDeeplinkShort

    private val _pendingUpdateInfo = mutableStateOf<UpdateInfo?>(null)
    val pendingUpdateInfo: State<UpdateInfo?> = _pendingUpdateInfo

    private val _openMusicPlayerRequest = mutableIntStateOf(0)
    val openMusicPlayerRequest: State<Int> = _openMusicPlayerRequest

    private val _pendingWidgetRoute = mutableStateOf<String?>(null)
    val pendingWidgetRoute: State<String?> = _pendingWidgetRoute

    @Inject
    lateinit var lifecyclePlaybackPreferences: LifecyclePlaybackPreferences

    private var pipDismissCheckJob: Job? = null
    private var pendingAutoPip = false
    private var cachedAppUiRoot = AppUiRoot.MOBILE

    private fun videoPlaybackStateName(state: Int?): String =
        when (state) {
            androidx.media3.common.Player.STATE_IDLE -> "IDLE"
            androidx.media3.common.Player.STATE_BUFFERING -> "BUFFERING"
            androidx.media3.common.Player.STATE_READY -> "READY"
            androidx.media3.common.Player.STATE_ENDED -> "ENDED"
            null -> "NO_PLAYER"
            else -> "UNKNOWN($state)"
        }

    private fun lifecyclePlaybackSnapshot(): String {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val playerManager =
            com.fathtube.app.player.EnhancedPlayerManager
                .getInstance()
        val playerState = playerManager.playerState.value
        val player = playerManager.getPlayer()
        return "interactive=${powerManager?.isInteractive} lifecycle=${lifecycle.currentState} " +
            "pip=$isInPictureInPictureMode pendingAutoPip=$pendingAutoPip " +
            "bgPref=${lifecyclePlaybackPreferences.settings.backgroundPlayEnabled} " +
            "shortsBgPref=${lifecyclePlaybackPreferences.settings.shortsBackgroundPlay} " +
            "explicitBg=${GlobalPlayerState.isExplicitBackgroundPlaybackActive.value} " +
            "video=${playerState.currentVideoId} exo=${videoPlaybackStateName(player?.playbackState)} " +
            "pwr=${player?.playWhenReady} playing=${player?.isPlaying} buffering=${playerState.isBuffering} " +
            "pos=${player?.currentPosition}/${player?.duration} idx=${player?.currentMediaItemIndex} count=${player?.mediaItemCount}"
    }

    private fun videoLifecycleLog(message: String) {
        Log.w("NanzVideoLifecycle", "$message | ${lifecyclePlaybackSnapshot()}")
    }

    override fun attachBaseContext(newBase: Context) {
        val selectedLanguage = AppLanguageManager.loadSelectedLanguageTag(newBase)
        super.attachBaseContext(AppLanguageManager.wrapContext(newBase, selectedLanguage))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // the OS-level splash screen (camouflaged to match Compose splash background)
        installSplashScreen()

        super.onCreate(savedInstanceState)
        DiscordPresenceRuntime.attachActivity(this)

        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
            navigationBarStyle =
                SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        // Player setup reads DataStore and opens the media cache index, so it runs off the main
        // thread and settles after the first frame instead of blocking onCreate.
        lifecycleScope.launch { GlobalPlayerState.initializeAsync(applicationContext) }

        // Snapshot lives in the process, not the activity: onUserLeaveHint/onStop read it
        // synchronously and must still see it after a recreation they run inside of (#817).
        lifecyclePlaybackPreferences.observeIn(lifecycleScope)

        // Initialize Neuro Engine (Recommendation System)
        lifecycleScope.launch(Dispatchers.IO) {
            NanzNeuroEngine.initialize(applicationContext)
        }

        val dataManager = LocalDataManager(applicationContext)

        lifecycleScope.launch {
            com.fathtube.app.widget.core.NanzWidgets
                .observeThemeChanges(applicationContext)
        }

        handleIntent(intent)

        setContent {
            val scope = rememberCoroutineScope()
            var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
            var themeVariant by remember { mutableStateOf(ThemeVariant.DARK) }
            var customThemePalettes by remember { mutableStateOf(CustomThemePalettes()) }
            var systemLightThemeMode by remember { mutableStateOf(ThemeMode.DARK) }
            var systemDarkThemeMode by remember { mutableStateOf(ThemeMode.DARK) }
            var systemDarkThemeVariant by remember { mutableStateOf(ThemeVariant.DARK) }
            // State to control splash visibility
            var showSplash by remember { mutableStateOf(true) }

            val context = LocalContext.current
            val configuration = LocalConfiguration.current
            val uiPreferences = remember { AppUiModePreferences(applicationContext) }
            val appUiMode by uiPreferences.mode.collectAsState(initial = AppUiMode.AUTOMATIC)
            val deviceFormFactor =
                remember(configuration.uiMode, context) {
                    DeviceFormFactorDetector.detect(context)
                }
            val appUiRoot = appUiMode.resolve(deviceFormFactor)
            SideEffect { cachedAppUiRoot = appUiRoot }

            // Check for a crash that happened last session.
            // If found, show the CrashReporterScreen instead of the normal UI.
            var pendingCrashLog by remember {
                mutableStateOf(NanzCrashHandler.getLastCrash(applicationContext))
            }

            if (pendingCrashLog != null) {
                NanzTheme(
                    themeMode = themeMode,
                    themeVariant = themeVariant,
                    customThemePalettes = customThemePalettes,
                    systemLightThemeMode = systemLightThemeMode,
                    systemDarkThemeMode = systemDarkThemeMode,
                    systemDarkThemeVariant = systemDarkThemeVariant,
                ) {
                    CrashReporterScreen(
                        crashLog = pendingCrashLog!!,
                        onClearAndRestart = {
                            NanzCrashHandler.clearLastCrash(applicationContext)
                            pendingCrashLog = null
                        },
                    )
                }
                return@setContent
            }

            var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

            // Load theme preference and keep it reactive
            LaunchedEffect(Unit) {
                dataManager.themeMode.collect { mode ->
                    themeMode = mode
                }
            }

            LaunchedEffect(Unit) {
                dataManager.themeVariant.collect { variant ->
                    themeVariant = variant
                }
            }

            LaunchedEffect(Unit) {
                dataManager.customThemePalettes.collect { palettes ->
                    customThemePalettes = palettes
                }
            }

            LaunchedEffect(Unit) {
                dataManager.systemLightThemeMode.collect { mode ->
                    systemLightThemeMode = mode
                }
            }

            LaunchedEffect(Unit) {
                dataManager.systemDarkThemeMode.collect { mode ->
                    systemDarkThemeMode = mode
                }
            }

            LaunchedEffect(Unit) {
                dataManager.systemDarkThemeVariant.collect { variant ->
                    systemDarkThemeVariant = variant
                }
            }

            // Initialize Flow Neuro Engine
            LaunchedEffect(Unit) {
                com.fathtube.app.data.recommendation.NanzNeuroEngine
                    .initialize(applicationContext)
            }

            NanzTheme(
                themeMode = themeMode,
                themeVariant = themeVariant,
                customThemePalettes = customThemePalettes,
                systemLightThemeMode = systemLightThemeMode,
                systemDarkThemeMode = systemDarkThemeMode,
                systemDarkThemeVariant = systemDarkThemeVariant,
            ) {
                // Show Dialog Overlay if update exists (github flavor only)
                if (BuildConfig.UPDATER_ENABLED && updateInfo != null) {
                    UpdateDialog(
                        updateInfo = updateInfo!!,
                        onDismiss = { updateInfo = null },
                        onUpdate = {
                            UpdateManager.triggerDownload(context, updateInfo!!.downloadUrl)
                            updateInfo = null
                        },
                    )
                }

                // Handle update from notification (github flavor only)
                if (BuildConfig.UPDATER_ENABLED) {
                    val pendingUpdate by this@MainActivity.pendingUpdateInfo
                    LaunchedEffect(pendingUpdate) {
                        if (pendingUpdate != null) {
                            updateInfo = pendingUpdate
                        }
                    }
                }

                // Request notification permission for Android 13+ (skip during benchmark/test runs)
                val isBypassMode = intent?.getBooleanExtra(EXTRA_BENCHMARK_BYPASS_ONBOARDING, false) == true
                if (!isBypassMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher =
                        androidx.activity.compose.rememberLauncherForActivityResult(
                            androidx.activity.result.contract.ActivityResultContracts
                                .RequestPermission(),
                        ) { isGranted ->
                            if (isGranted) {
                                android.util.Log.d("MainActivity", "Notification permission granted")
                            } else {
                                android.util.Log.w("MainActivity", "Notification permission denied")
                            }
                        }

                    LaunchedEffect(Unit) {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.POST_NOTIFICATIONS,
                            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // Date preferences: five DataStore flows used to be opened per video card,
                // metadata line, info section, description sheet and info dialog.
                ProvideWindowSizeClass {
                    ProvideDateDisplaySettings {
                        // Card preferences and watch progress are collected once here. Cards used to
                        // collect them individually, so a feed of ten opened ten Room observers and
                        // fifty DataStore collectors.
                        ProvideVideoCardState {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .semantics { testTagsAsResourceId = true },
                            ) {
                                // 1. MAIN APP (Home/NavHost)
                                // This loads *behind* the splash screen immediately.
                                // By the time splash fades, this is ready.
                                val deeplinkVideoId by this@MainActivity.deeplinkVideoId
                                val isDeeplinkShort by this@MainActivity.isDeeplinkShort
                                val openMusicPlayerRequest by this@MainActivity.openMusicPlayerRequest
                                val pendingWidgetRoute by this@MainActivity.pendingWidgetRoute

                                if (appUiRoot == AppUiRoot.TV) {
                                    NanzTvApp(
                                        deeplinkVideoId = deeplinkVideoId,
                                        isShort = isDeeplinkShort,
                                        onDeeplinkConsumed = { consumeDeeplink() },
                                    )
                                } else {
                                    ProvideChannelGroupLabels {
                                        NanzApp(
                                            currentTheme = themeMode,
                                            themeVariant = themeVariant,
                                            customThemePalettes = customThemePalettes,
                                            systemLightThemeMode = systemLightThemeMode,
                                            systemDarkThemeMode = systemDarkThemeMode,
                                            systemDarkThemeVariant = systemDarkThemeVariant,
                                            onThemeChange = { newTheme ->
                                                themeMode = newTheme
                                                scope.launch {
                                                    dataManager.setThemeMode(newTheme)
                                                }
                                            },
                                            onThemeVariantChange = { variant ->
                                                themeVariant = variant
                                                scope.launch {
                                                    dataManager.setThemeVariant(variant)
                                                }
                                            },
                                            onCustomThemePalettesChange = { palettes ->
                                                customThemePalettes = palettes
                                                scope.launch {
                                                    dataManager.setCustomThemePalettes(palettes)
                                                }
                                            },
                                            onSystemLightThemeChange = { newTheme ->
                                                systemLightThemeMode = newTheme
                                                scope.launch {
                                                    dataManager.setSystemLightThemeMode(newTheme)
                                                }
                                            },
                                            onSystemDarkThemeChange = { newTheme ->
                                                systemDarkThemeMode = newTheme
                                                scope.launch {
                                                    dataManager.setSystemDarkThemeMode(newTheme)
                                                }
                                            },
                                            onSystemDarkThemeVariantChange = { variant ->
                                                systemDarkThemeVariant = variant
                                                scope.launch {
                                                    dataManager.setSystemDarkThemeVariant(variant)
                                                }
                                            },
                                            deeplinkVideoId = deeplinkVideoId,
                                            isShort = isDeeplinkShort,
                                            openMusicPlayerRequest = openMusicPlayerRequest,
                                            onDeeplinkConsumed = {
                                                consumeDeeplink()
                                            },
                                            pendingWidgetRoute = pendingWidgetRoute,
                                            onWidgetRouteConsumed = {
                                                _pendingWidgetRoute.value = null
                                            },
                                        )
                                    }
                                }

                                // 2. THE SPLASH SCREEN (Z-Index Top)
                                if (showSplash) {
                                    com.fathtube.app.ui.components.NanzSplashScreen(
                                        onAnimationFinished = {
                                            showSplash = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        DiscordPresenceRuntime.setAppForeground(true)
    }

    override fun onDestroy() {
        videoLifecycleLog("onDestroy")
        DiscordPresenceRuntime.detachActivity(this)
        val playerManager =
            com.fathtube.app.player.EnhancedPlayerManager
                .getInstance()
        val playerState = playerManager.playerState.value
        val hasActiveVideo =
            playerState.currentVideoId != null &&
                (playerState.playWhenReady || playerState.isPlaying || playerState.isBuffering)
        val shouldKeepBackgroundPlayback =
            BackgroundPlaybackPolicy.shouldKeepPlaybackInBackground(
                backgroundPlaybackPreferenceEnabled = lifecyclePlaybackPreferences.settings.backgroundPlayEnabled,
                explicitBackgroundPlaybackActive = GlobalPlayerState.isExplicitBackgroundPlaybackActive.value,
                hasActiveVideo = hasActiveVideo,
            )

        if (shouldKeepBackgroundPlayback) {
            handOffVideoPlaybackToBackground()
        } else if (!isChangingConfigurations) {
            GlobalPlayerState.release()
        }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val data = intent.data
        val notificationVideoId = intent.getStringExtra("notification_video_id") ?: intent.getStringExtra("video_id")

        val widgetRoute =
            intent.getStringExtra(
                com.fathtube.app.widget.core.WidgetDeepLink.EXTRA_WIDGET_ROUTE,
            )
        if (widgetRoute != null) {
            intent.removeExtra(com.fathtube.app.widget.core.WidgetDeepLink.EXTRA_WIDGET_ROUTE)
            _pendingWidgetRoute.value = widgetRoute
            return
        }

        if (intent.getBooleanExtra("open_music_player", false)) {
            _deeplinkVideoId.value = null
            _isDeeplinkShort.value = false
            _openMusicPlayerRequest.intValue += 1
            intent.removeExtra("notification_video_id")
            intent.removeExtra("video_id")
            intent.removeExtra("deeplink_video_id")
            return
        }

        if (intent.getBooleanExtra("open_video_player", false)) {
            intent.removeExtra("open_video_player")
            val currentVideoId = GlobalPlayerState.currentVideo.value?.id
            if (currentVideoId != null) {
                _isDeeplinkShort.value = false
                _deeplinkVideoId.value = currentVideoId
            }
            return
        }

        // Reset shorts flag
        _isDeeplinkShort.value = false

        val videoId =
            if (data != null && intent.action == Intent.ACTION_VIEW) {
                val urlString = data.toString()
                if (urlString.contains("shorts/")) {
                    _isDeeplinkShort.value = true
                }
                extractVideoId(urlString)
            } else if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (sharedText != null) {
                    if (sharedText.contains("shorts/")) {
                        _isDeeplinkShort.value = true
                    }
                    extractVideoId(sharedText)
                } else {
                    null
                }
            } else {
                notificationVideoId
            }
        // Check extra
        if (intent.getBooleanExtra("is_short", false) || intent.getBooleanExtra("is_shorts", false)) {
            _isDeeplinkShort.value = true
        }

        if (videoId != null) {
            _deeplinkVideoId.value = videoId
            intent.putExtra("deeplink_video_id", videoId)
        }

        // Check for Update Notification extras
        if (intent.hasExtra("EXTRA_UPDATE_VERSION")) {
            val version = intent.getStringExtra("EXTRA_UPDATE_VERSION") ?: ""
            val changelog = intent.getStringExtra("EXTRA_UPDATE_CHANGELOG") ?: ""
            val url = intent.getStringExtra("EXTRA_UPDATE_URL") ?: ""
            _pendingUpdateInfo.value = UpdateInfo(version, changelog, url, true)
        }
    }

    fun consumeDeeplink() {
        _deeplinkVideoId.value = null
        _isDeeplinkShort.value = false
    }

    private fun extractVideoId(url: String): String? {
        val patterns =
            listOf(
                Regex("v=([^&]+)"),
                Regex("shorts/([^/?]+)"),
                Regex("youtu.be/([^/?]+)"),
                Regex("embed/([^/?]+)"),
                Regex("v/([^/?]+)"),
            )
        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) return match.groupValues[1]
        }
        return url.substringAfterLast("/").substringBefore("?").ifEmpty { null }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        videoLifecycleLog("onPictureInPictureModeChanged pip=$isInPictureInPictureMode")
        GlobalPlayerState.setPipMode(isInPictureInPictureMode)
        pendingAutoPip = false

        clearWindowBrightnessOverride()

        pipDismissCheckJob?.cancel()
        if (!isInPictureInPictureMode) {
            pipDismissCheckJob =
                lifecycleScope.launch {
                    delay(350L)
                    val stillBackgrounded = !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                    if (stillBackgrounded && !isInPictureInPictureMode) {
                        GlobalPlayerState.requestDismiss()
                        com.fathtube.app.player.EnhancedPlayerManager
                            .getInstance()
                            .stop()
                        com.fathtube.app.player.EnhancedPlayerManager
                            .getInstance()
                            .stopBackgroundService()
                    }
                }
        }
    }

    private fun releaseOrientationLock() {
        if (requestedOrientation != android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    private fun clearWindowBrightnessOverride() {
        val layoutParams = window.attributes
        if (layoutParams.screenBrightness != android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
            layoutParams.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = layoutParams
        }
    }

    override fun onResume() {
        super.onResume()
        NanzCrashHandler.recordPhase("activity", "onResume pip=$isInPictureInPictureMode")
        videoLifecycleLog("onResume")
        // GlobalPlayerState outlives this Activity, so an instance destroyed straight out of PiP
        // without an onPictureInPictureModeChanged(false) would leave the flag latched true for
        // the rest of the process. Re-reading the real value here is the only reset path.
        GlobalPlayerState.setPipMode(isInPictureInPictureMode)
        pendingAutoPip = false
        pipDismissCheckJob?.cancel()
        PictureInPictureHelper.dismissPopup(this)
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        if (
            (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) &&
            com.fathtube.app.player.PlayerHardwareController.fullscreenVideoActive.value
        ) {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val direction =
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                        AudioManager.ADJUST_RAISE
                    } else {
                        AudioManager.ADJUST_LOWER
                    }
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    direction,
                    if (com.fathtube.app.player.PlayerHardwareController.inAppVolumeOverlayEnabled.value) {
                        0
                    } else {
                        AudioManager.FLAG_SHOW_UI
                    },
                )
                if (com.fathtube.app.player.PlayerHardwareController.inAppVolumeOverlayEnabled.value) {
                    com.fathtube.app.player.PlayerHardwareController
                        .notifyVolumeKey()
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        if (
            (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) &&
            com.fathtube.app.player.PlayerHardwareController.fullscreenVideoActive.value
        ) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onStop() {
        DiscordPresenceRuntime.setAppForeground(false)
        super.onStop()
        NanzCrashHandler.recordPhase(
            "activity",
            "onStop pip=$isInPictureInPictureMode " +
                "backgroundPlay=${lifecyclePlaybackPreferences.settings.backgroundPlayEnabled} " +
                "shortsBackground=${lifecyclePlaybackPreferences.settings.shortsBackgroundPlay}",
        )
        videoLifecycleLog("onStop")
        if (!isInPictureInPictureMode && !PictureInPictureHelper.isPopupActive) {
            if (cachedAppUiRoot == AppUiRoot.MOBILE) {
                releaseOrientationLock()
            }
            if (!lifecyclePlaybackPreferences.settings.shortsBackgroundPlay) {
                com.fathtube.app.player.shorts.ShortsPlayerPool
                    .getInstance()
                    .pauseAll()
            }

            if (pendingAutoPip) {
                lifecycleScope.launch {
                    delay(800L)
                    if (
                        pendingAutoPip &&
                        !isInPictureInPictureMode &&
                        !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                    ) {
                        pendingAutoPip = false
                        handleBackgroundPlaybackOnStop()
                    }
                }
            } else {
                handleBackgroundPlaybackOnStop()
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (cachedAppUiRoot == AppUiRoot.TV) return
        val explicitBackgroundPlaybackActive =
            GlobalPlayerState.isExplicitBackgroundPlaybackActive.value
        NanzCrashHandler.recordPhase(
            "activity",
            "onUserLeaveHint autoPip=${lifecyclePlaybackPreferences.settings.autoPipEnabled} " +
                "explicitBackground=$explicitBackgroundPlaybackActive",
        )
        videoLifecycleLog("onUserLeaveHint")
        // Only enter PiP mode if video is playing and has progressed
        // We use the EnhancedPlayerManager directly to get the immediate state
        val playerManager =
            com.fathtube.app.player.EnhancedPlayerManager
                .getInstance()
        val musicManager = com.fathtube.app.player.EnhancedMusicPlayerManager

        val isVideoPlaying =
            playerManager.playerState.value.isPlaying &&
                playerManager.playerState.value.currentVideoId != null &&
                playerManager.getCurrentPosition() > 500 // At least 0.5s in
        val isMusicPlaying = musicManager.playerState.value.isPlaying

        // Only enter PiP for video, not for music (which uses background service)
        val shouldEnterAutoPip =
            BackgroundPlaybackPolicy.shouldEnterAutoPip(
                autoPipEnabled = lifecyclePlaybackPreferences.settings.autoPipEnabled,
                isVideoPlaying = isVideoPlaying,
                explicitBackgroundPlaybackActive = explicitBackgroundPlaybackActive,
            )
        if (shouldEnterAutoPip && !isMusicPlaying) {
            enterPlayerPictureInPictureMode(
                aspectRatio = PictureInPictureHelper.currentVideoAspectRatio,
                isPlaying = true,
            )
            return
        }

        if (!lifecyclePlaybackPreferences.settings.shortsPipEnabled || isMusicPlaying) return
        val shortsPool =
            com.fathtube.app.player.shorts.ShortsPlayerPool
                .getInstance()
        if (!shortsPool.isPlaying()) return
        enterPlayerPictureInPictureMode(
            aspectRatio = shortsPool.activeVideoAspectRatio() ?: PORTRAIT_REEL_ASPECT_RATIO,
            isPlaying = true,
        )
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        NanzCrashHandler.recordPhase("memory", "MainActivity.onTrimMemory level=$level")
        if (MemoryPressurePolicy.shouldReleaseVideoPlayback(level)) {
            com.fathtube.app.player.EnhancedPlayerManager
                .getInstance()
                .handleCriticalMemoryPressure()
        }
    }

    fun enterPlayerPictureInPictureMode(
        aspectRatio: Float = PictureInPictureHelper.currentVideoAspectRatio,
        isPlaying: Boolean = true,
        openSettingsOnDenied: Boolean = false,
    ): Boolean {
        if (cachedAppUiRoot == AppUiRoot.TV) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        if (!PictureInPictureHelper.isPipAllowed(this)) {
            if (openSettingsOnDenied) {
                PictureInPictureHelper.openPipSettings(this)
            }
            return false
        }

        pendingAutoPip = true
        val entered =
            PictureInPictureHelper.enterPipMode(
                activity = this,
                aspectRatio = aspectRatio,
                isPlaying = isPlaying,
                autoEnterEnabled = false,
            )
        if (!entered) {
            pendingAutoPip = false
        }
        return entered
    }

    private fun handOffVideoPlaybackToBackground() {
        NanzCrashHandler.recordPhase("background-handoff", "handOffVideoPlaybackToBackground")
        videoLifecycleLog("handOffVideoPlaybackToBackground")
        val playerManager =
            com.fathtube.app.player.EnhancedPlayerManager
                .getInstance()
        val playerState = playerManager.playerState.value
        if (
            playerState.currentVideoId != null &&
            (playerState.playWhenReady || playerState.isPlaying || playerState.isBuffering)
        ) {
            val video = GlobalPlayerState.currentVideo.value
            playerManager.startBackgroundService(
                videoId = video?.id ?: playerState.currentVideoId,
                title = video?.title?.ifEmpty { "Playing..." } ?: "Playing...",
                channel = video?.channelName ?: "",
                thumbnail = video?.thumbnailUrl ?: "",
            )
            playerManager.continueVideoPlaybackInBackground()
        }
    }

    private fun handleBackgroundPlaybackOnStop() {
        NanzCrashHandler.recordPhase("background-handoff", "handleBackgroundPlaybackOnStop")
        videoLifecycleLog("handleBackgroundPlaybackOnStop")
        val playerManager =
            com.fathtube.app.player.EnhancedPlayerManager
                .getInstance()
        val playerState = playerManager.playerState.value
        val hasActiveVideo =
            playerState.currentVideoId != null &&
                (playerState.playWhenReady || playerState.isPlaying || playerState.isBuffering)

        if (!hasActiveVideo) return

        val shouldKeepBackgroundPlayback =
            BackgroundPlaybackPolicy.shouldKeepPlaybackInBackground(
                backgroundPlaybackPreferenceEnabled = lifecyclePlaybackPreferences.settings.backgroundPlayEnabled,
                explicitBackgroundPlaybackActive = GlobalPlayerState.isExplicitBackgroundPlaybackActive.value,
                hasActiveVideo = hasActiveVideo,
            )

        if (shouldKeepBackgroundPlayback) {
            videoLifecycleLog("handleBackgroundPlaybackOnStop handoff")
            handOffVideoPlaybackToBackground()
        } else {
            videoLifecycleLog("handleBackgroundPlaybackOnStop pause")
            playerManager.pause()
            playerManager.stopBackgroundService()
        }
    }



    companion object {
        const val EXTRA_BENCHMARK_BYPASS_ONBOARDING = "com.fathtube.app.extra.BENCHMARK_BYPASS_ONBOARDING"
    }
}
