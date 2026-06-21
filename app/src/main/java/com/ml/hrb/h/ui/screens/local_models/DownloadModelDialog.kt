// Author: Harshal R. Bisen
// UI dialog component for initiating local model downloads.

package com.ml.hrb.h.ui.screens.local_models

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ml.hrb.h.ui.theme.BrandOrange

@Composable
fun DownloadDialogModel(
    dialogState: DownloadModelDialogUIState,
    onDismiss: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
) {
    if (dialogState.isDialogVisible) {
        Dialog(
            onDismissRequest = onDismiss,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Downloading Model",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (dialogState.isPaused) "Download paused." else "The selected model is being downloaded to your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    if (dialogState.showProgress) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Progress",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${dialogState.progress}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandOrange
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { dialogState.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = BrandOrange,
                            trackColor = BrandOrange.copy(alpha = 0.2f)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (dialogState.showProgress) {
                            Button(
                                onClick = {
                                    if (dialogState.isPaused) {
                                        onResumeClick()
                                    } else {
                                        onPauseClick()
                                    }
                                },
                                modifier = Modifier.padding(end = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (dialogState.isPaused) BrandOrange else MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text(if (dialogState.isPaused) "Resume" else "Pause")
                            }
                        }
                        Button(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview
private fun DownloadDialogModelPreview() {
    DownloadDialogModel(
        dialogState =
            DownloadModelDialogUIState(
                isDialogVisible = true,
                showProgress = true,
                progress = 80,
            ),
        onDismiss = {},
        onPauseClick = {},
        onResumeClick = {},
    )
}

