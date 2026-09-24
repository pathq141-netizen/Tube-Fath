/*
 * Copyright (C) 2025-2026 Flow | A-EDev
 *
 * This file is part of Flow (https://github.com/A-EDev/Flow).
 */

package com.fathtube.app.ui.components.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fathtube.app.ui.theme.Dimensions
import com.fathtube.app.ui.utils.LocalWindowSizeClass
import com.fathtube.app.ui.utils.isExpandedWidth
import com.fathtube.app.ui.utils.isMediumWidth

private val MinLaneItemWidth = 240.dp
private val MinHeroArtworkSize = 180.dp
private val MaxHeroArtworkSize = 240.dp
private const val HERO_ARTWORK_FRACTION = 0.55f

/**
 * Width available to an adaptive section, measured from the window rather than the display so
 * split-screen and freeform windows lay out like the narrow devices they behave as.
 */
@Composable
fun nanzWindowWidth(): Dp {
    val density = LocalDensity.current
    val width = LocalWindowInfo.current.containerSize.width
    return with(density) { width.toDp() }
}

/**
 * Width of one card in a horizontally scrolling lane of wide items: fills a phone with [peek] of
 * the next card showing, and stops growing at [maxWidth] on wider windows.
 */
@Composable
fun nanzLaneItemWidth(
    maxWidth: Dp,
    peek: Dp = 40.dp,
): Dp = nanzLaneItemWidthFor(nanzWindowWidth(), maxWidth, peek)

fun nanzLaneItemWidthFor(
    windowWidth: Dp,
    maxWidth: Dp,
    peek: Dp,
): Dp = (windowWidth - Dimensions.ContentPaddingHorizontal * 2 - peek).coerceIn(MinLaneItemWidth, maxWidth)

@Composable
fun nanzGridColumns(
    compact: Int,
    medium: Int,
    expanded: Int,
): Int {
    val windowSizeClass = LocalWindowSizeClass.current
    return when {
        windowSizeClass.isExpandedWidth -> expanded
        windowSizeClass.isMediumWidth -> medium
        else -> compact
    }
}

@Composable
fun nanzGridCellWidth(
    columns: Int,
    gap: Dp = Dimensions.ItemSpacing,
): Dp = nanzGridCellWidthFor(nanzWindowWidth(), columns, gap)

fun nanzGridCellWidthFor(
    windowWidth: Dp,
    columns: Int,
    gap: Dp,
): Dp = (windowWidth - Dimensions.ContentPaddingHorizontal * 2 - gap * (columns - 1)) / columns

/**
 * Artwork size for a collection or artist header: over half a phone, capped on wider windows so a
 * tablet header stays a header rather than a poster.
 */
@Composable
fun nanzHeroArtworkSize(): Dp = nanzHeroArtworkSizeFor(nanzWindowWidth())

fun nanzHeroArtworkSizeFor(windowWidth: Dp): Dp = (windowWidth * HERO_ARTWORK_FRACTION).coerceIn(MinHeroArtworkSize, MaxHeroArtworkSize)
