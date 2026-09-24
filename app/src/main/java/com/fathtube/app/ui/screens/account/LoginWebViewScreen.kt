package com.fathtube.app.ui.screens.account

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.fathtube.app.data.account.AccountRepository
import com.fathtube.app.innertube.models.AccountInfo
import org.json.JSONObject
import org.json.JSONTokener

private const val GOOGLE_LOGIN_URL =
    "https://accounts.google.com/ServiceLogin?service=youtube&uilel=3&continue=https%3A%2F%2Fm.youtube.com%2F"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginWebViewScreen(
    onLoginSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var isVerifying by remember { mutableStateOf(false) }
    var showImportJsonDialog by remember { mutableStateOf(false) }
    var inputJsonText by remember { mutableStateOf("") }

    fun checkAndSaveLogin(currentUrl: String? = null, onCompleted: (() -> Unit)? = null): Boolean {
        val cookieManager = CookieManager.getInstance()
        val domains = listOfNotNull(
            "https://www.youtube.com",
            "https://m.youtube.com",
            "https://youtube.com",
            ".youtube.com",
            "https://accounts.google.com",
            ".google.com",
            currentUrl,
        )

        val mergedCookies = mutableMapOf<String, String>()
        for (u in domains) {
            val c = cookieManager.getCookie(u)
            if (!c.isNullOrBlank()) {
                mergedCookies.putAll(com.fathtube.app.innertube.utils.parseCookieString(c))
            }
        }

        val hasAuth = mergedCookies.containsKey("SAPISID") ||
            mergedCookies.containsKey("__Secure-3PAPISID") ||
            mergedCookies.containsKey("__Secure-1PAPISID") ||
            mergedCookies.containsKey("LOGIN_INFO") ||
            mergedCookies.containsKey("SSID")

        if (hasAuth) {
            val combinedCookie = mergedCookies.map { "${it.key}=${it.value}" }.joinToString("; ")
            webViewInstance?.evaluateJavascript(
                """
                (function() {
                    try {
                        var name = (window.ytcfg && window.ytcfg.get('USER_DISPLAY_NAME')) || '';
                        var email = (window.ytcfg && window.ytcfg.get('USER_EMAIL')) || '';
                        var avatar = (window.ytcfg && window.ytcfg.get('USER_AVATAR_URL')) || '';
                        var img = document.querySelector('button#avatar-btn img') || document.querySelector('img.yt-spec-avatar-shape__image') || document.querySelector('ytm-topbar-logo + * img');
                        if (!avatar && img) avatar = img.src;
                        return JSON.stringify({ name: name, email: email, avatar: avatar });
                    } catch(e) {
                        return "{}";
                    }
                })()
                """.trimIndent(),
            ) { resultJson ->
                var initialInfo: AccountInfo? = null
                if (!resultJson.isNullOrBlank() && resultJson != "null") {
                    try {
                        val parsedJson = if (resultJson.startsWith("\"") && resultJson.endsWith("\"")) {
                            JSONTokener(resultJson).nextValue().toString()
                        } else {
                            resultJson
                        }
                        val jsonObj = JSONObject(parsedJson)
                        val name = jsonObj.optString("name").takeIf { it.isNotBlank() }
                        val email = jsonObj.optString("email").takeIf { it.isNotBlank() }
                        val avatar = jsonObj.optString("avatar").takeIf { it.isNotBlank() }
                        if (!name.isNullOrBlank() || !avatar.isNullOrBlank()) {
                            initialInfo = AccountInfo(
                                name = name ?: "Pengguna YouTube",
                                email = email,
                                channelHandle = null,
                                thumbnailUrl = avatar,
                            )
                        }
                    } catch (_: Exception) {}
                }

                AccountRepository.getInstance(context).onLoginSuccess(combinedCookie, initialInfo)
                Toast.makeText(context, "Login berhasil! Akun tersinkronisasi.", Toast.LENGTH_SHORT).show()
                onLoginSuccess()
                onCompleted?.invoke()
            }
            return true
        }
        return false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Masuk Akun YouTube",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = "Login Resmi Google (OAuth Web)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        inputJsonText = ""
                        showImportJsonDialog = true
                    }) {
                        Icon(Icons.Outlined.FileUpload, contentDescription = "Impor Sesi JSON")
                    }
                    IconButton(onClick = { webViewInstance?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang")
                    }
                    IconButton(onClick = {
                        if (!checkAndSaveLogin(webViewInstance?.url)) {
                            Toast.makeText(context, "Belum terdeteksi login. Silakan selesaikan proses masuk di halaman web.", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Cek Status Login")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            inputJsonText = ""
                            showImportJsonDialog = true
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Impor JSON", fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            if (!checkAndSaveLogin(webViewInstance?.url)) {
                                Toast.makeText(context, "Belum ada sesi login aktif. Pastikan Anda sudah login.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text("Verifikasi Login", fontSize = 12.sp)
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (pageProgress in 0.01f..0.99f) {
                LinearProgressIndicator(
                    progress = { pageProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Security Notice Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Aman & Resmi: Login langsung ke server Google. Sandi Anda tidak disimpan di FathTube.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        createConfiguredWebView(ctx) { progress ->
                            pageProgress = progress
                        }.apply {
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    checkAndSaveLogin(url)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    checkAndSaveLogin(url)
                                }
                            }
                            loadUrl(GOOGLE_LOGIN_URL)
                            webViewInstance = this
                        }
                    },
                )
            }
        }
    }

    if (showImportJsonDialog) {
        val accountRepo = remember { AccountRepository.getInstance(context) }
        AlertDialog(
            onDismissRequest = { showImportJsonDialog = false },
            title = {
                Text(
                    text = "Impor Sesi Login JSON",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            },
            text = {
                Column {
                    Text(
                        text = "Tempelkan teks berkas nanztube_session.json di bawah ini untuk langsung masuk tanpa perlu memasukkan kata sandi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (accountRepo.isSessionJsonSaved()) {
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val success = accountRepo.restoreSession(force = true)
                                if (success) {
                                    showImportJsonDialog = false
                                    Toast.makeText(context, "Sesi login berhasil dipulihkan dari nanztube_session.json!", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess()
                                } else {
                                    Toast.makeText(context, "Gagal memulihkan sesi", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Gunakan nanztube_session.json di Perangkat (1 Klik)")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputJsonText,
                        onValueChange = { inputJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 200.dp),
                        placeholder = {
                            Text(
                                text = "{\"app\": \"FathTube\", \"cookie\": \"...\"}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 8,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = inputJsonText.trim()
                        if (trimmed.isBlank()) {
                            Toast.makeText(context, "Teks JSON tidak boleh kosong", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val result = accountRepo.importSessionJson(trimmed)
                        if (result.isSuccess) {
                            showImportJsonDialog = false
                            Toast.makeText(context, "Sesi login berhasil diimpor!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        } else {
                            val msg = result.exceptionOrNull()?.message ?: "Format JSON tidak valid"
                            Toast.makeText(context, "Gagal: $msg", Toast.LENGTH_LONG).show()
                        }
                    },
                ) {
                    Text("Impor & Masuk", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showImportJsonDialog = false }) {
                    Text("Batal")
                }
            },
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createConfiguredWebView(
    context: Context,
    onProgressUpdate: (Float) -> Unit,
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString =
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgressUpdate(newProgress / 100f)
            }
        }
    }
}
