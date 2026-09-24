package com.fathtube.app.innertube.pages

import com.fathtube.app.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
