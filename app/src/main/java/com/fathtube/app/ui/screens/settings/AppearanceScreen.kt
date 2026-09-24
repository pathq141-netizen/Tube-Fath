package com.fathtube.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fathtube.app.R
import com.fathtube.app.ui.components.layout.topbar.NanzTopBar
import com.fathtube.app.ui.theme.CustomThemePalettes
import com.fathtube.app.ui.theme.ThemeMode
import com.fathtube.app.ui.theme.ThemeVariant

data class ThemeOption(
    val mode: ThemeMode,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    currentTheme: ThemeMode,
    themeVariant: ThemeVariant,
    customThemePalettes: CustomThemePalettes,
    systemLightThemeMode: ThemeMode,
    systemDarkThemeMode: ThemeMode,
    systemDarkThemeVariant: ThemeVariant,
    onThemeChange: (ThemeMode) -> Unit,
    onThemeVariantChange: (ThemeVariant) -> Unit,
    onCustomThemePalettesChange: (CustomThemePalettes) -> Unit,
    onSystemLightThemeChange: (ThemeMode) -> Unit,
    onSystemDarkThemeChange: (ThemeMode) -> Unit,
    onSystemDarkThemeVariantChange: (ThemeVariant) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val options = listOf(
        ThemeOption(
            mode = ThemeMode.SYSTEM,
            title = stringResource(R.string.theme_name_system_default),
            subtitle = stringResource(R.string.theme_desc_system_default),
            icon = Icons.Outlined.PhoneAndroid,
        ),
        ThemeOption(
            mode = ThemeMode.DARK,
            title = stringResource(R.string.theme_name_classic_dark),
            subtitle = "Sleek YouTube dark style",
            icon = Icons.Outlined.DarkMode,
        ),
        ThemeOption(
            mode = ThemeMode.OLED,
            title = stringResource(R.string.theme_name_true_black),
            subtitle = "Pure black for OLED & battery savings",
            icon = Icons.Outlined.BrightnessAuto,
        ),
        ThemeOption(
            mode = ThemeMode.LIGHT,
            title = stringResource(R.string.theme_name_pure_light),
            subtitle = stringResource(R.string.theme_desc_pure_light),
            icon = Icons.Outlined.LightMode,
        ),
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            NanzTopBar(
                title = stringResource(R.string.settings_item_theme),
                onBack = onNavigateBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Theme / Tampilan",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            items(options.size) { index ->
                val option = options[index]
                val selected = currentTheme == option.mode

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onThemeChange(option.mode)
                            when (option.mode) {
                                ThemeMode.OLED -> onThemeVariantChange(ThemeVariant.AMOLED)
                                ThemeMode.LIGHT -> onThemeVariantChange(ThemeVariant.LIGHT)
                                else -> onThemeVariantChange(ThemeVariant.DARK)
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    tonalElevation = if (selected) 4.dp else 1.dp,
                    border = if (selected) {
                        androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    } else null,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = option.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        RadioButton(
                            selected = selected,
                            onClick = {
                                onThemeChange(option.mode)
                                when (option.mode) {
                                    ThemeMode.OLED -> onThemeVariantChange(ThemeVariant.AMOLED)
                                    ThemeMode.LIGHT -> onThemeVariantChange(ThemeVariant.LIGHT)
                                    else -> onThemeVariantChange(ThemeVariant.DARK)
                                }
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            }
        }
    }
}
