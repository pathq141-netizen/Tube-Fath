package com.fathtube.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

data class UpdateInfo(
    val version: String,      // e.g., "v1.2.0"
    val changelog: String,    // The release notes
    val downloadUrl: String,  // Link to the .apk or the release page
    val isNewer: Boolean
)

internal data class ReleaseAsset(
    val name: String,
    val downloadUrl: String
)

object UpdateManager {
    suspend fun checkForUpdate(currentVersionName: String): UpdateInfo? = null

    // Helper to open browser
    fun triggerDownload(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("UpdateManager", "Could not open browser", e)
        }
    }
}