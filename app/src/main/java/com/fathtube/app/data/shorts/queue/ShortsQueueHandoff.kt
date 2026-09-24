package com.fathtube.app.data.shorts.queue

import com.fathtube.app.data.model.ShortVideo
import com.fathtube.app.data.model.Video
import com.fathtube.app.data.model.toShortVideo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortsQueueHandoff
    @Inject
    constructor() {
        private val entries = LinkedHashMap<String, List<ShortVideo>>()
        private var nextToken = 0L

        @Synchronized
        fun offer(items: List<ShortVideo>): String {
            val token = "h${nextToken++}"
            entries[token] = items
            while (entries.size > MAX_ENTRIES) {
                val oldest = entries.keys.firstOrNull() ?: break
                entries.remove(oldest)
            }
            return token
        }

        @Synchronized
        fun peek(token: String): List<ShortVideo>? = entries[token]

        @Synchronized
        fun clear() {
            entries.clear()
        }

        fun sourceForShelf(
            shelf: List<Video>,
            tapped: Video,
        ): ShortsQueueSource {
            val reels = shelf.filter { it.isShort }
            if (reels.none { it.id == tapped.id }) return ShortsQueueSource.SeededFeed(tapped.id)
            return ShortsQueueSource.Snapshot(
                token = offer(reels.map { it.toShortVideo() }),
                startVideoId = tapped.id,
            )
        }

        private companion object {
            const val MAX_ENTRIES = 4
        }
    }
