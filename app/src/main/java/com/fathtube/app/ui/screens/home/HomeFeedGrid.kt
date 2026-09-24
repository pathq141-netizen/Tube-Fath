package com.fathtube.app.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fathtube.app.data.local.VideoHistoryEntry
import com.fathtube.app.data.model.Video
import com.fathtube.app.ui.components.ShortsShelf
import com.fathtube.app.ui.components.VideoCardFullWidth
import com.fathtube.app.ui.components.VideoCardHorizontal
import com.fathtube.app.ui.components.home.ContinueWatchingShelf
import com.fathtube.app.ui.components.shared.NanzFeedProgress
import com.fathtube.app.ui.components.shared.VideoThumbnailImage
import com.fathtube.app.ui.components.shared.pressScale
import com.fathtube.app.ui.theme.ElectricBlue
import com.fathtube.app.ui.theme.NeonCyanBlue
import com.fathtube.app.utils.formatDuration
import com.fathtube.app.utils.formatViewCount

private const val FEED_FOOTER_MIN_VIDEOS = 100

@Composable
internal fun HomeFeedGrid(
    uiState: HomeUiState,
    layoutConfig: HomeLayoutConfig,
    isListView: Boolean,
    gridState: LazyGridState,
    onVideoClick: (Video) -> Unit,
    onChannelClick: (String) -> Unit,
    onEnrichChannelMetadata: (Video) -> Unit,
    onContinueWatchingClick: (VideoHistoryEntry) -> Unit,
    onContinueWatchingRemove: (String) -> Unit,
    onShortClick: (List<Video>, Video) -> Unit,
    onSeeAllHistory: () -> Unit,
    onOpenShortsFeed: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = if (isListView) GridCells.Fixed(1) else layoutConfig.cells,
        modifier =
            modifier
                .fillMaxSize()
                .testTag("home_feed"),
        state = gridState,
        contentPadding =
            PaddingValues(
                start = if (isListView) 0.dp else layoutConfig.contentPadding,
                end = if (isListView) 0.dp else layoutConfig.contentPadding,
                top = 4.dp,
                bottom = 80.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(if (isListView) 0.dp else layoutConfig.cardSpacing),
        verticalArrangement = Arrangement.spacedBy(if (isListView) 0.dp else layoutConfig.cardSpacing),
    ) {
        val videos = uiState.videos
        if (videos.isNotEmpty()) {
            val featuredVideo = videos.firstOrNull()
            val trendingVideos = if (videos.size > 1) videos.drop(1).take(8) else emptyList()
            val remainingVideos = videos.drop(1 + trendingVideos.size)

            // 1. Netflix Hero Billboard Banner
            if (featuredVideo != null) {
                item(
                    key = "netflix_hero_billboard",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    NetflixHeroBillboard(
                        video = featuredVideo,
                        onPlayClick = { onVideoClick(featuredVideo) },
                        onInfoClick = { onVideoClick(featuredVideo) },
                        onChannelClick = onChannelClick,
                        onEnrichChannelMetadata = onEnrichChannelMetadata,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                    )
                }
            }

            // 2. Netflix Horizontal Shelf: "Sedang Tren di FathTube" (Top 8)
            if (trendingVideos.isNotEmpty()) {
                item(
                    key = "netflix_trending_shelf",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    NetflixHorizontalShelf(
                        title = "Sedang Populer di FathTube",
                        badge = "TOP 10",
                        videos = trendingVideos,
                        onVideoClick = onVideoClick,
                        onChannelClick = onChannelClick,
                        onEnrichChannelMetadata = onEnrichChannelMetadata,
                    )
                }
            }

            // 3. Continue Watching Shelf (if user has active watch history)
            if (uiState.continueWatchingVideos.isNotEmpty()) {
                item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "continue_watching_shelf",
                ) {
                    ContinueWatchingShelf(
                        entries = uiState.continueWatchingVideos,
                        onVideoClick = { videoId ->
                            uiState.continueWatchingVideos
                                .find { it.videoId == videoId }
                                ?.let(onContinueWatchingClick)
                        },
                        onRemove = onContinueWatchingRemove,
                        onSeeAllClick = onSeeAllHistory,
                        modifier = Modifier.testTag("home_continue_watching_shelf"),
                    )
                }
            }

            // 4. Shorts Shelf (Nanz Shorts)
            if (uiState.shorts.isNotEmpty()) {
                item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "shorts_shelf",
                ) {
                    ShortsShelf(
                        shorts = uiState.shorts,
                        onShortClick = onShortClick,
                        onSeeAllClick = onOpenShortsFeed,
                        modifier = Modifier.testTag("home_shorts_shelf"),
                    )
                }
            }

            // 5. Netflix Recommendations Section
            if (remainingVideos.isNotEmpty()) {
                item(
                    key = "netflix_recommendations_header",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    NetflixSectionHeader(
                        title = "Rekomendasi Untuk Anda",
                        subtitle = "Berdasarkan video terpopuler",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    )
                }

                feedVideos(
                    videos = remainingVideos,
                    isListView = isListView,
                    onVideoClick = onVideoClick,
                    onChannelClick = onChannelClick,
                    onEnrichChannelMetadata = onEnrichChannelMetadata,
                )
            }
        }

        if (uiState.isLoadingMore) {
            item(
                key = "loading_indicator",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                NanzFeedProgress()
            }
        }

        if (!uiState.hasMorePages && videos.size > FEED_FOOTER_MIN_VIDEOS && !uiState.isLoadingMore) {
            item(
                key = "feed_footer",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                NanzFeedFooter(
                    videoCount = videos.size,
                    onRefresh = onRefresh,
                )
            }
        }
    }
}

@Composable
private fun NetflixHeroBillboard(
    video: Video,
    onPlayClick: () -> Unit,
    onInfoClick: () -> Unit,
    onChannelClick: (String) -> Unit,
    onEnrichChannelMetadata: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(video.id, video.channelId, video.channelThumbnailUrl) {
        onEnrichChannelMetadata(video)
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(350.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                ElectricBlue.copy(alpha = 0.5f),
                                Color.Transparent,
                            ),
                        ),
                    ),
                    RoundedCornerShape(16.dp),
                )
                .clickable(onClick = onPlayClick)
                .testTag("home_hero_billboard"),
    ) {
        // Thumbnail Backdrop
        VideoThumbnailImage(
            videoId = video.id,
            model = video.thumbnailUrl,
            contentDescription = video.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // Dark Netflix Gradient Scrim
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.25f),
                            0.35f to Color.Black.copy(alpha = 0.15f),
                            0.65f to Color.Black.copy(alpha = 0.75f),
                            1.0f to Color.Black.copy(alpha = 0.98f),
                        ),
                    ),
        )

        // Top Badges
        Row(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ElectricBlue,
                ) {
                    Text(
                        text = "NANZ EXCLUSIVE",
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                fontSize = 9.sp,
                            ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(0.5.dp, NeonCyanBlue.copy(alpha = 0.7f)),
                ) {
                    Text(
                        text = "4K ULTRA HD",
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                            ),
                        color = NeonCyanBlue,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }

            if (video.duration > 0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                ) {
                    Text(
                        text = formatDuration(video.duration),
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                            ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
            }
        }

        // Bottom Info and Action Buttons
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            // Title
            Text(
                text = video.title,
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                        lineHeight = 23.sp,
                    ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Metadata: Channel name and views
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!video.channelName.isNullOrBlank()) {
                    Text(
                        text = video.channelName.orEmpty(),
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        color = NeonCyanBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier =
                            Modifier.clickable {
                                video.channelId?.let(onChannelClick)
                            },
                    )
                }

                if (video.viewCount > 0L) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                    Text(
                        text = "${formatViewCount(video.viewCount)} x ditonton",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons (Play & Info)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // High contrast white Play Button
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier.weight(1f).height(42.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                        ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.Black,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Putar",
                        style =
                            MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                }

                // Semi-transparent Info Button
                Button(
                    onClick = onInfoClick,
                    modifier = Modifier.weight(1f).height(42.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White,
                        ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Info",
                        style =
                            MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun NetflixHorizontalShelf(
    title: String,
    badge: String? = null,
    videos: List<Video>,
    onVideoClick: (Video) -> Unit,
    onChannelClick: (String) -> Unit,
    onEnrichChannelMetadata: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
    ) {
        // Shelf Header
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.TrendingUp,
                contentDescription = null,
                tint = ElectricBlue,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (badge != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ElectricBlue,
                ) {
                    Text(
                        text = badge,
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 8.sp,
                            ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }
        }

        // Horizontal Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(
                items = videos,
                key = { _, video -> "netflix_shelf_${video.id}" },
            ) { index, video ->
                NetflixShelfCard(
                    video = video,
                    rank = index + 1,
                    onClick = { onVideoClick(video) },
                    onChannelClick = onChannelClick,
                    onEnrichChannelMetadata = onEnrichChannelMetadata,
                )
            }
        }
    }
}

@Composable
private fun NetflixShelfCard(
    video: Video,
    rank: Int,
    onClick: () -> Unit,
    onChannelClick: (String) -> Unit,
    onEnrichChannelMetadata: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(video.id, video.channelId, video.channelThumbnailUrl) {
        onEnrichChannelMetadata(video)
    }

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier =
            modifier
                .width(200.dp)
                .pressScale(interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
    ) {
        // Thumbnail with 16:9 aspect ratio and rank badge
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF101726))
                    .border(
                        BorderStroke(0.5.dp, Color(0xFF1E293B)),
                        RoundedCornerShape(10.dp),
                    ),
        ) {
            VideoThumbnailImage(
                videoId = video.id,
                model = video.thumbnailUrl,
                contentDescription = video.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            // Top-left Rank Badge (#1, #2, etc.)
            Surface(
                shape = RoundedCornerShape(bottomEnd = 8.dp),
                color = ElectricBlue,
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Text(
                    text = "#$rank",
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                        ),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }

            // Duration badge at bottom-right
            if (video.duration > 0) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp),
                ) {
                    Text(
                        text = formatDuration(video.duration),
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                            ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title
        Text(
            text = video.title,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Channel Name
        if (!video.channelName.isNullOrBlank()) {
            Text(
                text = video.channelName.orEmpty(),
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier.clickable {
                        video.channelId?.let(onChannelClick)
                    },
            )
        }
    }
}

@Composable
private fun NetflixSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(width = 4.dp, height = 16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ElectricBlue),
            )
            Text(
                text = title,
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 10.dp, top = 2.dp),
            )
        }
    }
}

private fun LazyGridScope.feedVideos(
    videos: List<Video>,
    isListView: Boolean,
    onVideoClick: (Video) -> Unit,
    onChannelClick: (String) -> Unit,
    onEnrichChannelMetadata: (Video) -> Unit,
) {
    items(
        items = videos,
        key = { it.id },
    ) { video ->
        HomeFeedVideoItem(
            video = video,
            isListView = isListView,
            onVideoClick = onVideoClick,
            onChannelClick = onChannelClick,
            onEnrichChannelMetadata = onEnrichChannelMetadata,
        )
    }
}

@Composable
private fun LazyGridItemScope.HomeFeedVideoItem(
    video: Video,
    isListView: Boolean,
    onVideoClick: (Video) -> Unit,
    onChannelClick: (String) -> Unit,
    onEnrichChannelMetadata: (Video) -> Unit,
) {
    LaunchedEffect(video.id, video.channelId, video.channelThumbnailUrl) {
        onEnrichChannelMetadata(video)
    }
    if (isListView) {
        VideoCardHorizontal(
            video = video,
            onClick = { onVideoClick(video) },
            onChannelClick = onChannelClick,
            modifier = Modifier.testTag("home_video_card"),
        )
    } else {
        VideoCardFullWidth(
            video = video,
            onClick = { onVideoClick(video) },
            onChannelClick = onChannelClick,
            useInternalPadding = false,
            modifier = Modifier.testTag("home_video_card"),
        )
    }
}
