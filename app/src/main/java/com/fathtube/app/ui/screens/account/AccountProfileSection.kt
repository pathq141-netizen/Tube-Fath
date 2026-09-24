package com.fathtube.app.ui.screens.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import coil3.compose.AsyncImage
import com.fathtube.app.data.account.AccountRepository
import com.fathtube.app.data.account.AccountState

@Composable
fun AccountProfileSection(
    accountState: AccountState,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
    ) {
        if (accountState.isLoggedIn) {
            LoggedInAccountView(
                accountState = accountState,
                onLogoutClick = onLogoutClick,
                onSyncClick = onSyncClick,
            )
        } else {
            LoggedOutAccountView(
                onLoginClick = onLoginClick,
            )
        }
    }
}

@Composable
private fun LoggedInAccountView(
    accountState: AccountState,
    onLogoutClick: () -> Unit,
    onSyncClick: () -> Unit,
) {
    val info = accountState.accountInfo
    var showConfirmLogout by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!info?.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = info?.thumbnailUrl,
                    contentDescription = info?.name ?: "Profile Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = info?.name ?: "Akun YouTube Terhubung",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Terhubung",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = info?.channelHandle ?: info?.email ?: "Sinkronisasi realtime aktif",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onSyncClick,
                enabled = !accountState.isSyncing,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                if (accountState.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sinkron...", fontSize = 12.sp)
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sinkron Langganan", fontSize = 12.sp)
                }
            }

            OutlinedButton(
                onClick = { showConfirmLogout = true },
                modifier = Modifier.weight(0.7f),
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Keluar", fontSize = 12.sp)
            }
        }

        AnimatedVisibility(visible = !accountState.syncMessage.isNullOrBlank()) {
            Text(
                text = accountState.syncMessage ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (showConfirmLogout) {
            val context = LocalContext.current
            val accountRepo = remember { AccountRepository.getInstance(context) }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showConfirmLogout = false },
                title = { Text("Keluar dari Akun?") },
                text = { Text("Pilih apakah ingin tetap menyimpan berkas nanztube_session.json di perangkat untuk login cepat nanti, atau menghapusnya secara permanen.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmLogout = false
                            accountRepo.logout(deleteJsonFile = true)
                            Toast.makeText(context, "Sesi login dan file JSON dihapus", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text("Hapus Sesi & JSON")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showConfirmLogout = false }) {
                            Text("Batal")
                        }
                        Button(
                            onClick = {
                                showConfirmLogout = false
                                accountRepo.logout(deleteJsonFile = false)
                                Toast.makeText(context, "Keluar (file JSON tetap disimpan)", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Text("Keluar (Simpan JSON)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun LoggedOutAccountView(
    onLoginClick: () -> Unit,
) {
    val context = LocalContext.current
    val accountRepo = remember { AccountRepository.getInstance(context) }
    val hasSavedJson = remember { accountRepo.isSessionJsonSaved() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Masuk ke Akun YouTube",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = if (hasSavedJson) "Berkas nanztube_session.json terdeteksi" else "Sinkronkan langganan, like, & komentar realtime",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (hasSavedJson) {
            OutlinedButton(
                onClick = {
                    val success = accountRepo.restoreSession(force = true)
                    if (success) {
                        Toast.makeText(context, "Sesi login berhasil dipulihkan dari JSON!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Gagal memulihkan sesi", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(20.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pulihkan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Button(
            onClick = onLoginClick,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Masuk", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
