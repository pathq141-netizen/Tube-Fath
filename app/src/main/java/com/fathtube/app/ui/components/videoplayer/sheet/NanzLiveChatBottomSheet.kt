package com.fathtube.app.ui.components.videoplayer.sheet

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fathtube.app.R
import com.fathtube.app.data.model.LiveChatMessage
import com.fathtube.app.ui.components.shared.NanzBottomSheet
import com.fathtube.app.ui.components.shared.NanzSheetHeader
import com.fathtube.app.ui.components.shared.defaultSheetExpandedHeight
import com.fathtube.app.ui.components.shared.rememberNanzBottomSheetState

// Draggable live-chat bottom sheet (portrait)
@Composable
fun NanzLiveChatBottomSheet(
    messages: List<LiveChatMessage>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    expandedHeight: Dp? = null,
    collapsedHeight: Dp = 0.dp,
    onSheetProgressChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberNanzBottomSheetState()
    NanzBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
        state = sheetState,
        expandedHeight = expandedHeight ?: defaultSheetExpandedHeight(),
        collapsedHeight = collapsedHeight,
        dismissOnOutsideTap = false,
        shape = RectangleShape,
        containerColor = MaterialTheme.colorScheme.surface,
        onProgressChange = onSheetProgressChange,
        header = { dragModifier ->
            NanzSheetHeader(
                title = stringResource(R.string.live_chat),
                onClose = { sheetState.dismiss() },
                modifier = dragModifier,
            )
        },
    ) {
        LiveChatList(
            messages = messages,
            isLoading = isLoading,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
        )
    }
}
