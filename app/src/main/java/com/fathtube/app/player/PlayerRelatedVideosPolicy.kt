package com.fathtube.app.player

import com.fathtube.app.data.model.Video

object PlayerRelatedVideosPolicy {
    fun select(
        videoId: String,
        primary: List<Video>,
        fallback: List<Video>,
        current: List<Video>,
        shortsEnabled: Boolean = true,
    ): List<Video> =
        sequenceOf(primary, fallback, current)
            .map { candidates -> sanitize(videoId, candidates, shortsEnabled) }
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()

    fun sanitize(
        videoId: String,
        candidates: List<Video>,
        shortsEnabled: Boolean = true,
    ): List<Video> =
        candidates
            .filter { it.id.isNotBlank() && it.id != videoId }
            .filter { shortsEnabled || !it.isShort }
            .distinctBy { it.id }
}
