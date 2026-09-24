package com.fathtube.app.ui.components.videoplayer.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fathtube.app.R
import com.fathtube.app.player.AudioTrackOption
import com.fathtube.app.player.QualityOption
import com.fathtube.app.player.SubtitleOption
import com.fathtube.app.player.stream.VideoCodecUtils
import com.fathtube.app.ui.components.shared.NanzNavRow
import com.fathtube.app.ui.components.shared.NanzRowGroup
import com.fathtube.app.ui.components.shared.NanzSelectionRow
import com.fathtube.app.ui.components.shared.MediaAudioTrackRow
import com.fathtube.app.ui.components.shared.MediaPlaybackSpeedPicker
import com.fathtube.app.ui.components.shared.MediaQualitySelectorContent
import com.fathtube.app.ui.components.shared.MediaQualitySelectorOption
import com.fathtube.app.ui.components.shared.audioTrackFallbackLabel
import com.fathtube.app.ui.components.shared.nanzRowGroupShape

@Composable
internal fun PlayerSettingsQualityPage(
    availableQualities: List<QualityOption>,
    currentQuality: Int,
    currentQualityKey: String?,
    useGroupedQualitySelector: Boolean,
    onQualitySelected: (QualityOption) -> Unit,
) {
    val autoLabel = stringResource(R.string.quality_auto)
    val selectorOptions =
        availableQualities.map { quality ->
            MediaQualitySelectorOption(
                item = quality,
                height = quality.height,
                label = if (quality.height == 0) autoLabel else quality.displayLabel(),
                codecKey = quality.codecKey,
                codecLabel =
                    quality.codecKey
                        .takeIf { it.isNotBlank() }
                        ?.let(VideoCodecUtils::codecLabelFromKey)
                        .orEmpty(),
                selected = quality.isSelected(currentQuality, currentQualityKey),
            )
        }

    MediaQualitySelectorContent(
        options = selectorOptions,
        groupedByResolution = useGroupedQualitySelector,
        onOptionSelected = onQualitySelected,
    )
}

private fun QualityOption.displayLabel(): String = label.takeIf { it.isNotBlank() } ?: "${height}p"

private fun QualityOption.isSelected(
    currentQuality: Int,
    currentQualityKey: String?,
): Boolean =
    if (height == 0) {
        currentQuality == 0
    } else {
        streamKey != null && streamKey == currentQualityKey
    }

@Composable
internal fun PlayerSettingsSpeedPage(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onSpeedSelectionFinished: () -> Unit,
) {
    val context = LocalContext.current
    val playerPrefs =
        remember {
            com.fathtube.app.data.local
                .PlayerPreferences(context)
        }
    val customSpeedsEnabled by playerPrefs.customSpeedsEnabled.collectAsState(initial = false)
    val customSpeedPresetsRaw by playerPrefs.customSpeedPresets.collectAsState(initial = "")
    val speedSliderEnabled by playerPrefs.speedSliderEnabled.collectAsState(initial = false)

    MediaPlaybackSpeedPicker(
        currentSpeed = currentSpeed,
        sliderEnabled = speedSliderEnabled,
        customSpeedsEnabled = customSpeedsEnabled,
        customSpeedPresetsRaw = customSpeedPresetsRaw,
        onSpeedSelected = onSpeedSelected,
        onSpeedRowSelected = { onSpeedSelectionFinished() },
    )
}

@Composable
internal fun PlayerSettingsAudioPage(
    availableAudioTracks: List<AudioTrackOption>,
    currentAudioTrack: Int,
    onTrackSelected: (Int) -> Unit,
) {
    NanzRowGroup {
        availableAudioTracks.forEachIndexed { index, track ->
            MediaAudioTrackRow(
                label = audioTrackDisplayLabel(track, index),
                supportingText = track.language.takeIf { it.isNotBlank() },
                selected = index == currentAudioTrack,
                shape = nanzRowGroupShape(index, availableAudioTracks.size),
                onClick = { onTrackSelected(index) },
            )
        }
    }
}

@Composable
internal fun PlayerSettingsSubtitlesPage(
    availableSubtitles: List<SubtitleOption>,
    selectedSubtitleUrl: String?,
    subtitlesEnabled: Boolean,
    onSubtitleSelected: (Int) -> Unit,
    onDisableSubtitles: () -> Unit,
    onShowStyleCustomizer: () -> Unit,
) {
    val automaticLabel = stringResource(R.string.quality_auto)
    val translatedLabel = stringResource(R.string.subtitle_translated)
    val rowCount = availableSubtitles.size + 1
    NanzRowGroup {
        NanzSelectionRow(
            title = stringResource(R.string.off),
            selected = !subtitlesEnabled,
            shape = nanzRowGroupShape(0, rowCount),
            onClick = onDisableSubtitles,
        )
        availableSubtitles.forEachIndexed { index, subtitle ->
            NanzSelectionRow(
                title =
                    when {
                        subtitle.isTranslated -> {
                            stringResource(
                                R.string.subtitle_auto_generated_template,
                                subtitle.label,
                                translatedLabel,
                            )
                        }

                        subtitle.isAutoGenerated -> {
                            stringResource(
                                R.string.subtitle_auto_generated_template,
                                subtitle.label,
                                automaticLabel,
                            )
                        }

                        else -> {
                            subtitle.label
                        }
                    },
                supportingText = subtitle.language.takeIf { it.isNotBlank() },
                selected = subtitle.url == selectedSubtitleUrl && subtitlesEnabled,
                shape = nanzRowGroupShape(index + 1, rowCount),
                onClick = { onSubtitleSelected(index) },
            )
        }
    }
    Spacer(modifier = Modifier.height(SubtitleStyleRowSpacing))
    NanzRowGroup {
        NanzNavRow(
            title = stringResource(R.string.subtitle_style),
            leadingIcon = Icons.Filled.Tune,
            shape = nanzRowGroupShape(0, 1),
            onClick = onShowStyleCustomizer,
        )
    }
}

private val SubtitleStyleRowSpacing = 12.dp

@Composable
internal fun audioTrackDisplayLabel(
    track: AudioTrackOption?,
    fallbackIndex: Int,
): String = track?.label?.takeIf { it.isNotBlank() } ?: audioTrackFallbackLabel(fallbackIndex)
