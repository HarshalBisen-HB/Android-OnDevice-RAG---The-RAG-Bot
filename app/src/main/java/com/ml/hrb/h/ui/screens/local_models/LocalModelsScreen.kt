// Author: Harshal R. Bisen
// UI screen for browsing and managing local LLM models.

package com.ml.hrb.h.ui.screens.local_models

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ml.hrb.h.data.LocalModel
import com.ml.hrb.h.ui.components.AppAlertDialog
import com.ml.hrb.h.ui.theme.BrandOrange
import com.ml.hrb.h.ui.theme.TRB
import com.ml.hrb.h.ui.theme.glassEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalModelsScreen(
    uiState: LocalModelsUIState,
    onEvent: (LocalModelsUIEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    var customUrl by remember { mutableStateOf("") }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            onEvent(LocalModelsUIEvent.OnImportModel(it))
        }
    }

    TRB {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Manage Local Models",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate Back",
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .fillMaxWidth(),
            ) {
                LaunchedEffect(0) {
                    onEvent(LocalModelsUIEvent.RefreshModelsList)
                }

                // Import & Custom Download Panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .glassEffect(RoundedCornerShape(16.dp), alpha = 0.08f)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Import or Download Custom Model",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customUrl,
                            onValueChange = { customUrl = it },
                            placeholder = { Text("Model URL (starts with https://)", color = Color.Gray) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = BrandOrange,
                                unfocusedBorderColor = Color.Gray,
                                cursorColor = BrandOrange
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (customUrl.isNotBlank()) {
                                    onEvent(LocalModelsUIEvent.OnDownloadCustomModel(customUrl))
                                    customUrl = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download custom URL",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Import model",
                            tint = Color.White,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Import Local Model (.task / .tflite)", color = Color.White)
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.White.copy(alpha = 0.12f)
                )

                Box(modifier = Modifier.weight(1f)) {
                    LocalModelsList(
                        uiState.models,
                        onDownloadModelClick = { localModel ->
                            onEvent(LocalModelsUIEvent.OnModelDownloadClick(localModel))
                        },
                        onLoadModelClick = { localModel ->
                            onEvent(LocalModelsUIEvent.OnUseModelClick(localModel))
                        },
                    )
                }

                DownloadDialogModel(
                    dialogState = uiState.downloadModelDialogState,
                    onDismiss = {
                        onEvent(LocalModelsUIEvent.DismissDownloadDialog)
                    },
                    onPauseClick = {
                        onEvent(LocalModelsUIEvent.OnPauseDownloadClick)
                    },
                    onResumeClick = {
                        onEvent(LocalModelsUIEvent.OnResumeDownloadClick)
                    }
                )
                AppAlertDialog()
            }
        }
    }
}

@Composable
private fun LocalModelsList(
    modelsList: List<LocalModel>,
    onDownloadModelClick: (LocalModel) -> Unit,
    onLoadModelClick: (LocalModel) -> Unit,
) {
    val context = LocalContext.current
    LazyColumn {
        items(modelsList) { localModel ->
            LocalModelListItem(
                modelName = localModel.name,
                modelDescription = localModel.description,
                isDownloaded =
                    if (context.filesDir != null) {
                        // `context.filesDir` can be null when rendering
                        // the Compose preview
                        localModel.isDownloaded(context.filesDir.absolutePath)
                    } else {
                        true
                    },
                isLoaded = localModel.isLoaded,
                onDownloadClick = { onDownloadModelClick(localModel) },
                onLoadModelClick = { onLoadModelClick(localModel) },
            )
        }
    }
}

@Composable
private fun LocalModelListItem(
    modelName: String,
    modelDescription: String,
    isDownloaded: Boolean,
    isLoaded: Boolean,
    onDownloadClick: () -> Unit,
    onLoadModelClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .glassEffect(RoundedCornerShape(12.dp), alpha = 0.08f)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = modelName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = modelDescription,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF909090),
            )
        }
        if (isDownloaded) {
            if (isLoaded) {
                Box(
                    modifier =
                        Modifier
                            .background(BrandOrange.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(1.dp, BrandOrange, RoundedCornerShape(4.dp))
                            .padding(4.dp),
                ) {
                    Text("Loaded", style = MaterialTheme.typography.labelSmall, color = BrandOrange)
                }
            } else {
                IconButton(onClick = onLoadModelClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Load model",
                        tint = BrandOrange,
                    )
                }
            }
        } else {
            IconButton(onClick = onDownloadClick) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download model",
                    tint = BrandOrange,
                )
            }
        }
    }
}

@Composable
@Preview
private fun LocalModelsScreenPreview() {
    LocalModelsScreen(
        uiState =
            LocalModelsUIState(
                models =
                    listOf<LocalModel>(
                        LocalModel(
                            name = "Qwen3 8B",
                            description = "A Qwen family model series",
                            isLoaded = false,
                            downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_seq128_q8_ekv4096.task",
                        ),
                        LocalModel(
                            name = "Qwen2.5 1.5B",
                            description = "A Qwen family model series",
                            isLoaded = true,
                            downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_seq128_q8_ekv4096.task",
                        ),
                    ),
            ),
        onEvent = { },
        onBackClick = {},
    )
}
