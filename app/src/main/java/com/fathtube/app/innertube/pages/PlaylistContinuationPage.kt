package com.fathtube.app.innertube.pages

import com.fathtube.app.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
