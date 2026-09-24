package com.fathtube.app.service

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSessionService
import com.fathtube.app.player.error.PlayerDiagnostics
import com.fathtube.app.utils.NanzCrashHandler

@UnstableApi
internal fun MediaSessionService.recordForegroundStartFailures(tag: String) {
    setListener(
        object : MediaSessionService.Listener {
            @RequiresApi(Build.VERSION_CODES.S)
            override fun onForegroundServiceStartNotAllowedException() {
                NanzCrashHandler.recordPhase(tag, "foreground start refused by platform")
                PlayerDiagnostics.logWarning(
                    tag,
                    "startForegroundService refused: app is in the background without an " +
                        "FGS-start allowance; playback notification stays non-foreground",
                )
            }
        },
    )
}
