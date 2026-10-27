package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReportReason
import com.example.ui.theme.BlinkGreen
import com.example.ui.theme.BlinkPink
import com.example.ui.theme.BlinkPrimary

@Composable
fun ReportDialog(
    targetName: String,
    onDismiss: () -> Unit,
    onSubmitReport: (reason: ReportReason, details: String) -> Unit
) {
    var selectedReason by remember { mutableStateOf(ReportReason.HARASSMENT) }
    var detailsText by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Report,
                    contentDescription = "Report",
                    tint = BlinkPink,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSubmitted) "Report Received" else "Report $targetName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            if (isSubmitted) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = BlinkGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Thank you for keeping Blinkchat safe.",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Our trust & safety team has received your report. The reported content has been hidden from your feed.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "Help us understand what's happening. Select the reason that best applies:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(ReportReason.values()) { reason ->
                        val isSelected = selectedReason == reason
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedReason = reason },
                            color = if (isSelected) BlinkPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            border = if (isSelected) ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BlinkPrimary)) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedReason = reason }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = reason.displayName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = reason.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = detailsText,
                            onValueChange = { detailsText = it },
                            label = { Text("Additional details (optional)") },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            maxLines = 3
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isSubmitted) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dismiss_report_button")
                ) {
                    Text("Done")
                }
            } else {
                Button(
                    onClick = {
                        isSubmitted = true
                        onSubmitReport(selectedReason, detailsText)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BlinkPink),
                    modifier = Modifier.testTag("submit_report_button")
                ) {
                    Text("Submit Report")
                }
            }
        },
        dismissButton = {
            if (!isSubmitted) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun BlockConfirmDialog(
    targetName: String,
    onDismiss: () -> Unit,
    onConfirmBlock: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = "Block User",
                tint = BlinkPink,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Block $targetName?",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "When you block this person:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "• They will not be able to message or call you\n" +
                           "• They cannot view your stories, posts, or profile\n" +
                           "• Their existing posts and chats will be hidden\n" +
                           "• They won't be notified that they were blocked",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You can unblock them at any time from Settings > Blocked Users.",
                    fontSize = 12.sp,
                    color = BlinkPrimary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmBlock,
                colors = ButtonDefaults.buttonColors(containerColor = BlinkPink),
                modifier = Modifier.testTag("confirm_block_button")
            ) {
                Text("Block User")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
