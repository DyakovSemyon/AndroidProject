package com.example.coflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.coflow.R
import com.example.coflow.ui.theme.CoFlowColors

@Composable
fun NotificationBell() {
    var dialogTitle by remember { mutableStateOf<String?>(null) }
    var dialogMessage by remember { mutableStateOf("") }

    val notificationTitle = stringResource(R.string.notification_title)
    val notificationMessage = stringResource(R.string.notification_message)
    val dialogOkText = stringResource(R.string.dialog_ok)

    Box {
        CoFlowGlassIconButton(
            iconName = "bell",
            onClick = {
                dialogTitle = notificationTitle
                dialogMessage = notificationMessage
            }
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 3.dp, end = 3.dp)
                .size(6.dp)
                .background(CoFlowColors.Blue, CircleShape)
        )
    }

    // Всплывающее окно уведомлений
    if (dialogTitle != null) {
        AlertDialog(
            onDismissRequest = { dialogTitle = null },
            title = { Text(text = dialogTitle!!, color = CoFlowColors.TextPrimary) },
            text = { Text(text = dialogMessage, color = CoFlowColors.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { dialogTitle = null }) {
                    Text(dialogOkText, color = CoFlowColors.Blue)
                }
            },
            containerColor = Color(0xFFF4F7FF)
        )
    }


}