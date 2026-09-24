package com.fathtube.app.ui.screens.home

import com.fathtube.app.data.local.VideoHistoryEntry
import com.fathtube.app.data.model.Video

data class HomeUiState(
    val videos: List<Video> = emptyList(),
    val shorts: List<Video> = emptyList(),
    val continueWatchingVideos: List<VideoHistoryEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasMorePages: Boolean = true,
    val error: String? = null,
    val isFlowFeed: Boolean = false,
    val lastRefreshTime: Long = 0L,
)
