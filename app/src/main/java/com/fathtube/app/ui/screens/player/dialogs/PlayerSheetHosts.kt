package com.fathtube.app.ui.screens.player.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fathtube.app.R
import com.fathtube.app.data.model.Comment
import com.fathtube.app.data.model.LiveChatMessage
import com.fathtube.app.data.model.Video
import com.fathtube.app.data.model.toVideo
import com.fathtube.app.data.model.uploadDateMillis
import com.fathtube.app.player.EnhancedPlayerManager
import com.fathtube.app.ui.components.shared.NanzDescriptionBottomSheet
import com.fathtube.app.ui.components.shared.MediaSleepTimerSheet
import com.fathtube.app.ui.components.shared.commentTimestampToMs
import com.fathtube.app.ui.components.shared.rememberDateDisplaySettings
import com.fathtube.app.ui.components.videoplayer.sheet.NanzChaptersBottomSheet
import com.fathtube.app.ui.components.videoplayer.sheet.NanzTranscriptBottomSheet
import com.fathtube.app.ui.components.videoplayer.sheet.LiveChatList
import com.fathtube.app.ui.components.videoplayer.sheet.PlayerCommentsPanel
import com.fathtube.app.ui.screens.player.VideoPlayerViewModel
import com.fathtube.app.ui.screens.player.state.PlayerCommentsUiState
import com.fathtube.app.ui.screens.player.state.PlayerScreenState
import com.fathtube.app.ui.screens.player.state.VideoPlayerUiState
import com.fathtube.app.ui.screens.player.state.selectCommentSort
import com.fathtube.app.ui.screens.player.state.visibleComments
import com.fathtube.app.utils.DateContext
import org.schabi.newpipe.extractor.stream.StreamSegment

/**
 * The surfaces the portrait bottom-sheet slot, the landscape fullscreen side panel and the tablet
 * detail column all raise. Each is wired once here; `asSidePanel` picks the drawer geometry (no
 * vertical dismiss, fills the drawer) over the bottom-sheet geometry.
 */
@Composable
internal fun PlayerChaptersSheetHost(
    screenState: PlayerScreenState,
    chapters: List<StreamSegment>,
    thumbnailUrl: String,
    asSidePanel: Boolean,
    expandedHeight: Dp?,
    onDismiss: () -> Unit,
    collapsedHeight: Dp = 0.dp,
    onSheetProgressChange: (Float) -> Unit = {},
) {
    val chaptersPositionMs by remember {
        derivedStateOf { (screenState.currentPosition / 1_000L) * 1_000L }
    }
    NanzChaptersBottomSheet(
        chapters = chapters,
        currentPosition = chaptersPositionMs,
        durationMs = screenState.duration,
        onChapterClick = { newPosition ->
            EnhancedPlayerManager.getInstance().seekTo(newPosition)
        },
        onDismiss = onDismiss,
        thumbnailUrl = thumbnailUrl,
        expandedHeight = expandedHeight,
        collapsedHeight = collapsedHeight,
        enableVerticalDismiss = !asSidePanel,
        onSheetProgressChange = onSheetProgressChange,
        modifier = if (asSidePanel) Modifier.fillMaxSize() else Modifier,
    )
}

@Composable
internal fun PlayerDescriptionSheetHost(
    video: Video,
    uiState: VideoPlayerUiState,
    viewModel: VideoPlayerViewModel,
    asSidePanel: Boolean,
    expandedHeight: Dp?,
    onDismiss: () -> Unit,
    onChaptersClick: (() -> Unit)? = null,
    onTranscriptClick: (() -> Unit)? = null,
    onChannelClick: ((String) -> Unit)? = null,
    hasTranscriptTrack: Boolean = false,
    collapsedHeight: Dp = 0.dp,
    onSheetProgressChange: (Float) -> Unit = {},
) {
    val descriptionPage by viewModel.descriptionState.collectAsStateWithLifecycle()
    LaunchedEffect(video.id) {
        viewModel.loadDescription(video.id)
    }
    val dateSettings = rememberDateDisplaySettings()
    val currentVideo =
        remember(uiState.streamInfo, video, uiState.channelAvatarUrl, dateSettings) {
            val streamInfo = uiState.streamInfo ?: return@remember video
            streamInfo.toVideo(
                base = video,
                uploadDateText =
                    streamInfo.textualUploadDate
                        ?: streamInfo.uploadDateMillis
                            ?.let { dateSettings.format(date = null, context = DateContext.DESCRIPTION, timestampFallbackMs = it) }
                            ?.takeIf { it.isNotBlank() }
                        ?: video.uploadDate,
                channelAvatarUrl = uiState.channelAvatarUrl,
                likeCount = streamInfo.likeCount,
            )
        }
    NanzDescriptionBottomSheet(
        video = currentVideo,
        descriptionPage = descriptionPage,
        tags = uiState.streamInfo?.tags ?: emptyList(),
        chapterCount = uiState.chapters.size,
        onChaptersClick = onChaptersClick,
        onTranscriptClick = onTranscriptClick?.takeIf { hasTranscriptTrack },
        onChannelClick = onChannelClick,
        artworkUrl = currentVideo.thumbnailUrl,
        onSeekMs = { EnhancedPlayerManager.getInstance().seekTo(it) },
        expandedHeight = expandedHeight,
        collapsedHeight = collapsedHeight,
        enableVerticalDismiss = !asSidePanel,
        onSheetProgressChange = onSheetProgressChange,
        onDismiss = onDismiss,
        modifier = if (asSidePanel) Modifier.fillMaxSize() else Modifier,
    )
}

@Composable
internal fun PlayerTranscriptSheetHost(
    viewModel: VideoPlayerViewModel,
    screenState: PlayerScreenState,
    trackUrl: String?,
    artworkUrl: String?,
    asSidePanel: Boolean,
    expandedHeight: Dp?,
    onDismiss: () -> Unit,
    collapsedHeight: Dp = 0.dp,
    onSheetProgressChange: (Float) -> Unit = {},
) {
    val transcript by viewModel.transcriptState.collectAsStateWithLifecycle()
    LaunchedEffect(trackUrl) {
        viewModel.loadTranscript(trackUrl)
    }
    NanzTranscriptBottomSheet(
        cues = transcript.cues,
        isLoading = transcript.isLoading,
        currentPositionMs = { screenState.currentPosition },
        artworkUrl = artworkUrl,
        onSeekMs = { EnhancedPlayerManager.getInstance().seekTo(it) },
        onDismiss = onDismiss,
        expandedHeight = expandedHeight,
        collapsedHeight = collapsedHeight,
        enableVerticalDismiss = !asSidePanel,
        onSheetProgressChange = onSheetProgressChange,
        modifier = if (asSidePanel) Modifier.fillMaxSize() else Modifier,
    )
}

@Composable
internal fun PlayerSleepTimerSheetHost(
    asSidePanel: Boolean,
    expandedHeight: Dp?,
    onDismiss: () -> Unit,
    collapsedHeight: Dp = 0.dp,
    onSheetProgressChange: (Float) -> Unit = {},
) {
    MediaSleepTimerSheet(
        onDismiss = onDismiss,
        expandedHeight = expandedHeight,
        collapsedHeight = collapsedHeight,
        enableVerticalDismiss = !asSidePanel,
        asBottomSheet = !asSidePanel,
        onSheetProgressChange = onSheetProgressChange,
        modifier = if (asSidePanel) Modifier.fillMaxSize() else Modifier,
    )
}

@Composable
internal fun PlayerCommentsPanelHost(
    videoId: String,
    screenState: PlayerScreenState,
    viewModel: VideoPlayerViewModel,
    commentsUiState: PlayerCommentsUiState,
    artworkUrl: String?,
    onNavigateToChannel: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlayerCommentsPanel(
        comments = commentsUiState.visibleComments(screenState),
        isLoading = commentsUiState.isLoading,
        isLoadingMore = commentsUiState.isLoadingMore,
        hasMore = commentsUiState.hasMore,
        selectedFilter = screenState.commentSortFilter,
        totalText = commentsUiState.totalText,
        artworkUrl = artworkUrl,
        timedOnly = screenState.commentsTimedOnly,
        onTimedChange = { screenState.commentsTimedOnly = it },
        onFilterChanged = { filter ->
            commentsUiState.selectCommentSort(filter, videoId, screenState, viewModel)
        },
        onSeekMs = { EnhancedPlayerManager.getInstance().seekTo(it) },
        onLoadReplies = { viewModel.loadCommentReplies(it) },
        onLoadMoreReplies = { viewModel.loadMoreCommentReplies(it) },
        onAuthorClick = { authorChannelRef ->
            onClose()
            onNavigateToChannel(authorChannelRef)
        },
        onLoadMore = { viewModel.loadMoreComments(videoId) },
        onClose = onClose,
        modifier = modifier,
    )
}

@Composable
internal fun PlayerLiveChatColumn(
    messages: List<LiveChatMessage>,
    isLoading: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxHeight()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.live_chat),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.close),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        LiveChatList(
            messages = messages,
            isLoading = isLoading,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
}
