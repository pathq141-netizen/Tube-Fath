package com.fathtube.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.JsonParser
import com.fathtube.app.BuildConfig
import com.fathtube.app.R
import com.fathtube.app.data.local.AppUiModePreferences
import com.fathtube.app.data.local.DEEP_NANZ_NEVER_EXPIRES_HOURS
import com.fathtube.app.data.local.PlayerPreferences
import com.fathtube.app.data.recommendation.NanzNeuroEngine
import com.fathtube.app.data.recommendation.UserBrain
import com.fathtube.app.discord.DiscordPresenceRuntime
import com.fathtube.app.network.AppProxyManager
import com.fathtube.app.platform.AppUiMode
import com.fathtube.app.player.DeepNanzManager
import com.fathtube.app.ui.components.layout.topbar.NanzSearchTopBar
import com.fathtube.app.ui.components.layout.topbar.NanzTopBar
import com.fathtube.app.ui.theme.ThemeMode
import com.fathtube.app.ui.theme.extendedColors
import com.fathtube.app.utils.AppLanguageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: ThemeMode,
    onNavigateBack: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToPlayerAppearance: () -> Unit,
    onNavigateToDonations: () -> Unit,
    onNavigateToPersonality: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToTimeManagement: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToPlayerSettings: () -> Unit,
    onNavigateToProxySettings: () -> Unit,
    onNavigateToVideoQuality: () -> Unit,
    onNavigateToShortsQuality: () -> Unit,
    onNavigateToContentSettings: () -> Unit,
    onNavigateToDateTimeSettings: () -> Unit,
    onNavigateToBufferSettings: () -> Unit,
    onNavigateToSearchHistory: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToUserPreferences: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToAutoBackup: () -> Unit,
    onNavigateToSyncDevices: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToSponsorBlockSettings: () -> Unit,
    onNavigateToDiscordSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val playerPreferences = remember { PlayerPreferences(context) }
    val appUiModePreferences = remember { AppUiModePreferences(context) }
    val appUiMode by appUiModePreferences.mode.collectAsStateWithLifecycle(initialValue = AppUiMode.AUTOMATIC)
    var showInterfaceModeDialog by remember { mutableStateOf(false) }
    val backupRepo =
        remember {
            com.fathtube.app.data.local
                .BackupRepository(context)
        }

    // Brain State
    var userBrain by remember { mutableStateOf<UserBrain?>(null) }
    var refreshBrainTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshBrainTrigger) {
        userBrain = NanzNeuroEngine.getBrainSnapshot()
    }

    var showRegionDialog by remember { mutableStateOf(false) }
    var showAppLanguageDialog by remember { mutableStateOf(false) }
    var showResetBrainDialog by remember { mutableStateOf(false) }
    // Update checker state (github flavor only)
    var isCheckingUpdate by remember { mutableStateOf(false) }
    // null = no dialog; non-null = tag string of the available update
    var updateAvailableTag by remember { mutableStateOf<String?>(null) }

    // Player preferences states
    val currentRegion by playerPreferences.trendingRegion.collectAsState(initial = "US")
    val currentAppLanguage by playerPreferences.appLanguage.collectAsState(initial = AppLanguageManager.SYSTEM_DEFAULT)
    val discordSettingsState by DiscordPresenceRuntime.settingsState.collectAsStateWithLifecycle()
    val discordSettingsSummary = discordSettingsSummaryText(discordSettingsState)

    if (showInterfaceModeDialog) {
        InterfaceModeDialog(
            selected = appUiMode,
            onSelected = { mode ->
                coroutineScope.launch { appUiModePreferences.setMode(mode) }
            },
            onDismiss = { showInterfaceModeDialog = false },
        )
    }

    // Deep Flow state
    val deepNanzActive by playerPreferences.deepNanzActive.collectAsState(initial = false)
    val deepNanzActivatedAt by playerPreferences.deepNanzActivatedAt.collectAsState(initial = 0L)
    val deepNanzExpireHours by playerPreferences.deepNanzExpireHours.collectAsState(initial = 4)
    val deepFlowSaveHistory by playerPreferences.deepNanzSaveToHistory.collectAsState(initial = false)
    var showDeepNanzDurationDialog by remember { mutableStateOf(false) }

    val deepFlowRemainingLabel: String? =
        remember(deepNanzActive, deepNanzActivatedAt, deepNanzExpireHours) {
            if (!deepNanzActive || deepNanzActivatedAt == 0L || deepNanzExpireHours == DEEP_NANZ_NEVER_EXPIRES_HOURS) return@remember null
            val expiresAt = deepNanzActivatedAt + deepNanzExpireHours * 3_600_000L
            val remainingMs = expiresAt - System.currentTimeMillis()
            if (remainingMs <= 0) return@remember null
            val remainingMins = remainingMs / 60_000
            if (remainingMins < 60) {
                context.getString(R.string.duration_minutes_short, remainingMins)
            } else {
                context.getString(
                    R.string.duration_hours_minutes_short,
                    remainingMins / 60,
                    remainingMins % 60,
                )
            }
        }

    // Optimize Region Dialog: compute list only once
    val regionOptions = remember { regionPickerOptions() }
    val appLanguageOptions = remember { AppLanguageManager.getSupportedLanguages() }
    val currentAppLanguageLabel =
        remember(currentAppLanguage, appLanguageOptions) {
            val normalizedLanguage = AppLanguageManager.normalizeLanguageTag(currentAppLanguage)
            if (normalizedLanguage == AppLanguageManager.SYSTEM_DEFAULT) {
                context.getString(R.string.settings_language_system_default)
            } else {
                appLanguageOptions.firstOrNull { it.tag == normalizedLanguage }?.localizedName
                    ?: AppLanguageManager.getLanguageLabel(normalizedLanguage)
            }
        }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(SettingsTab.UMUM) }
    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }

    val onCheckForUpdatesClick: () -> Unit = {
        if (BuildConfig.UPDATER_ENABLED && !isCheckingUpdate) {
            isCheckingUpdate = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val client = AppProxyManager.applyTo(OkHttpClient.Builder()).build()
                    val request =
                        Request
                            .Builder()
                            .url("https://api.github.com/repos/nanasmuda121/FathTube/releases/latest")
                            .header("Accept", "application/vnd.github.v3+json")
                            .build()
                    val response = client.newCall(request).execute()
                    withContext(Dispatchers.Main) {
                        isCheckingUpdate = false
                        if (response.isSuccessful) {
                            val body = response.body?.string()
                            if (body != null) {
                                val json = JsonParser.parseString(body).asJsonObject
                                val latestTag = json.get("tag_name").asString
                                val cleanLatest = latestTag.removePrefix("v")
                                val cleanCurrent = BuildConfig.VERSION_NAME.removePrefix("v")
                                val latestParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }
                                val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
                                var isNewer = false
                                val size = maxOf(latestParts.size, currentParts.size)
                                for (i in 0 until size) {
                                    val l = latestParts.getOrNull(i) ?: 0
                                    val c = currentParts.getOrNull(i) ?: 0
                                    if (l > c) {
                                        isNewer = true
                                        break
                                    }
                                    if (l < c) break
                                }
                                if (isNewer) {
                                    updateAvailableTag = latestTag
                                } else {
                                    android.widget.Toast
                                        .makeText(
                                            context,
                                            context.getString(R.string.nanz_is_up_to_date),
                                            android.widget.Toast.LENGTH_SHORT,
                                        ).show()
                                }
                            }
                        } else {
                            android.widget.Toast
                                .makeText(
                                    context,
                                    context.getString(R.string.update_check_failed),
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isCheckingUpdate = false
                        android.widget.Toast
                            .makeText(
                                context,
                                context.getString(R.string.update_check_failed),
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                    }
                }
            }
        }
    }

    // Section label strings for the search index
    val secFlowEngine = stringResource(R.string.settings_nanz_engine_header)
    val secAppearance = stringResource(R.string.settings_header_appearance)
    val secContentPlayback = stringResource(R.string.settings_header_content_playback)
    val secNotifications = stringResource(R.string.settings_header_notifications)
    val secDataManagement = stringResource(R.string.settings_header_data_management)

    val allSettingsEntries =
        listOf(
            SettingSearchEntry(
                Icons.Outlined.Psychology,
                stringResource(R.string.nanz_control_center),
                stringResource(R.string.neural_interest_map_subtitle),
                secFlowEngine,
                onNavigateToPersonality,
            ),
            SettingSearchEntry(
                Icons.Outlined.Palette,
                stringResource(R.string.settings_item_theme),
                "",
                secAppearance,
                onNavigateToAppearance,
            ),
            SettingSearchEntry(
                Icons.Outlined.Tv,
                stringResource(R.string.settings_item_interface_mode),
                stringResource(R.string.settings_item_interface_mode_subtitle),
                secAppearance,
            ) { showInterfaceModeDialog = true },
            SettingSearchEntry(
                Icons.Outlined.Language,
                stringResource(R.string.settings_item_app_language),
                currentAppLanguageLabel,
                secAppearance,
            ) { showAppLanguageDialog = true },
            SettingSearchEntry(
                Icons.Outlined.Tune,
                stringResource(R.string.settings_item_player_appearance),
                stringResource(R.string.settings_item_player_appearance_subtitle),
                secAppearance,
                onNavigateToPlayerAppearance,
            ),
            SettingSearchEntry(
                Icons.Outlined.GridView,
                stringResource(R.string.settings_item_content_display),
                stringResource(R.string.settings_item_content_display_subtitle),
                secAppearance,
                onNavigateToContentSettings,
            ),
            SettingSearchEntry(
                Icons.Outlined.Schedule,
                stringResource(R.string.settings_item_datetime),
                stringResource(R.string.settings_item_datetime_subtitle),
                secAppearance,
                onNavigateToDateTimeSettings,
            ),
            SettingSearchEntry(
                Icons.Outlined.FilterAlt,
                stringResource(R.string.settings_item_content_prefs),
                stringResource(R.string.settings_item_content_prefs_subtitle),
                secContentPlayback,
                onNavigateToUserPreferences,
            ),
            SettingSearchEntry(
                Icons.Outlined.PlayCircle,
                stringResource(R.string.settings_item_player),
                stringResource(R.string.settings_item_player_subtitle),
                secContentPlayback,
                onNavigateToPlayerSettings,
            ),
            SettingSearchEntry(
                Icons.Outlined.Share,
                stringResource(R.string.discord_presence_title),
                discordSettingsSummary,
                secContentPlayback,
                onNavigateToDiscordSettings,
            ),
            SettingSearchEntry(
                Icons.Outlined.Public,
                stringResource(R.string.settings_item_proxy),
                stringResource(R.string.settings_item_proxy_subtitle),
                secContentPlayback,
                onNavigateToProxySettings,
            ),
            SettingSearchEntry(
                R.drawable.ic_block,
                stringResource(R.string.sb_settings_title),
                stringResource(R.string.sb_settings_subtitle),
                secContentPlayback,
                onNavigateToSponsorBlockSettings,
            ),
            SettingSearchEntry(
                Icons.Outlined.HighQuality,
                stringResource(R.string.settings_item_quality),
                stringResource(R.string.settings_item_quality_subtitle),
                secContentPlayback,
                onNavigateToVideoQuality,
            ),
            SettingSearchEntry(
                Icons.Outlined.Slideshow,
                stringResource(R.string.shorts_quality_settings_title),
                stringResource(R.string.shorts_quality_settings_subtitle),
                secContentPlayback,
                onNavigateToShortsQuality,
            ),
            SettingSearchEntry(
                Icons.Outlined.Speed,
                stringResource(R.string.settings_item_buffer),
                stringResource(R.string.settings_item_buffer_subtitle),
                secContentPlayback,
                onNavigateToBufferSettings,
            ),
            SettingSearchEntry(
                Icons.Outlined.Download,
                stringResource(R.string.settings_item_downloads),
                stringResource(R.string.settings_item_downloads_subtitle),
                secContentPlayback,
                onNavigateToDownloads,
            ),
            SettingSearchEntry(
                Icons.Outlined.TrendingUp,
                stringResource(R.string.settings_item_region),
                REGION_NAMES[currentRegion] ?: currentRegion,
                secContentPlayback,
            ) { showRegionDialog = true },
            SettingSearchEntry(
                Icons.Outlined.NotificationsNone,
                stringResource(R.string.settings_item_notifications),
                stringResource(R.string.settings_item_notifications_subtitle),
                secNotifications,
                onNavigateToNotifications,
            ),
            SettingSearchEntry(
                Icons.Outlined.History,
                stringResource(R.string.settings_item_search_history),
                stringResource(R.string.settings_item_search_history_subtitle),
                secDataManagement,
                onNavigateToSearchHistory,
            ),
            SettingSearchEntry(
                Icons.Outlined.Schedule,
                stringResource(R.string.settings_item_time_management),
                stringResource(R.string.settings_item_time_management_subtitle),
                secDataManagement,
                onNavigateToTimeManagement,
            ),
            SettingSearchEntry(
                Icons.Outlined.FileUpload,
                stringResource(R.string.settings_item_export_data),
                stringResource(R.string.settings_item_export_data_subtitle),
                secDataManagement,
                onNavigateToExport,
            ),
            SettingSearchEntry(
                Icons.Outlined.FileDownload,
                stringResource(R.string.settings_item_import_data),
                stringResource(R.string.settings_item_import_data_subtitle),
                secDataManagement,
                onNavigateToImport,
            ),
            SettingSearchEntry(
                Icons.Outlined.Schedule,
                stringResource(R.string.auto_backup_title),
                stringResource(R.string.auto_backup_subtitle),
                secDataManagement,
                onNavigateToAutoBackup,
            ),
            SettingSearchEntry(
                Icons.Outlined.Devices,
                stringResource(R.string.sync_devices_title),
                stringResource(R.string.sync_devices_subtitle),
                secDataManagement,
                onNavigateToSyncDevices,
            ),
        )
    val filteredEntries =
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allSettingsEntries.filter { entry ->
                entry.title.contains(searchQuery, ignoreCase = true) ||
                    entry.subtitle.contains(searchQuery, ignoreCase = true) ||
                    entry.sectionLabel.contains(searchQuery, ignoreCase = true)
            }
        }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            if (isSearchActive) {
                NanzSearchTopBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onClose = {
                        isSearchActive = false
                        searchQuery = ""
                    },
                    placeholder = stringResource(R.string.ui_search_settings),
                )
            } else {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background),
                ) {
                    NanzTopBar(
                        title = stringResource(R.string.settings_title),
                        onBack = onNavigateBack,
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Outlined.Search, stringResource(R.string.ui_search_settings))
                            }
                        },
                    )
                    SettingsCategoryTabRow(
                        tabs = SettingsTab.entries,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                    )
                }
            }
        },
        modifier = modifier,
    ) { paddingValues ->
        if (isSearchActive && searchQuery.isNotBlank()) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (filteredEntries.isEmpty()) {
                    item {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.settings_search_no_results, searchQuery),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    items(filteredEntries.size) { index ->
                        SettingsSearchResultItem(
                            entry = filteredEntries[index],
                            onNavigate = {
                                isSearchActive = false
                                searchQuery = ""
                                filteredEntries[index].onClick()
                            },
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (selectedTab) {
                    SettingsTab.UMUM -> {
                        generalSettingsSection(
                            onNavigateToPersonality = onNavigateToPersonality,
                            deepNanzActive = deepNanzActive,
                            deepFlowRemainingLabel = deepFlowRemainingLabel,
                            deepNanzExpireHours = deepNanzExpireHours,
                            deepFlowSaveHistory = deepFlowSaveHistory,
                            onToggleDeepNanz = { enabled ->
                                coroutineScope.launch { DeepNanzManager.setEnabled(context, enabled) }
                            },
                            onOpenDeepNanzDurationDialog = { showDeepNanzDurationDialog = true },
                            onToggleDeepNanzSaveHistory = { enabled ->
                                coroutineScope.launch { playerPreferences.setDeepNanzSaveToHistory(enabled) }
                            },
                            currentAppLanguageLabel = currentAppLanguageLabel,
                            onOpenAppLanguageDialog = { showAppLanguageDialog = true },
                            currentRegion = currentRegion,
                            onOpenRegionDialog = { showRegionDialog = true },
                            onNavigateToNotifications = onNavigateToNotifications,
                            onOpenInterfaceModeDialog = { showInterfaceModeDialog = true },
                        )
                    }

                    SettingsTab.TAMPILAN -> {
                        appearanceSettingsSection(
                            currentTheme = currentTheme,
                            currentAppLanguageLabel = currentAppLanguageLabel,
                            onNavigateToAppearance = onNavigateToAppearance,
                            onOpenInterfaceModeDialog = { showInterfaceModeDialog = true },
                            onOpenAppLanguageDialog = { showAppLanguageDialog = true },
                            onNavigateToPlayerAppearance = onNavigateToPlayerAppearance,
                            onNavigateToContentSettings = onNavigateToContentSettings,
                            onNavigateToDateTimeSettings = onNavigateToDateTimeSettings,
                        )
                    }

                    SettingsTab.PEMUTAR -> {
                        playbackSettingsSection(
                            discordSettingsSummary = discordSettingsSummary,
                            currentRegion = currentRegion,
                            onNavigateToUserPreferences = onNavigateToUserPreferences,
                            onNavigateToPlayerSettings = onNavigateToPlayerSettings,
                            onNavigateToDiscordSettings = onNavigateToDiscordSettings,
                            onNavigateToProxySettings = onNavigateToProxySettings,
                            onNavigateToSponsorBlockSettings = onNavigateToSponsorBlockSettings,
                            onNavigateToVideoQuality = onNavigateToVideoQuality,
                            onNavigateToShortsQuality = onNavigateToShortsQuality,
                            onNavigateToBufferSettings = onNavigateToBufferSettings,
                            onNavigateToDownloads = onNavigateToDownloads,
                            onOpenRegionDialog = { showRegionDialog = true },
                        )
                    }

                    SettingsTab.DATA -> {
                        dataSettingsSection(
                            onNavigateToDownloads = onNavigateToDownloads,
                            onNavigateToSearchHistory = onNavigateToSearchHistory,
                            onNavigateToTimeManagement = onNavigateToTimeManagement,
                            onNavigateToExport = onNavigateToExport,
                            onNavigateToImport = onNavigateToImport,
                            onNavigateToAutoBackup = onNavigateToAutoBackup,
                            onNavigateToSyncDevices = onNavigateToSyncDevices,
                        )
                    }

                    SettingsTab.SEMUA -> {
                        generalSettingsSection(
                            onNavigateToPersonality = onNavigateToPersonality,
                            deepNanzActive = deepNanzActive,
                            deepFlowRemainingLabel = deepFlowRemainingLabel,
                            deepNanzExpireHours = deepNanzExpireHours,
                            deepFlowSaveHistory = deepFlowSaveHistory,
                            onToggleDeepNanz = { enabled ->
                                coroutineScope.launch { DeepNanzManager.setEnabled(context, enabled) }
                            },
                            onOpenDeepNanzDurationDialog = { showDeepNanzDurationDialog = true },
                            onToggleDeepNanzSaveHistory = { enabled ->
                                coroutineScope.launch { playerPreferences.setDeepNanzSaveToHistory(enabled) }
                            },
                            currentAppLanguageLabel = currentAppLanguageLabel,
                            onOpenAppLanguageDialog = { showAppLanguageDialog = true },
                            currentRegion = currentRegion,
                            onOpenRegionDialog = { showRegionDialog = true },
                            onNavigateToNotifications = onNavigateToNotifications,
                            onOpenInterfaceModeDialog = { showInterfaceModeDialog = true },
                        )
                        appearanceSettingsSection(
                            currentTheme = currentTheme,
                            currentAppLanguageLabel = currentAppLanguageLabel,
                            onNavigateToAppearance = onNavigateToAppearance,
                            onOpenInterfaceModeDialog = { showInterfaceModeDialog = true },
                            onOpenAppLanguageDialog = { showAppLanguageDialog = true },
                            onNavigateToPlayerAppearance = onNavigateToPlayerAppearance,
                            onNavigateToContentSettings = onNavigateToContentSettings,
                            onNavigateToDateTimeSettings = onNavigateToDateTimeSettings,
                        )
                        playbackSettingsSection(
                            discordSettingsSummary = discordSettingsSummary,
                            currentRegion = currentRegion,
                            onNavigateToUserPreferences = onNavigateToUserPreferences,
                            onNavigateToPlayerSettings = onNavigateToPlayerSettings,
                            onNavigateToDiscordSettings = onNavigateToDiscordSettings,
                            onNavigateToProxySettings = onNavigateToProxySettings,
                            onNavigateToSponsorBlockSettings = onNavigateToSponsorBlockSettings,
                            onNavigateToVideoQuality = onNavigateToVideoQuality,
                            onNavigateToShortsQuality = onNavigateToShortsQuality,
                            onNavigateToBufferSettings = onNavigateToBufferSettings,
                            onNavigateToDownloads = onNavigateToDownloads,
                            onOpenRegionDialog = { showRegionDialog = true },
                        )
                        dataSettingsSection(
                            onNavigateToDownloads = onNavigateToDownloads,
                            onNavigateToSearchHistory = onNavigateToSearchHistory,
                            onNavigateToTimeManagement = onNavigateToTimeManagement,
                            onNavigateToExport = onNavigateToExport,
                            onNavigateToImport = onNavigateToImport,
                            onNavigateToAutoBackup = onNavigateToAutoBackup,
                            onNavigateToSyncDevices = onNavigateToSyncDevices,
                        )
                    }
                }
            }
        }
    }

    if (showDeepNanzDurationDialog) {
        val durationOptions =
            listOf(
                DEEP_NANZ_NEVER_EXPIRES_HOURS to stringResource(R.string.deep_nanz_duration_never),
                1 to stringResource(R.string.deep_nanz_duration_1h),
                2 to stringResource(R.string.deep_nanz_duration_2h),
                4 to stringResource(R.string.deep_nanz_duration_4h),
                6 to stringResource(R.string.deep_nanz_duration_6h),
                8 to stringResource(R.string.deep_nanz_duration_8h),
                12 to stringResource(R.string.deep_nanz_duration_12h),
                24 to stringResource(R.string.deep_nanz_duration_24h),
            )
        AlertDialog(
            onDismissRequest = { showDeepNanzDurationDialog = false },
            icon = { Icon(Icons.Outlined.Timer, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.deep_nanz_dialog_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.deep_nanz_dialog_body),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    durationOptions.forEach { (hours, label) ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch {
                                            playerPreferences.setDeepNanzExpireHours(hours)
                                        }
                                        showDeepNanzDurationDialog = false
                                    }.padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = deepNanzExpireHours == hours,
                                onClick = {
                                    coroutineScope.launch {
                                        playerPreferences.setDeepNanzExpireHours(hours)
                                    }
                                    showDeepNanzDurationDialog = false
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDeepNanzDurationDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showResetBrainDialog) {
        AlertDialog(
            onDismissRequest = { showResetBrainDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.settings_reset_brain_title)) },
            text = {
                Text(
                    stringResource(R.string.settings_reset_brain_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            NanzNeuroEngine.resetBrain(context)
                            refreshBrainTrigger++
                            showResetBrainDialog = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.settings_reset_everything)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetBrainDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    // Update Available Dialog (github flavor only)
    if (BuildConfig.UPDATER_ENABLED) {
        val tag = updateAvailableTag
        if (tag != null) {
            AlertDialog(
                onDismissRequest = { updateAvailableTag = null },
                icon = { Icon(Icons.Outlined.Update, null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text(stringResource(R.string.new_update_available), fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        stringResource(R.string.update_available_template, tag),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        updateAvailableTag = null
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nanasmuda121/FathTube/releases/latest"))
                        context.startActivity(intent)
                    }) {
                        Text(stringResource(R.string.download))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { updateAvailableTag = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }

    if (showAppLanguageDialog) {
        val languageOptions =
            remember(appLanguageOptions) {
                listOf(
                    PickerOption(
                        key = AppLanguageManager.SYSTEM_DEFAULT,
                        label = context.getString(R.string.settings_language_system_default),
                        secondaryLabel = context.getString(R.string.settings_item_app_language_subtitle),
                    ),
                ) +
                    appLanguageOptions.map { option ->
                        PickerOption(
                            key = option.tag,
                            label = option.nativeName,
                            secondaryLabel = option.localizedName.takeIf { it != option.nativeName },
                        )
                    }
            }
        SearchablePickerDialog(
            title = stringResource(R.string.settings_language_dialog_title),
            options = languageOptions,
            selectedKey = AppLanguageManager.normalizeLanguageTag(currentAppLanguage),
            onSelect = { tag ->
                coroutineScope.launch {
                    playerPreferences.setAppLanguage(tag)
                    AppLanguageManager.saveLanguageTag(context, tag)
                    showAppLanguageDialog = false
                    AppLanguageManager.activityContext(context)?.recreate()
                }
            },
            onDismiss = { showAppLanguageDialog = false },
        )
    }

    if (showRegionDialog) {
        SearchablePickerDialog(
            title = stringResource(R.string.settings_region_dialog_title),
            options = regionOptions,
            selectedKey = currentRegion,
            onSelect = { code ->
                coroutineScope.launch {
                    playerPreferences.setTrendingRegion(code)
                    showRegionDialog = false
                }
            },
            onDismiss = { showRegionDialog = false },
            listMaxHeight = 260.dp,
        )
    }

}

private fun getThemeNameRes(theme: ThemeMode): Int =
    when (theme) {
        ThemeMode.LIGHT -> R.string.theme_name_pure_light
        ThemeMode.MINT_LIGHT -> R.string.theme_name_mint_fresh
        ThemeMode.ROSE_LIGHT -> R.string.theme_name_rose_petal
        ThemeMode.SKY_LIGHT -> R.string.theme_name_sky_blue
        ThemeMode.CREAM_LIGHT -> R.string.theme_name_cream_paper
        ThemeMode.DARK -> R.string.theme_name_classic_dark
        ThemeMode.OLED -> R.string.theme_name_true_black
        ThemeMode.MIDNIGHT_BLACK -> R.string.theme_name_midnight
        ThemeMode.OCEAN_BLUE -> R.string.theme_name_deep_ocean
        ThemeMode.FOREST_GREEN -> R.string.theme_name_forest
        ThemeMode.LAVENDER_MIST -> R.string.theme_name_lavender
        ThemeMode.SUNSET_ORANGE -> R.string.theme_name_sunset
        ThemeMode.PURPLE_NEBULA -> R.string.theme_name_nebula
        ThemeMode.ROSE_GOLD -> R.string.theme_name_rose_gold
        ThemeMode.ARCTIC_ICE -> R.string.theme_name_arctic
        ThemeMode.MINTY_FRESH -> R.string.theme_name_mint_night
        ThemeMode.CRIMSON_RED -> R.string.theme_name_crimson
        ThemeMode.COSMIC_VOID -> R.string.theme_name_cosmic_void
        ThemeMode.SOLAR_FLARE -> R.string.theme_name_solar_flare
        ThemeMode.CYBERPUNK -> R.string.theme_name_cyberpunk
        ThemeMode.ROYAL_GOLD -> R.string.theme_name_royal_gold
        ThemeMode.NORDIC_HORIZON -> R.string.theme_name_nordic
        ThemeMode.ESPRESSO -> R.string.theme_name_espresso
        ThemeMode.GUNMETAL -> R.string.theme_name_gunmetal
        ThemeMode.SYSTEM -> R.string.theme_name_system_default
        ThemeMode.MONOCHROME -> R.string.theme_name_monochrome
        ThemeMode.CUSTOM -> R.string.theme_name_custom
        ThemeMode.MATERIAL_YOU -> R.string.theme_name_material_you
    }

private data class SettingSearchEntry(
    val icon: Any,
    val title: String,
    val subtitle: String,
    val sectionLabel: String,
    val onClick: () -> Unit,
)

@Composable
private fun SettingsSearchResultItem(
    entry: SettingSearchEntry,
    onNavigate: () -> Unit,
) {
    Column {
        Text(
            text = entry.sectionLabel.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 72.dp, top = 8.dp, bottom = 2.dp),
        )
        when (entry.icon) {
            is ImageVector -> {
                SettingsItem(
                    icon = entry.icon as ImageVector,
                    title = entry.title,
                    subtitle = entry.subtitle,
                    onClick = onNavigate,
                )
            }

            is Int -> {
                SettingsItem(
                    icon = painterResource(entry.icon as Int),
                    title = entry.title,
                    subtitle = entry.subtitle,
                    onClick = onNavigate,
                )
            }
        }
        HorizontalDivider(
            Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        )
    }
}

enum class SettingsTab(
    val titleRes: Int,
    val icon: ImageVector,
) {
    UMUM(R.string.settings_tab_general, Icons.Outlined.Tune),
    TAMPILAN(R.string.settings_tab_appearance, Icons.Outlined.Palette),
    PEMUTAR(R.string.settings_tab_playback, Icons.Outlined.PlayCircle),
    DATA(R.string.settings_tab_data, Icons.Outlined.Folder),
    SEMUA(R.string.settings_tab_all, Icons.Outlined.List),
}

@Composable
private fun SettingsCategoryTabRow(
    tabs: List<SettingsTab>,
    selectedTab: SettingsTab,
    onTabSelected: (SettingsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tabs.size) { index ->
            val tab = tabs[index]
            val isSelected = tab == selectedTab
            Surface(
                onClick = { onTabSelected(tab) },
                shape = RoundedCornerShape(20.dp),
                color =
                    if (isSelected) {
                        Color(0xFF0070F3)
                    } else {
                        Color(0xFF0C1322)
                    },
                border =
                    BorderStroke(
                        1.dp,
                        if (isSelected) {
                            Color(0xFF00D2FF)
                        } else {
                            Color(0xFF1E293B).copy(alpha = 0.6f)
                        },
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                    )
                    Text(
                        text = stringResource(tab.titleRes),
                        style =
                            MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                            ),
                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                    )
                }
            }
        }
    }
}

@Composable
private fun DeepNanzModeGroup(
    deepNanzActive: Boolean,
    deepFlowRemainingLabel: String?,
    deepNanzExpireHours: Int,
    deepFlowSaveHistory: Boolean,
    onToggleActive: (Boolean) -> Unit,
    onOpenDurationDialog: () -> Unit,
    onToggleSaveHistory: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    SettingsGroup {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onToggleActive(!deepNanzActive) }
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.VisibilityOff,
                contentDescription = null,
                tint =
                    if (deepNanzActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.deep_nanz_mode_title),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (deepNanzActive && deepFlowRemainingLabel != null) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.deep_nanz_learning_paused),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                Text(
                    text =
                        when {
                            deepNanzActive && deepFlowRemainingLabel != null -> {
                                stringResource(
                                    R.string.deep_nanz_expires_in,
                                    deepFlowRemainingLabel,
                                )
                            }

                            deepNanzActive && deepNanzExpireHours == DEEP_NANZ_NEVER_EXPIRES_HOURS -> {
                                stringResource(
                                    R.string.deep_nanz_active_until_disabled,
                                )
                            }

                            else -> {
                                stringResource(R.string.deep_nanz_mode_subtitle)
                            }
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = deepNanzActive,
                onCheckedChange = onToggleActive,
            )
        }

        HorizontalDivider(
            Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDurationDialog() }
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.deep_nanz_expire_duration_title),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text =
                        stringResource(
                            R.string.deep_nanz_expire_duration_subtitle,
                            deepNanzExpireHours.let { hours ->
                                when (hours) {
                                    DEEP_NANZ_NEVER_EXPIRES_HOURS -> {
                                        context.getString(R.string.deep_nanz_duration_never)
                                    }

                                    1 -> {
                                        context.getString(R.string.deep_nanz_duration_1h)
                                    }

                                    2 -> {
                                        context.getString(R.string.deep_nanz_duration_2h)
                                    }

                                    4 -> {
                                        context.getString(R.string.deep_nanz_duration_4h)
                                    }

                                    6 -> {
                                        context.getString(R.string.deep_nanz_duration_6h)
                                    }

                                    8 -> {
                                        context.getString(R.string.deep_nanz_duration_8h)
                                    }

                                    12 -> {
                                        context.getString(R.string.deep_nanz_duration_12h)
                                    }

                                    24 -> {
                                        context.getString(R.string.deep_nanz_duration_24h)
                                    }

                                    else -> {
                                        context.resources.getQuantityString(
                                            R.plurals.deep_nanz_duration_hours,
                                            hours,
                                            hours,
                                        )
                                    }
                                }
                            },
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider(
            Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.deep_nanz_save_history_title),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.deep_nanz_save_history_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = deepFlowSaveHistory,
                onCheckedChange = onToggleSaveHistory,
            )
        }
    }
}

private fun LazyListScope.generalSettingsSection(
    onNavigateToPersonality: () -> Unit,
    deepNanzActive: Boolean,
    deepFlowRemainingLabel: String?,
    deepNanzExpireHours: Int,
    deepFlowSaveHistory: Boolean,
    onToggleDeepNanz: (Boolean) -> Unit,
    onOpenDeepNanzDurationDialog: () -> Unit,
    onToggleDeepNanzSaveHistory: (Boolean) -> Unit,
    currentAppLanguageLabel: String,
    onOpenAppLanguageDialog: () -> Unit,
    currentRegion: String,
    onOpenRegionDialog: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onOpenInterfaceModeDialog: () -> Unit,
) {
    item { SectionHeader(text = stringResource(R.string.settings_header_smart_features)) }
    item {
        SettingsGroup {
            SettingsItem(
                icon = Icons.Outlined.Psychology,
                title = "Analisis AI Persona & Selera Musik",
                subtitle = "Pantau profil minat musik dan statistik rekomendasi Anda",
                onClick = onNavigateToPersonality,
            )
        }
    }
    item {
        Spacer(Modifier.height(4.dp))
        DeepNanzModeGroup(
            deepNanzActive = deepNanzActive,
            deepFlowRemainingLabel = deepFlowRemainingLabel,
            deepNanzExpireHours = deepNanzExpireHours,
            deepFlowSaveHistory = deepFlowSaveHistory,
            onToggleActive = onToggleDeepNanz,
            onOpenDurationDialog = onOpenDeepNanzDurationDialog,
            onToggleSaveHistory = onToggleDeepNanzSaveHistory,
        )
    }
    item { SectionHeader(text = stringResource(R.string.settings_header_regional)) }
    item {
        SettingsGroup {
            SettingsItem(
                icon = Icons.Outlined.Language,
                title = stringResource(R.string.settings_item_app_language),
                subtitle = currentAppLanguageLabel,
                onClick = onOpenAppLanguageDialog,
            )
            HorizontalDivider(
                Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            SettingsItem(
                icon = Icons.Outlined.TrendingUp,
                title = stringResource(R.string.settings_item_region),
                subtitle = REGION_NAMES[currentRegion] ?: currentRegion,
                onClick = onOpenRegionDialog,
            )
        }
    }
    item { SectionHeader(text = stringResource(R.string.settings_header_notifications)) }
    item {
        SettingsGroup {
            SettingsItem(
                icon = Icons.Outlined.NotificationsNone,
                title = stringResource(R.string.settings_item_notifications),
                subtitle = stringResource(R.string.settings_item_notifications_subtitle),
                onClick = onNavigateToNotifications,
            )
        }
    }
    item { SectionHeader(text = stringResource(R.string.settings_item_interface_mode)) }
    item {
        SettingsGroup {
            SettingsItem(
                icon = Icons.Outlined.Tv,
                title = stringResource(R.string.settings_item_interface_mode),
                subtitle = stringResource(R.string.settings_item_interface_mode_subtitle),
                onClick = onOpenInterfaceModeDialog,
            )
        }
    }
}

private fun LazyListScope.appearanceSettingsSection(
    currentTheme: ThemeMode,
    currentAppLanguageLabel: String,
    onNavigateToAppearance: () -> Unit,
    onOpenInterfaceModeDialog: () -> Unit,
    onOpenAppLanguageDialog: () -> Unit,
    onNavigateToPlayerAppearance: () -> Unit,
    onNavigateToContentSettings: () -> Unit,
    onNavigateToDateTimeSettings: () -> Unit,
) {
    item { SectionHeader(text = stringResource(R.string.settings_header_appearance)) }
    item {
        SettingsGroup {
            SettingsItem(
                icon = Icons.Outlined.Palette,
                title = stringResource(R.string.settings_item_theme),
                subtitle = stringResource(getThemeNameRes(currentTheme)),
                onClick = onNavigateToAppearance,
            )
            HorizontalDivider(
                Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            SettingsItem(
                icon = Icons.Outlined.Tv,
                title = stringResource(R.string.settings_item_interface_mode),
                subtitle = stringResource(R.string.settings_item_interface_mode_subtitle),
                onClick = onOpenInterfaceModeDialog,
            )
            HorizontalDivider(
                Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            SettingsItem(
                icon = Icons.Outlined.Language,
                title = stringResource(R.string.settings_item_app_language),
                subtitle = currentAppLanguageLabel,
                onClick = onOpenAppLanguageDialog,
            )
            HorizontalDivider(
                Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            SettingsItem(
                icon = Icons.Outlined.Tune,
                title = stringResource(R.string.settings_item_player_appearance),
                subtitle = stringResource(R.string.settings_item_player_appearance_subtitle),
                onClick = onNavigateToPlayerAppearance,
            )
            HorizontalDivider(
                Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            SettingsItem(
                icon = Icons.Outlined.GridView,
                title = stringResource(R.string.settings_item_content_display),
                subtitle = stringResource(R.string.settings_item_content_display_subtitle),
                onClick = onNavigateToContentSettings,
            )
            HorizontalDivider(
                Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            SettingsItem(
                icon = Icons.Outlined.Schedule,
                title = stringResource(R.string.settings_item_datetime),
                subtitle = stringResource(R.string.settings_item_datetime_subtitle),
                onClick = onNavigateToDateTimeSettings,
            )
        }
    }
}

private fun LazyListScope.playbackSettingsSection(
    discordSettingsSummary: String,
    currentRegion: String,
    onNavigateToUserPreferences: () -> Unit,
    onNavigateToPlayerSettings: () -> Unit,
    onNavigateToDiscordSettings: () -> Unit,
    onNavigateToProxySettings: () -> Unit,
    onNavigateToSponsorBlockSettings: () -> Unit,
    onNavigateToVideoQuality: () -> Unit,
    onNavigateToShortsQuality: () -> Unit,
    onNavigateToBufferSettings: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onOpenRegionDialog: () -> Unit,
) {
    item { SectionHeader(text = stringResource(R.string.settings_header_content_playback)) }
    item {
        SettingsGroup {
            SettingsItem(
                icon = Icons.Outlined.FilterAlt,
                title = stringResource(R.string.settings_item_content_prefs),
                subtitle = stringResource(R.string.settings_item_content_prefs_subtitle),
                onClick = onNavigateToUserPreferences,
            )
            HorizontalDivider(
                Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            SettingsItem(
                icon = Icons.Outlined.PlayCircle,
                title = stringResource(R.string.settings_item_player),
                subtitle = stringResource(R.string.settings_item_player_subtitle),
                onClick = onNavigateToPlayerSettings,
            )
            HorizontalDivider(
                Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            SettingsItem(
                icon = Icons.Outlined.Share,
                title = stringResource(R.string.discord_presence_title),
                subtitle = discordSettingsSummary,
                onClick = onNavigateToDiscordSettings,
            )
            HorizontalDivider(
                Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            SettingsItem(
                icon = Icons.Outlined.Public,
                title = stringResource(R.string.settings_item_proxy),
                subtitle = stringResource(R.string.settings_item_proxy_subtitle),
                onClick = onNavigateToProxySettings,
            )
            HorizontalDivider(
                Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            SettingsItem(
                icon = painterResource(R.drawable.ic_block),
                title = stringResource(R.string.sb_settings_title),
                subtitle = stringResource(R.string.sb_settings_subtitle),
                onClick = onNavigateToSponsorBlockSettings,
            )
            HorizontalDivider(
                Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            SettingsItem(
                icon = Icons.Outlined.HighQuality,
                title = stringResource(R.string.settings_item_quality),
                subtitle = stringResource(R.string.settings_item_quality_subtitle),
                onClick = onNavigateToVideoQuality,
            )
            HorizontalDivider(
                Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            SettingsItem(
                icon = Icons.Outlined.Slideshow,
                title = stringResource(R.string.shorts_quality_settings_title),
                subtitle = stringResource(R.string.shorts_quality_settings_subtitle),
                onClick = onNavigateToShortsQuality,
            )
            HorizontalDivider(
                Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            SettingsItem(
                icon = Icons.Outlined.Speed,
                title = stringResource(R.string.settings_item_buffer),
                subtitle = stringResource(R.string.settings_item_buffer_subtitle),
                onClick = onNavigateToBufferSettings,
            )
            HorizontalDivider(
                Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            SettingsItem(
                icon = Icons.Outlined.Download,
                title = stringResource(R.string.settings_item_downloads),
                subtitle = stringResource(R.string.settings_item_downloads_subtitle),
                onClick = onNavigateToDownloads,
            )
            HorizontalDivider(
                Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            SettingsItem(
                icon = Icons.Outlined.TrendingUp,
                title = stringResource(R.string.settings_item_region),
                subtitle = REGION_NAMES[currentRegion] ?: currentRegion,
                onClick = onOpenRegionDialog,
            )
        }
    }
}

private fun LazyListScope.dataSettingsSection(
    onNavigateToDownloads: () -> Unit,
    onNavigateToSearchHistory: () -> Unit,
    onNavigateToTimeManagement: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToAutoBackup: () -> Unit,
    onNavigateToSyncDevices: () -> Unit,
) {
    item { SectionHeader(text = stringResource(R.string.settings_item_downloads)) }
    item {
        SettingsGroup {
            SettingsItem(
                icon = Icons.Outlined.Download,
                title = stringResource(R.string.settings_item_downloads),
                subtitle = stringResource(R.string.settings_item_downloads_subtitle),
                onClick = onNavigateToDownloads,
            )
        }
    }
    item { SectionHeader(text = stringResource(R.string.settings_header_data_management)) }
    item {
        SettingsGroup {
            SettingsItem(
                icon = Icons.Outlined.History,
                title = stringResource(R.string.settings_item_search_history),
                subtitle = stringResource(R.string.settings_item_search_history_subtitle),
                onClick = onNavigateToSearchHistory,
            )
            HorizontalDivider(
                Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            SettingsItem(
                icon = Icons.Outlined.Schedule,
                title = stringResource(R.string.settings_item_time_management),
                subtitle = stringResource(R.string.settings_item_time_management_subtitle),
                onClick = onNavigateToTimeManagement,
            )
            HorizontalDivider(
                Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            SettingsItem(
                icon = Icons.Outlined.FileUpload,
                title = stringResource(R.string.settings_item_export_data),
                subtitle = stringResource(R.string.settings_item_export_data_subtitle),
                onClick = onNavigateToExport,
            )
            HorizontalDivider(
                Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            SettingsItem(
                icon = Icons.Outlined.FileDownload,
                title = stringResource(R.string.settings_item_import_data),
                subtitle = stringResource(R.string.settings_item_import_data_subtitle),
                onClick = onNavigateToImport,
            )
            HorizontalDivider(
                Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            SettingsItem(
                icon = Icons.Outlined.Schedule,
                title = stringResource(R.string.auto_backup_title),
                subtitle = stringResource(R.string.auto_backup_subtitle),
                onClick = onNavigateToAutoBackup,
            )
            HorizontalDivider(
                Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            SettingsItem(
                icon = Icons.Outlined.Devices,
                title = stringResource(R.string.sync_devices_title),
                subtitle = stringResource(R.string.sync_devices_subtitle),
                onClick = onNavigateToSyncDevices,
            )
        }
    }
}

