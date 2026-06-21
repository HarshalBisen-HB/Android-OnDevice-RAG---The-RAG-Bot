// Author: Harshal R. Bisen
// UI screen for managing, adding, and viewing documents.

package com.ml.hrb.h.ui.screens.docs

import AppProgressDialog
import android.content.Intent
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ml.hrb.h.data.Document
import com.ml.hrb.h.domain.readers.Readers
import com.ml.hrb.h.domain.readers.getMimeType
import com.ml.hrb.h.ui.components.AppAlertDialog
import com.ml.hrb.h.ui.components.createAlertDialog
import com.ml.hrb.h.ui.theme.BrandOrange
import com.ml.hrb.h.ui.theme.glassEffect
import com.ml.hrb.h.ui.theme.TRB

private val showDocDetailDialog = mutableStateOf(false)
private val dialogDoc = mutableStateOf<Document?>(null)

@Preview
@Composable
private fun DocsScreenPreview() {
    val uiState =
        DocsScreenUIState(
            documents =
                listOf(
                    Document(
                        docId = 1,
                        docFileName = "Document 1",
                        docText = "Text 1",
                        docAddedTime = 0
                    ),
                    Document(
                        docId = 2,
                        docFileName = "Document 2",
                        docText = "Text 2",
                        docAddedTime = 0
                    ),
                ),
            docDownloadState = DocDownloadState.DOWNLOAD_NONE,
        )
    DocsScreen(uiState = uiState, onBackClick = {}, onEvent = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocsScreen(
    uiState: DocsScreenUIState,
    onBackClick: (() -> Unit),
    onEvent: (DocsScreenUIEvent) -> Unit,
) {
    TRB {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Manage Documents",
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
                    actions = {
                        var showSettingsDialog by remember { mutableStateOf(false) }
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Chunking Settings",
                                tint = Color.White
                            )
                        }
                        if (showSettingsDialog) {
                            ChunkingSettingsDialog(
                                uiState = uiState,
                                onDismiss = { showSettingsDialog = false },
                                onSave = { size, overlap, strategy ->
                                    onEvent(DocsScreenUIEvent.OnSaveChunkingSettings(size, overlap, strategy))
                                }
                            )
                        }
                    }
                )
            },
        ) { innerPadding ->
            Column(modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()) {
                Spacer(modifier = Modifier.height(12.dp))
                DocsList(uiState.documents, onEvent)
                DocOperations(uiState.docDownloadState, onEvent)
                AppProgressDialog()
                AppAlertDialog()
                DocDetailDialog()
            }
        }
    }
}

@Composable
private fun ColumnScope.DocsList(
    docs: List<Document>,
    onEvent: (DocsScreenUIEvent) -> Unit,
) {
    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .weight(1f)) {
        items(docs) { doc ->
            DocsListItem(
                doc.copy(
                    docText =
                        if (doc.docText.length > 200) {
                            doc.docText.substring(0, 200) + " ..."
                        } else {
                            doc.docText
                        },
                ),
                onRemoveDocClick = { docId -> onEvent(DocsScreenUIEvent.OnRemoveDoc(docId)) },
            )
        }
    }
}

@Composable
private fun DocsListItem(
    document: Document,
    onRemoveDocClick: ((Long) -> Unit),
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .glassEffect(RoundedCornerShape(12.dp), alpha = 0.08f)
                .clickable {
                    dialogDoc.value = document
                    showDocDetailDialog.value = true
                }
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .weight(1f)) {
            Text(
                text = document.docFileName,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = document.docText.trim().replace("\n", ""),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE0E0E0),
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = DateUtils.getRelativeTimeSpanString(document.docAddedTime).toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF909090),
            )
        }
        Icon(
            modifier =
                Modifier.clickable {
                    createAlertDialog(
                        dialogTitle = "Remove document",
                        dialogText =
                            "Are you sure to remove this document from the database. Responses to " +
                                "further queries will not refer content from this document.",
                        dialogPositiveButtonText = "Remove",
                        onPositiveButtonClick = { onRemoveDocClick(document.docId) },
                        dialogNegativeButtonText = "Cancel",
                        onNegativeButtonClick = {},
                    )
                },
            imageVector = Icons.Default.Clear,
            tint = Color.White,
            contentDescription = "Remove this document",
        )
        Spacer(modifier = Modifier.width(2.dp))
    }
}

@Composable
private fun ChooseDocTypeDialog(
    onDismiss: () -> Unit,
    onDocTypeSelected: (Readers.DocumentType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Document Type") },
        text = {
            Column {
                Text(
                    "PDF",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDocTypeSelected(Readers.DocumentType.PDF) }
                        .padding(vertical = 16.dp)
                )
                Text(
                    "MS Word (DOCX)",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDocTypeSelected(Readers.DocumentType.MS_DOCX) }
                        .padding(vertical = 16.dp)
                )
                Text(
                    "Plain Text",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDocTypeSelected(Readers.DocumentType.PLAIN_TEXT) }
                        .padding(vertical = 16.dp)
                )
                Text(
                    "Markdown",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDocTypeSelected(Readers.DocumentType.MARKDOWN) }
                        .padding(vertical = 16.dp)
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DocOperations(
    docDownloadState: DocDownloadState,
    onEvent: (DocsScreenUIEvent) -> Unit,
) {
    val context = LocalContext.current
    var docType by remember { mutableStateOf(Readers.DocumentType.PDF) }
    var pdfUrl by remember { mutableStateOf("") }
    var showUrlDialog by remember { mutableStateOf(false) }
    var showChooseDocTypeDialog by remember { mutableStateOf(false) }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            it.data?.data?.let { uri ->
                onEvent(DocsScreenUIEvent.OnDocSelected(uri, docType))
            }
        }

    if (showChooseDocTypeDialog) {
        ChooseDocTypeDialog(
            onDismiss = { showChooseDocTypeDialog = false },
            onDocTypeSelected = { selectedDocType ->
                showChooseDocTypeDialog = false
                docType = selectedDocType
                launcher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = docType.getMimeType()
                })
            }
        )
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 16.dp)
            .fillMaxWidth(),
    ) {
        // Upload from device
        Button(
            modifier = Modifier
                .weight(1f)
                .padding(2.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
            onClick = { showChooseDocTypeDialog = true },
        ) {
            Text(text = "Add From Device", color = Color.White)
        }

        // Add from URL
        Button(
            modifier = Modifier
                .weight(1f)
                .padding(2.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
            onClick = { showUrlDialog = true },
        ) {
            Text(text = "Add From URL", color = Color.White)
        }
    }

    when (docDownloadState) {
        DocDownloadState.DOWNLOAD_NONE -> {}
        DocDownloadState.DOWNLOAD_IN_PROGRESS -> {
            showUrlDialog = false
        }

        DocDownloadState.DOWNLOAD_SUCCESS -> {
            Toast.makeText(context, "Document added from URL", Toast.LENGTH_SHORT).show()
        }

        DocDownloadState.DOWNLOAD_FAILURE -> {
            Toast.makeText(context, "Failed to download", Toast.LENGTH_SHORT).show()
        }
    }

    // URL Dialog
    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = {
                showUrlDialog = false
                pdfUrl = ""
            },
            title = {
                Column {
                    Text("Add document from URL", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "The app will determine the type of the document using the file-extension of the downloaded " +
                            "document",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            },
            text = {
                Column {
                    TextField(
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        value = pdfUrl,
                        onValueChange = { pdfUrl = it },
                        label = { Text("Enter URL") },
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (pdfUrl.isNotBlank()) {
                        onEvent(DocsScreenUIEvent.OnDocURLSubmitted(context, pdfUrl, docType))
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                Button(onClick = { showUrlDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun DocDetailDialog() {
    var isVisible by remember { showDocDetailDialog }
    val context = LocalContext.current
    val doc by remember { dialogDoc }
    if (isVisible && doc != null) {
        Dialog(onDismissRequest = { /* Progress dialogs are non-cancellable */ }) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White, shape = RoundedCornerShape(8.dp))
                        .padding(24.dp),
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = doc?.docFileName ?: "",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = doc?.docText ?: "",
                        modifier = Modifier
                            .height(200.dp)
                            .verticalScroll(rememberScrollState()),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                            onClick = {
                                val sendIntent: Intent =
                                    Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, doc?.docText)
                                        type = "text/plain"
                                    }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            },
                        ) {
                            Text(text = "Share Text", color = Color.White)
                        }
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                            onClick = { isVisible = false },
                        ) {
                            Text(text = "Close", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChunkingSettingsDialog(
    uiState: DocsScreenUIState,
    onDismiss: () -> Unit,
    onSave: (Int, Int, String) -> Unit,
) {
    var size by remember { mutableStateOf(uiState.chunkSize) }
    var overlap by remember { mutableStateOf(uiState.chunkOverlap) }
    var strategy by remember { mutableStateOf(uiState.strategy) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Document Chunking Settings") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Chunk Size: $size characters")
                Slider(
                    value = size.toFloat(),
                    onValueChange = { size = it.toInt() },
                    valueRange = 200f..1000f,
                    steps = 16,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Chunk Overlap: $overlap characters")
                Slider(
                    value = overlap.toFloat(),
                    onValueChange = { overlap = it.toInt() },
                    valueRange = 20f..200f,
                    steps = 18,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Splitting Strategy:")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { strategy = "whitespace" }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = strategy == "whitespace",
                        onClick = { strategy = "whitespace" }
                    )
                    Text("Whitespace / Word boundaries", style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { strategy = "sentence" }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = strategy == "sentence",
                        onClick = { strategy = "sentence" }
                    )
                    Text("Sentence boundaries (semantic)", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(size, overlap, strategy) }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

