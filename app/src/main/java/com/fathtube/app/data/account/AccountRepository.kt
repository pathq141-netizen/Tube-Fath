package com.fathtube.app.data.account

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import com.fathtube.app.data.local.ChannelSubscription
import com.fathtube.app.data.local.LikedVideoInfo
import com.fathtube.app.data.local.LikedVideosRepository
import com.fathtube.app.data.local.PlaylistRepository
import com.fathtube.app.data.local.SubscriptionRepository
import com.fathtube.app.data.model.Video
import com.fathtube.app.innertube.YouTube
import com.fathtube.app.innertube.models.AccountInfo
import com.fathtube.app.innertube.models.YouTubeClient
import com.fathtube.app.innertube.pages.videoCommentsContinuation
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class StoredSession(
    val cookie: String,
    val accountInfo: AccountInfo?,
    val savedAt: Long = System.currentTimeMillis(),
)

data class SyncedPlaylistInfo(
    val playlistId: String,
    val title: String,
    val thumbnailUrl: String,
)

data class AccountState(
    val isLoggedIn: Boolean = false,
    val accountInfo: AccountInfo? = null,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
)

class AccountRepository private constructor(
    private val context: Context,
    private val subscriptionRepository: SubscriptionRepository,
) {
    companion object {
        private const val TAG = "AccountRepository"
        private const val PREFS_NAME = "nanztube_account_prefs"
        private const val KEY_AUTH_COOKIE = "auth_cookie"
        private const val KEY_NAME = "account_name"
        private const val KEY_EMAIL = "account_email"
        private const val KEY_HANDLE = "account_handle"
        private const val KEY_AVATAR = "account_avatar"
        private const val KEY_EXPLICIT_LOGOUT = "explicit_logout"
        const val SESSION_JSON_FILENAME = "nanztube_session.json"

        @Volatile
        private var INSTANCE: AccountRepository? = null

        fun getInstance(
            context: Context,
            subscriptionRepository: SubscriptionRepository? = null,
        ): AccountRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AccountRepository(
                    context.applicationContext,
                    subscriptionRepository ?: SubscriptionRepository.getInstance(context.applicationContext),
                ).also { INSTANCE = it }
            }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _accountState = MutableStateFlow(AccountState())
    val accountState: StateFlow<AccountState> = _accountState.asStateFlow()

    init {
        restoreSession()
    }

    /**
     * Memulihkan sesi login.
     * Prioritas utama membaca file JSON (nanztube_session.json).
     * Selama file JSON tersebut masih tersimpan di penyimpanan, login tidak akan hilang.
     */
    fun restoreSession(force: Boolean = false): Boolean {
        if (!force && prefs.getBoolean(KEY_EXPLICIT_LOGOUT, false)) {
            Log.d(TAG, "Skipping auto-restore: explicit logout was previously performed")
            return false
        }

        // 1. Prioritaskan pembacaan dari file JSON nanztube_session.json
        val jsonSession = loadSessionFromJson()
        if (jsonSession != null && jsonSession.cookie.isNotBlank()) {
            prefs.edit().putBoolean(KEY_EXPLICIT_LOGOUT, false).apply()
            YouTube.cookie = jsonSession.cookie
            YouTube.useLoginForBrowse = true

            // Sinkronkan ke SharedPreferences agar konsisten
            prefs.edit()
                .putString(KEY_AUTH_COOKIE, jsonSession.cookie)
                .apply {
                    jsonSession.accountInfo?.let { info ->
                        putString(KEY_NAME, info.name)
                        putString(KEY_EMAIL, info.email)
                        putString(KEY_HANDLE, info.channelHandle)
                        putString(KEY_AVATAR, info.thumbnailUrl)
                    }
                }.apply()

            _accountState.value = AccountState(
                isLoggedIn = true,
                accountInfo = jsonSession.accountInfo,
            )

            scope.launch {
                refreshAccountInfo()
            }
            return true
        }

        // 2. Fallback: jika JSON belum ada, cek SharedPreferences
        val savedCookie = prefs.getString(KEY_AUTH_COOKIE, null)
        if (!savedCookie.isNullOrBlank()) {
            prefs.edit().putBoolean(KEY_EXPLICIT_LOGOUT, false).apply()
            YouTube.cookie = savedCookie
            YouTube.useLoginForBrowse = true

            val savedName = prefs.getString(KEY_NAME, null)
            val savedEmail = prefs.getString(KEY_EMAIL, null)
            val savedHandle = prefs.getString(KEY_HANDLE, null)
            val savedAvatar = prefs.getString(KEY_AVATAR, null)

            val cachedInfo = if (savedName != null) {
                AccountInfo(
                    name = savedName,
                    email = savedEmail,
                    channelHandle = savedHandle,
                    thumbnailUrl = savedAvatar,
                )
            } else null

            _accountState.value = AccountState(
                isLoggedIn = true,
                accountInfo = cachedInfo,
            )

            // Simpan otomatis ke file JSON agar persistensi JSON aktif
            saveSessionToJson(savedCookie, cachedInfo)

            scope.launch {
                refreshAccountInfo()
            }
            return true
        }
        return false
    }

    fun onLoginSuccess(cookie: String, initialInfo: AccountInfo? = null) {
        prefs.edit()
            .putBoolean(KEY_EXPLICIT_LOGOUT, false)
            .putString(KEY_AUTH_COOKIE, cookie)
            .apply()
        YouTube.cookie = cookie
        YouTube.useLoginForBrowse = true

        // Simpan sesi login ke file JSON secara instan
        saveSessionToJson(cookie, initialInfo)

        if (initialInfo != null) {
            prefs.edit()
                .putString(KEY_NAME, initialInfo.name)
                .putString(KEY_EMAIL, initialInfo.email)
                .putString(KEY_HANDLE, initialInfo.channelHandle)
                .putString(KEY_AVATAR, initialInfo.thumbnailUrl)
                .apply()
            _accountState.value = _accountState.value.copy(
                isLoggedIn = true,
                accountInfo = initialInfo,
            )
        } else {
            _accountState.value = _accountState.value.copy(isLoggedIn = true)
        }

        scope.launch {
            refreshAccountInfo()
            syncSubscriptions()
            syncLikedVideos()
        }
    }

    fun logout(deleteJsonFile: Boolean = false) {
        prefs.edit().clear().putBoolean(KEY_EXPLICIT_LOGOUT, true).apply()
        YouTube.cookie = null
        YouTube.useLoginForBrowse = false

        if (deleteJsonFile) {
            deleteSessionJsonFiles()
        }

        runCatching {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }

        _accountState.value = AccountState(
            isLoggedIn = false,
            accountInfo = null,
            isSyncing = false,
            syncMessage = null,
        )
    }

    suspend fun refreshAccountInfo() {
        if (YouTube.cookie.isNullOrBlank()) return
        withContext(Dispatchers.IO) {
            YouTube.accountInfo().onSuccess { info ->
                prefs.edit()
                    .putString(KEY_NAME, info.name)
                    .putString(KEY_EMAIL, info.email)
                    .putString(KEY_HANDLE, info.channelHandle)
                    .putString(KEY_AVATAR, info.thumbnailUrl)
                    .apply()

                // Perbarui data nama dan avatar di file JSON
                YouTube.cookie?.let { currentCookie ->
                    saveSessionToJson(currentCookie, info)
                }

                _accountState.value = _accountState.value.copy(
                    isLoggedIn = true,
                    accountInfo = info,
                )
            }.onFailure { error ->
                Log.w(TAG, "Failed to refresh account info: ${error.message}")
            }
        }
    }

    // =========================================================================
    // 📁 JSON SESSION PERSISTENCE, EXPORT & IMPORT
    // =========================================================================

    /**
     * Menyimpan data sesi login ke nanztube_session.json (internal dan external).
     */
    fun saveSessionToJson(cookie: String, info: AccountInfo?) {
        try {
            val root = buildJsonObject {
                put("app", "FathTube")
                put("version", 1)
                put("cookie", cookie)
                put("savedAt", System.currentTimeMillis())
                put("name", info?.name.orEmpty())
                put("email", info?.email.orEmpty())
                put("channelHandle", info?.channelHandle.orEmpty())
                put("thumbnailUrl", info?.thumbnailUrl.orEmpty())
            }
            val jsonString = Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), root)

            // 1. Simpan di internal storage: files/nanztube_session.json
            val internalFile = File(context.filesDir, SESSION_JSON_FILENAME)
            internalFile.writeText(jsonString)

            // 2. Simpan di noBackupFilesDir agar aman dari pembersihan otomatis
            runCatching {
                File(context.noBackupFilesDir, SESSION_JSON_FILENAME).writeText(jsonString)
            }

            // 3. Simpan juga di external files dir jika tersedia
            runCatching {
                context.getExternalFilesDir(null)?.let { extDir ->
                    val extFile = File(extDir, SESSION_JSON_FILENAME)
                    extFile.writeText(jsonString)
                }
            }
            Log.i(TAG, "Login session successfully saved to $SESSION_JSON_FILENAME")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save session JSON: ${e.message}")
        }
    }

    /**
     * Memuat sesi login dari file JSON nanztube_session.json.
     */
    fun loadSessionFromJson(): StoredSession? {
        val candidateFiles = listOfNotNull(
            File(context.filesDir, SESSION_JSON_FILENAME),
            File(context.noBackupFilesDir, SESSION_JSON_FILENAME),
            context.getExternalFilesDir(null)?.let { File(it, SESSION_JSON_FILENAME) },
            File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), SESSION_JSON_FILENAME)
        )

        for (file in candidateFiles) {
            if (file.exists() && file.isFile) {
                try {
                    val text = file.readText()
                    val parsed = parseSessionJsonString(text)
                    if (parsed != null && parsed.cookie.isNotBlank()) {
                        Log.i(TAG, "Loaded login session from JSON file: ${file.absolutePath}")
                        return parsed
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error reading session file ${file.name}: ${e.message}")
                }
            }
        }
        return null
    }

    /**
     * Melakukan parsing teks JSON menjadi StoredSession.
     */
    fun parseSessionJsonString(text: String): StoredSession? {
        return try {
            val json = Json.parseToJsonElement(text).jsonObject
            val cookie = json["cookie"]?.jsonPrimitive?.contentOrNull
                ?: json["auth_cookie"]?.jsonPrimitive?.contentOrNull
                ?: return null
            if (cookie.isBlank()) return null

            val name = json["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: json["account_name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            val email = json["email"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: json["account_email"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            val handle = json["channelHandle"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: json["account_handle"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            val avatar = json["thumbnailUrl"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: json["account_avatar"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

            val info = if (name != null || avatar != null) {
                AccountInfo(
                    name = name ?: "Pengguna YouTube",
                    email = email,
                    channelHandle = handle,
                    thumbnailUrl = avatar,
                )
            } else null

            val savedAt = json["savedAt"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis()
            StoredSession(cookie = cookie, accountInfo = info, savedAt = savedAt)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse session JSON: ${e.message}")
            null
        }
    }

    /**
     * Mengembalikan string JSON sesi aktif untuk disalin / diekspor.
     */
    fun exportSessionJson(): String? {
        val cookie = YouTube.cookie ?: prefs.getString(KEY_AUTH_COOKIE, null) ?: return null
        val info = _accountState.value.accountInfo ?: run {
            val name = prefs.getString(KEY_NAME, null)
            if (name != null) {
                AccountInfo(
                    name = name,
                    email = prefs.getString(KEY_EMAIL, null),
                    channelHandle = prefs.getString(KEY_HANDLE, null),
                    thumbnailUrl = prefs.getString(KEY_AVATAR, null),
                )
            } else null
        }
        val root = buildJsonObject {
            put("app", "FathTube")
            put("version", 1)
            put("cookie", cookie)
            put("savedAt", System.currentTimeMillis())
            put("name", info?.name.orEmpty())
            put("email", info?.email.orEmpty())
            put("channelHandle", info?.channelHandle.orEmpty())
            put("thumbnailUrl", info?.thumbnailUrl.orEmpty())
        }
        return Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), root)
    }

    /**
     * Mengimpor sesi login dari string JSON dan langsung mengaktifkan akun.
     */
    fun importSessionJson(jsonStr: String): Result<AccountInfo?> {
        val parsed = parseSessionJsonString(jsonStr)
            ?: return Result.failure(IllegalArgumentException("Format JSON sesi tidak valid atau tidak memiliki cookie YouTube"))
        onLoginSuccess(parsed.cookie, parsed.accountInfo)
        return Result.success(parsed.accountInfo)
    }

    /**
     * Mengekspor file nanztube_session.json ke folder Unduhan (Downloads).
     */
    fun exportSessionToDownloads(): String? {
        return try {
            val json = exportSessionJson() ?: return null
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir.exists() || downloadsDir.mkdirs()) {
                val target = File(downloadsDir, SESSION_JSON_FILENAME)
                target.writeText(json)
                target.absolutePath
            } else {
                val internal = File(context.filesDir, SESSION_JSON_FILENAME)
                internal.writeText(json)
                internal.absolutePath
            }
        } catch (e: Exception) {
            val internal = File(context.filesDir, SESSION_JSON_FILENAME)
            internal.writeText(exportSessionJson() ?: return null)
            internal.absolutePath
        }
    }

    fun isSessionJsonSaved(): Boolean {
        return loadSessionFromJson() != null
    }

    fun getSessionFilePath(): String {
        val internalFile = File(context.filesDir, SESSION_JSON_FILENAME)
        return internalFile.absolutePath
    }

    private fun deleteSessionJsonFiles() {
        runCatching { File(context.filesDir, SESSION_JSON_FILENAME).delete() }
        runCatching { File(context.noBackupFilesDir, SESSION_JSON_FILENAME).delete() }
        runCatching { context.getExternalFilesDir(null)?.let { File(it, SESSION_JSON_FILENAME).delete() } }
    }

    fun syncSubscriptions() {
        if (YouTube.cookie.isNullOrBlank()) return
        scope.launch {
            _accountState.value = _accountState.value.copy(
                isSyncing = true,
                syncMessage = "Menyinkronkan data YouTube...",
            )
            try {
                val channels = mutableListOf<ChannelSubscription>()

                // 1. Primary: FEchannels (daftar channel langganan di YouTube Web)
                runCatching {
                    val response = YouTube.innerTube.browse(
                        client = YouTubeClient.WEB,
                        browseId = "FEchannels",
                        setLogin = true,
                    )
                    channels.addAll(parseChannelsFromBrowseJson(response.bodyAsText()))
                }

                // 2. Secondary: FEsubscriptions
                if (channels.isEmpty()) {
                    runCatching {
                        val response = YouTube.innerTube.browse(
                            client = YouTubeClient.WEB,
                            browseId = "FEsubscriptions",
                            setLogin = true,
                        )
                        channels.addAll(parseChannelsFromBrowseJson(response.bodyAsText()))
                    }
                }

                // 3. Fallback: FElibrary (memuat sidebar guide subscriptions)
                runCatching {
                    val response = YouTube.innerTube.browse(
                        client = YouTubeClient.WEB,
                        browseId = "FElibrary",
                        setLogin = true,
                    )
                    channels.addAll(parseChannelsFromBrowseJson(response.bodyAsText()))
                }

                val distinctChannels = channels.distinctBy { it.channelId }
                if (distinctChannels.isNotEmpty()) {
                    subscriptionRepository.subscribeAll(distinctChannels)
                }

                syncLikedVideosInternal()
                syncPlaylistsInternal()

                val msg = if (distinctChannels.isNotEmpty()) {
                    "Berhasil menyinkronkan ${distinctChannels.size} channel langganan, video like & playlist YouTube!"
                } else {
                    "Sinkronisasi data & playlist YouTube selesai."
                }
                _accountState.value = _accountState.value.copy(
                    isSyncing = false,
                    syncMessage = msg,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync subscriptions", e)
                _accountState.value = _accountState.value.copy(
                    isSyncing = false,
                    syncMessage = "Gagal sinkronisasi: ${e.localizedMessage ?: "Jaringan bermasalah"}",
                )
            }
        }
    }

    fun syncLikedVideos() {
        scope.launch {
            syncLikedVideosInternal()
        }
    }

    fun syncPlaylists() {
        scope.launch {
            syncPlaylistsInternal()
        }
    }

    private suspend fun syncLikedVideosInternal() {
        if (YouTube.cookie.isNullOrBlank()) return
        try {
            val likedRepo = LikedVideosRepository.getInstance(context)
            val response = YouTube.innerTube.browse(
                client = YouTubeClient.WEB,
                browseId = "VL" + "LL",
                setLogin = true,
            )
            val responseText = response.bodyAsText()
            val json = Json.parseToJsonElement(responseText)
            val likedVideos = mutableListOf<LikedVideoInfo>()
            findPlaylistVideoRenderers(json, likedVideos)
            if (likedVideos.isNotEmpty()) {
                likedRepo.importLikedVideos(likedVideos)
                Log.i(TAG, "Successfully synced ${likedVideos.size} liked videos from YouTube")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync liked videos: ${e.message}")
        }
    }

    private suspend fun syncPlaylistsInternal() {
        if (YouTube.cookie.isNullOrBlank()) return
        try {
            val playlistRepo = PlaylistRepository(context)
            val playlists = mutableListOf<SyncedPlaylistInfo>()

            // 1. Fetch user playlists from FEplaylists
            runCatching {
                val res = YouTube.innerTube.browse(
                    client = YouTubeClient.WEB,
                    browseId = "FEplaylists",
                    setLogin = true,
                )
                val json = Json.parseToJsonElement(res.bodyAsText())
                findPlaylistRenderers(json, playlists)
            }

            // 2. Fetch user playlists from FEplaylist_aggregation
            runCatching {
                val res = YouTube.innerTube.browse(
                    client = YouTubeClient.WEB,
                    browseId = "FEplaylist_aggregation",
                    setLogin = true,
                )
                val json = Json.parseToJsonElement(res.bodyAsText())
                findPlaylistRenderers(json, playlists)
            }

            // 3. Fallback or addition from FElibrary
            runCatching {
                val res = YouTube.innerTube.browse(
                    client = YouTubeClient.WEB,
                    browseId = "FElibrary",
                    setLogin = true,
                )
                val json = Json.parseToJsonElement(res.bodyAsText())
                findPlaylistRenderers(json, playlists)
            }

            // 4. Save each playlist and import its videos
            val uniquePlaylists = playlists.distinctBy { it.playlistId }.filter {
                it.playlistId.isNotBlank() && it.playlistId != "WL" && it.playlistId != "LL"
            }
            for (p in uniquePlaylists) {
                playlistRepo.saveExternalVideoPlaylist(
                    id = p.playlistId,
                    name = p.title,
                    description = "Playlist YouTube",
                    thumbnailUrl = p.thumbnailUrl,
                )
                runCatching {
                    val pRes = YouTube.innerTube.browse(
                        client = YouTubeClient.WEB,
                        browseId = "VL" + p.playlistId,
                        setLogin = true,
                    )
                    val pJson = Json.parseToJsonElement(pRes.bodyAsText())
                    val pVideos = mutableListOf<LikedVideoInfo>()
                    findPlaylistVideoRenderers(pJson, pVideos)
                    if (pVideos.isNotEmpty()) {
                        val domainVideos = pVideos.map { lv ->
                            Video(
                                id = lv.videoId,
                                title = lv.title,
                                channelName = lv.channelName,
                                channelId = "",
                                thumbnailUrl = lv.thumbnail,
                                duration = 0,
                                viewCount = 0L,
                                uploadDate = "",
                            )
                        }
                        playlistRepo.addVideosToPlaylist(p.playlistId, domainVideos)
                    }
                }
            }

            // 5. Sync Watch Later (VLWL)
            runCatching {
                val wlRes = YouTube.innerTube.browse(
                    client = YouTubeClient.WEB,
                    browseId = "VLWL",
                    setLogin = true,
                )
                val wlJson = Json.parseToJsonElement(wlRes.bodyAsText())
                val wlVideos = mutableListOf<LikedVideoInfo>()
                findPlaylistVideoRenderers(wlJson, wlVideos)
                if (wlVideos.isNotEmpty()) {
                    for (v in wlVideos) {
                        playlistRepo.addToWatchLater(
                            Video(
                                id = v.videoId,
                                title = v.title,
                                channelName = v.channelName,
                                channelId = "",
                                thumbnailUrl = v.thumbnail,
                                duration = 0,
                                viewCount = 0L,
                                uploadDate = "",
                            )
                        )
                    }
                }
            }
            Log.i(TAG, "Successfully synced ${uniquePlaylists.size} playlists from YouTube")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync playlists: ${e.message}")
        }
    }

    suspend fun likeVideo(videoId: String, like: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (YouTube.cookie.isNullOrBlank()) {
                Result.failure(IllegalStateException("Belum login ke akun YouTube"))
            } else {
                YouTube.likeVideo(videoId, like).map { }
            }
        }

    suspend fun subscribeChannel(channelId: String, subscribe: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (YouTube.cookie.isNullOrBlank()) {
                Result.failure(IllegalStateException("Belum login ke akun YouTube"))
            } else {
                YouTube.subscribeChannel(channelId, subscribe).map { }
            }
        }

    private val commentParamsCache = ConcurrentHashMap<String, String>()

    fun setCreateCommentParams(videoId: String, params: String) {
        if (params.isNotBlank()) {
            commentParamsCache[videoId] = params
        }
    }

    suspend fun getOrFetchCreateCommentParams(videoId: String): String? {
        commentParamsCache[videoId]?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val response = YouTube.innerTube.nextWatch(videoId = videoId, setLogin = true)
                val bodyText = response.bodyAsText()
                val json = Json.parseToJsonElement(bodyText)
                val directParams = findCreateCommentParamsFromJson(json)
                if (!directParams.isNullOrBlank()) {
                    commentParamsCache[videoId] = directParams
                    return@withContext directParams
                }
                val continuationToken = json.videoCommentsContinuation()
                if (!continuationToken.isNullOrBlank()) {
                    val commentsRes = YouTube.innerTube.nextWatch(continuation = continuationToken, setLogin = true)
                    val commentsJson = Json.parseToJsonElement(commentsRes.bodyAsText())
                    val params = findCreateCommentParamsFromJson(commentsJson)
                    if (!params.isNullOrBlank()) {
                        commentParamsCache[videoId] = params
                        return@withContext params
                    }
                }
                null
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resolve createCommentParams for $videoId: ${e.message}")
                null
            }
        }
    }

    suspend fun postComment(videoId: String, commentText: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (YouTube.cookie.isNullOrBlank()) {
                return@withContext Result.failure(IllegalStateException("Belum login ke akun Google / YouTube"))
            }
            val params = getOrFetchCreateCommentParams(videoId)
            if (params.isNullOrBlank()) {
                return@withContext Result.failure(IllegalStateException("Gagal memuat token komentar untuk video ini"))
            }
            YouTube.createComment(commentText, params).map { }
        }

    suspend fun postCommentWithParams(commentText: String, createCommentParams: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (YouTube.cookie.isNullOrBlank()) {
                Result.failure(IllegalStateException("Belum login ke akun YouTube"))
            } else {
                YouTube.createComment(commentText, createCommentParams).map { }
            }
        }

    private fun findCreateCommentParamsFromJson(element: JsonElement): String? {
        when (element) {
            is JsonObject -> {
                element["createCommentParams"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let { return it }
                for ((_, child) in element) {
                    val found = findCreateCommentParamsFromJson(child)
                    if (found != null) return found
                }
            }
            is JsonArray -> {
                for (child in element) {
                    val found = findCreateCommentParamsFromJson(child)
                    if (found != null) return found
                }
            }
            else -> Unit
        }
        return null
    }

    private fun findPlaylistVideoRenderers(element: JsonElement, output: MutableList<LikedVideoInfo>) {
        when (element) {
            is JsonObject -> {
                val pvr = element["playlistVideoRenderer"]?.jsonObject
                if (pvr != null) {
                    val videoId = pvr["videoId"]?.jsonPrimitive?.contentOrNull
                    val titleObj = pvr["title"]?.jsonObject
                    val title = titleObj?.get("simpleText")?.jsonPrimitive?.contentOrNull
                        ?: titleObj?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                    val channelObj = pvr["shortBylineText"]?.jsonObject
                    val channelName = channelObj?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                        ?: channelObj?.get("simpleText")?.jsonPrimitive?.contentOrNull
                    val thumbObj = pvr["thumbnail"]?.jsonObject
                    val thumbnailUrl = thumbObj?.get("thumbnails")?.jsonArray?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull

                    if (!videoId.isNullOrBlank() && !title.isNullOrBlank()) {
                        val cleanThumb = if (thumbnailUrl?.startsWith("//") == true) "https:$thumbnailUrl" else (thumbnailUrl ?: "")
                        output.add(
                            LikedVideoInfo(
                                videoId = videoId,
                                title = title,
                                thumbnail = cleanThumb,
                                channelName = channelName.orEmpty(),
                            )
                        )
                    }
                }
                for ((_, child) in element) {
                    findPlaylistVideoRenderers(child, output)
                }
            }
            is JsonArray -> {
                for (child in element) {
                    findPlaylistVideoRenderers(child, output)
                }
            }
            else -> Unit
        }
    }

    private fun findPlaylistRenderers(element: JsonElement, output: MutableList<SyncedPlaylistInfo>) {
        when (element) {
            is JsonObject -> {
                val pr = element["playlistRenderer"]?.jsonObject
                    ?: element["gridPlaylistRenderer"]?.jsonObject
                    ?: element["compactPlaylistRenderer"]?.jsonObject

                if (pr != null) {
                    val rawPlaylistId = pr["playlistId"]?.jsonPrimitive?.contentOrNull
                        ?: pr["navigationEndpoint"]?.jsonObject
                            ?.get("watchEndpoint")?.jsonObject
                            ?.get("playlistId")?.jsonPrimitive?.contentOrNull
                        ?: pr["navigationEndpoint"]?.jsonObject
                            ?.get("browseEndpoint")?.jsonObject
                            ?.get("browseId")?.jsonPrimitive?.contentOrNull
                            ?.removePrefix("VL")

                    val titleObj = pr["title"]?.jsonObject
                    val title = pr["title"]?.jsonPrimitive?.contentOrNull
                        ?: titleObj?.get("simpleText")?.jsonPrimitive?.contentOrNull
                        ?: titleObj?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull

                    val thumbObj = pr["thumbnail"]?.jsonObject
                        ?: pr["thumbnailRenderer"]?.jsonObject?.get("playlistVideoThumbnailRenderer")?.jsonObject?.get("thumbnail")?.jsonObject
                        ?: pr["thumbnailRenderer"]?.jsonObject?.get("playlistCustomThumbnailRenderer")?.jsonObject?.get("thumbnail")?.jsonObject

                    val thumbnailUrl = thumbObj?.get("thumbnails")?.jsonArray?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull

                    if (!rawPlaylistId.isNullOrBlank() && !title.isNullOrBlank()) {
                        val cleanThumb = if (thumbnailUrl?.startsWith("//") == true) "https:$thumbnailUrl" else (thumbnailUrl ?: "")
                        output.add(
                            SyncedPlaylistInfo(
                                playlistId = rawPlaylistId,
                                title = title,
                                thumbnailUrl = cleanThumb,
                            )
                        )
                    }
                }

                // Support newer YouTube lockupViewModel
                val lockup = element["lockupViewModel"]?.jsonObject
                if (lockup != null) {
                    var contentId = lockup["contentId"]?.jsonPrimitive?.contentOrNull
                    if (contentId.isNullOrBlank()) {
                        contentId = lockup["rendererContext"]?.jsonObject
                            ?.get("commandContext")?.jsonObject
                            ?.get("onTap")?.jsonObject
                            ?.get("innertubeCommand")?.jsonObject
                            ?.get("browseEndpoint")?.jsonObject
                            ?.get("browseId")?.jsonPrimitive?.contentOrNull
                            ?: lockup["rendererContext"]?.jsonObject
                                ?.get("commandContext")?.jsonObject
                                ?.get("onTap")?.jsonObject
                                ?.get("innertubeCommand")?.jsonObject
                                ?.get("watchEndpoint")?.jsonObject
                                ?.get("playlistId")?.jsonPrimitive?.contentOrNull
                    }
                    val metadata = lockup["metadata"]?.jsonObject?.get("lockupMetadataViewModel")?.jsonObject
                    val title = metadata?.get("title")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
                    val imageSources = lockup["contentImage"]?.jsonObject
                        ?.get("collectionThumbnailViewModel")?.jsonObject
                        ?.get("primaryThumbnail")?.jsonObject
                        ?.get("thumbnailViewModel")?.jsonObject
                        ?.get("image")?.jsonObject
                        ?.get("sources")?.jsonArray
                    val thumbnailUrl = imageSources?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull

                    if (!contentId.isNullOrBlank() && !title.isNullOrBlank()) {
                        val cleanId = contentId.removePrefix("VL")
                        val cleanThumb = if (thumbnailUrl?.startsWith("//") == true) "https:$thumbnailUrl" else (thumbnailUrl ?: "")
                        output.add(
                            SyncedPlaylistInfo(
                                playlistId = cleanId,
                                title = title,
                                thumbnailUrl = cleanThumb,
                            )
                        )
                    }
                }

                // Support guideEntryRenderer for sidebar playlists
                val ge = element["guideEntryRenderer"]?.jsonObject
                if (ge != null) {
                    val browseId = ge["navigationEndpoint"]?.jsonObject
                        ?.get("browseEndpoint")?.jsonObject
                        ?.get("browseId")?.jsonPrimitive?.contentOrNull
                    if (browseId != null && (browseId.startsWith("VLPL") || browseId.startsWith("PL"))) {
                        val cleanId = browseId.removePrefix("VL")
                        val titleObj = ge["title"]?.jsonObject ?: ge["formattedTitle"]?.jsonObject
                        val title = ge["title"]?.jsonPrimitive?.contentOrNull
                            ?: titleObj?.get("simpleText")?.jsonPrimitive?.contentOrNull
                            ?: titleObj?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                        val thumbObj = ge["thumbnail"]?.jsonObject
                        val thumbnailUrl = thumbObj?.get("thumbnails")?.jsonArray?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
                        if (!title.isNullOrBlank()) {
                            val cleanThumb = if (thumbnailUrl?.startsWith("//") == true) "https:$thumbnailUrl" else (thumbnailUrl ?: "")
                            output.add(
                                SyncedPlaylistInfo(
                                    playlistId = cleanId,
                                    title = title,
                                    thumbnailUrl = cleanThumb,
                                )
                            )
                        }
                    }
                }

                for ((_, child) in element) {
                    findPlaylistRenderers(child, output)
                }
            }
            is JsonArray -> {
                for (child in element) {
                    findPlaylistRenderers(child, output)
                }
            }
            else -> Unit
        }
    }

    private fun parseChannelsFromBrowseJson(jsonString: String): List<ChannelSubscription> {
        val result = mutableListOf<ChannelSubscription>()
        try {
            val root = Json.parseToJsonElement(jsonString)
            findChannelRenderers(root, result)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing channels JSON", e)
        }
        return result.distinctBy { it.channelId }
    }

    private fun findChannelRenderers(element: JsonElement, output: MutableList<ChannelSubscription>) {
        when (element) {
            is JsonObject -> {
                val cr = element["channelRenderer"]?.jsonObject
                    ?: element["gridChannelRenderer"]?.jsonObject
                    ?: element["channelListItemRenderer"]?.jsonObject
                    ?: element["guideChannelRenderer"]?.jsonObject
                    ?: element["compactChannelRenderer"]?.jsonObject
                    ?: element["guideEntryRenderer"]?.jsonObject
                if (cr != null) {
                    val channelId = cr["channelId"]?.jsonPrimitive?.contentOrNull
                        ?: cr["navigationEndpoint"]?.jsonObject
                            ?.get("browseEndpoint")?.jsonObject
                            ?.get("browseId")?.jsonPrimitive?.contentOrNull
                    val titleObj = cr["title"]?.jsonObject ?: cr["formattedTitle"]?.jsonObject
                    val title = cr["title"]?.jsonPrimitive?.contentOrNull
                        ?: titleObj?.get("simpleText")?.jsonPrimitive?.contentOrNull
                        ?: titleObj?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                    val thumbnailObj = cr["thumbnail"]?.jsonObject
                    val thumbnailUrl = thumbnailObj?.get("thumbnails")?.jsonArray?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull

                    if (!channelId.isNullOrBlank() && !title.isNullOrBlank() && (channelId.startsWith("UC") || cr.containsKey("channelId"))) {
                        val cleanThumb = if (thumbnailUrl?.startsWith("//") == true) {
                            "https:$thumbnailUrl"
                        } else {
                            thumbnailUrl ?: ""
                        }
                        output.add(
                            ChannelSubscription(
                                channelId = channelId,
                                channelName = title,
                                channelThumbnail = cleanThumb,
                            )
                        )
                    }
                }

                // Support lockupViewModel for channels
                val lockup = element["lockupViewModel"]?.jsonObject
                if (lockup != null) {
                    val contentId = lockup["contentId"]?.jsonPrimitive?.contentOrNull
                    if (contentId != null && contentId.startsWith("UC")) {
                        val metadata = lockup["metadata"]?.jsonObject?.get("lockupMetadataViewModel")?.jsonObject
                        val title = metadata?.get("title")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
                        val imageSources = lockup["contentImage"]?.jsonObject
                            ?.get("decoratedAvatarViewModel")?.jsonObject
                            ?.get("avatar")?.jsonObject
                            ?.get("avatarViewModel")?.jsonObject
                            ?.get("image")?.jsonObject
                            ?.get("sources")?.jsonArray
                            ?: lockup["contentImage"]?.jsonObject
                                ?.get("collectionThumbnailViewModel")?.jsonObject
                                ?.get("primaryThumbnail")?.jsonObject
                                ?.get("thumbnailViewModel")?.jsonObject
                                ?.get("image")?.jsonObject
                                ?.get("sources")?.jsonArray
                        val thumbUrl = imageSources?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
                        if (!title.isNullOrBlank()) {
                            val cleanThumb = if (thumbUrl?.startsWith("//") == true) "https:$thumbUrl" else (thumbUrl ?: "")
                            output.add(
                                ChannelSubscription(
                                    channelId = contentId,
                                    channelName = title,
                                    channelThumbnail = cleanThumb,
                                )
                            )
                        }
                    }
                }

                for ((_, child) in element) {
                    findChannelRenderers(child, output)
                }
            }
            is JsonArray -> {
                for (child in element) {
                    findChannelRenderers(child, output)
                }
            }
            else -> Unit
        }
    }
}
