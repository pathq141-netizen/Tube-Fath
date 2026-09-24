package com.fathtube.app.innertube.pages

import com.fathtube.app.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
