package com.fathtube.app.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fathtube.app.R

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF080D18),
        ),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    Color(0xFF0070F3).copy(alpha = 0.28f),
                    Color(0xFF00D2FF).copy(alpha = 0.08f),
                    Color(0xFF0070F3).copy(alpha = 0.18f),
                )
            ),
        ),
        shape = RoundedCornerShape(22.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            content = content,
        )
    }
}

@Composable
fun SectionHeader(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, bottom = 8.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF00D2FF), Color(0xFF0070F3))
                    )
                )
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            ),
            color = Color(0xFF00D2FF),
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF0070F3).copy(alpha = 0.18f),
                            Color(0xFF00D2FF).copy(alpha = 0.06f),
                        )
                    )
                )
                .border(
                    1.dp,
                    Color(0xFF0070F3).copy(alpha = 0.25f),
                    RoundedCornerShape(13.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF00D2FF),
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                ),
                color = Color.White,
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = Color(0xFF94A3B8),
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF0070F3).copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun SettingsSwitchItem(
    icon: Any,
    title: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            (if (checked) Color(0xFF0070F3) else Color(0xFF1E293B)).copy(alpha = 0.22f),
                            (if (checked) Color(0xFF00D2FF) else Color(0xFF0F172A)).copy(alpha = 0.08f),
                        )
                    )
                )
                .border(
                    1.dp,
                    (if (checked) Color(0xFF0070F3) else Color(0xFF334155)).copy(alpha = 0.35f),
                    RoundedCornerShape(13.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            when (icon) {
                is ImageVector -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) {
                            if (checked) Color(0xFF00D2FF) else Color(0xFF94A3B8)
                        } else {
                            Color(0xFF64748B).copy(alpha = 0.4f)
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }

                is Painter -> {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = if (enabled) {
                            if (checked) Color(0xFF00D2FF) else Color(0xFF94A3B8)
                        } else {
                            Color(0xFF64748B).copy(alpha = 0.4f)
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                ),
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = if (enabled) Color(0xFF94A3B8) else Color(0xFF94A3B8).copy(alpha = 0.5f),
                )
            }
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF0070F3),
                uncheckedThumbColor = Color(0xFF64748B),
                uncheckedTrackColor = Color(0xFF1E293B),
                checkedBorderColor = Color(0xFF00D2FF),
                uncheckedBorderColor = Color(0xFF334155),
            ),
        )
    }
}

@Composable
fun SimpleConfirmDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0B111E),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFCBD5E1),
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(text) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0070F3)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.confirm), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = Color(0xFF94A3B8))
            }
        },
    )
}

data class PickerOption(
    val key: String,
    val label: String,
    val secondaryLabel: String? = null,
)

@Composable
fun SearchablePickerDialog(
    title: String,
    options: List<PickerOption>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    listMaxHeight: Dp = 320.dp,
) {
    var query by remember { mutableStateOf("") }
    val filtered =
        remember(query, options) {
            if (query.isBlank()) {
                options
            } else {
                options.filter { option ->
                    option.key.contains(query, ignoreCase = true) ||
                        option.label.contains(query, ignoreCase = true) ||
                        option.secondaryLabel?.contains(query, ignoreCase = true) == true
                }
            }
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0B111E),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFCBD5E1),
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Color(0xFF00D2FF)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0070F3),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = listMaxHeight)) {
                    items(filtered, key = { it.key }) { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelect(option.key) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedKey == option.key,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF00D2FF),
                                    unselectedColor = Color(0xFF64748B),
                                ),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(option.label, color = Color.White, fontWeight = FontWeight.Medium)
                                option.secondaryLabel?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = Color(0xFF94A3B8))
            }
        },
    )
}
