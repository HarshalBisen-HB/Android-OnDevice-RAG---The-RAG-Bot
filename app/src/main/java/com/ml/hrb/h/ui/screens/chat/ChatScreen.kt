// Author: Harshal R. Bisen
// UI screen for the conversational chat interface.

package com.ml.hrb.h.ui.screens.chat

import android.content.Intent
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.ml.hrb.h.R
import com.ml.hrb.h.ui.theme.BrandOrange
import com.ml.hrb.h.ui.theme.TRB
import com.ml.hrb.h.ui.theme.glassEffect
import kotlinx.coroutines.launch

import com.ml.hrb.h.data.RetrievedContext
import com.ml.hrb.h.ui.components.AppAlertDialog
import com.ml.hrb.h.ui.components.createAlertDialog

import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    screenUiState: ChatScreenUIState,
    onScreenEvent: (ChatScreenUIEvent) -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    TRB {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(280.dp),
                    drawerContainerColor = Color(0xFF1A1A22),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Conversations",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // New Chat Button
                        NavigationDrawerItem(
                            label = { Text("New Chat", color = Color.White) },
                            icon = { Icon(Icons.Default.Add, contentDescription = "New Chat", tint = Color.White) },
                            selected = screenUiState.activeSessionId == 0L,
                            onClick = {
                                onScreenEvent(ChatScreenUIEvent.OnNewSessionClick)
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = BrandOrange.copy(alpha = 0.2f),
                                unselectedContainerColor = Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Sessions List
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(screenUiState.sessions) { session ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    NavigationDrawerItem(
                                        label = {
                                            Text(
                                                text = session.title,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        selected = screenUiState.activeSessionId == session.id,
                                        onClick = {
                                            onScreenEvent(ChatScreenUIEvent.OnSessionSelected(session.id))
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = NavigationDrawerItemDefaults.colors(
                                            selectedContainerColor = BrandOrange.copy(alpha = 0.2f),
                                            unselectedContainerColor = Color.Transparent
                                        )
                                    )
                                    IconButton(
                                        onClick = {
                                            onScreenEvent(ChatScreenUIEvent.OnDeleteSessionClick(session.id))
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Session",
                                            tint = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Chat",
                                style = MaterialTheme.typography.headlineSmall,
                                color = BrandOrange
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White,
                            actionIconContentColor = Color.White
                        ),
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = BrandOrange,
                                )
                            }
                        },
                        actions = {
                            var moreOptionsVisible by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = {
                                    moreOptionsVisible = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = BrandOrange,
                                    )
                                }
                                ChatScreenMoreOptionsPopup(
                                    expanded = moreOptionsVisible,
                                    onDismissRequest = { moreOptionsVisible = !moreOptionsVisible },
                                    onItemClick = { onScreenEvent(it) },
                                )
                            }
                        },
                    )
                },
            ) { innerPadding ->
                Column(
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                            .fillMaxWidth(),
                ) {
                    Column {
                        QALayout(screenUiState)
                        Spacer(modifier = Modifier.height(8.dp))
                        QueryInput(onScreenEvent)
                    }
                }
                AppAlertDialog()
            }
        }
    }
}

@Composable
private fun ColumnScope.QALayout(screenUiState: ChatScreenUIState) {
    val context = LocalContext.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .weight(1f),
    ) {
        if (screenUiState.messages.isEmpty() && !screenUiState.isGeneratingResponse) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .align(Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    modifier = Modifier.size(75.dp),
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = BrandOrange,
                )
                Text(
                    text = "Enter a query to see answers",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandOrange,
                )
            }
        } else {
            LazyColumn {
                // Past messages thread list
                items(screenUiState.messages) { message ->
                    val isUser = message.role == "user"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Text(
                            text = if (isUser) "You" else "Assistant",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandOrange,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        Column(
                            modifier = Modifier
                                .glassEffect(
                                    shape = RoundedCornerShape(12.dp),
                                    alpha = if (isUser) 0.12f else 0.06f
                                )
                                .padding(12.dp)
                                .fillMaxWidth(0.85f),
                        ) {
                            if (isUser) {
                                Text(
                                    text = message.text,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            } else {
                                MarkdownText(
                                    modifier = Modifier.fillMaxWidth(),
                                    markdown = message.text,
                                    style = TextStyle(color = Color.White, fontSize = 14.sp),
                                )
                            }
                        }
                    }
                }

                // Streaming answer bubble (currently generating)
                if (screenUiState.isGeneratingResponse) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "You",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandOrange,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                            Column(
                                modifier = Modifier
                                    .glassEffect(shape = RoundedCornerShape(12.dp), alpha = 0.12f)
                                    .padding(12.dp)
                                    .fillMaxWidth(0.85f),
                            ) {
                                Text(
                                    text = screenUiState.question,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Assistant",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandOrange,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                            Column(
                                modifier = Modifier
                                    .glassEffect(shape = RoundedCornerShape(12.dp), alpha = 0.06f)
                                    .padding(12.dp)
                                    .fillMaxWidth(0.85f),
                            ) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                if (screenUiState.response.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    MarkdownText(
                                        modifier = Modifier.fillMaxWidth(),
                                        markdown = screenUiState.response,
                                        style = TextStyle(color = Color.White, fontSize = 14.sp),
                                    )
                                }
                            }
                        }
                    }
                }

                // Citations list (retrieved context chunks)
                if (screenUiState.retrievedContextList.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Retrieved Context", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    items(screenUiState.retrievedContextList) { retrievedContext ->
                        var showDetails by remember { mutableStateOf(false) }
                        Column(
                            modifier =
                                Modifier
                                    .padding(vertical = 4.dp)
                                    .glassEffect(RoundedCornerShape(12.dp), alpha = 0.06f)
                                    .clickable { showDetails = true }
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = retrievedContext.fileName,
                                    color = BrandOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                val matchPercentage = (retrievedContext.similarityScore * 100).toInt().coerceIn(0, 100)
                                Box(
                                    modifier = Modifier
                                        .background(BrandOrange.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .border(1.dp, BrandOrange, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$matchPercentage% match",
                                        color = BrandOrange,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"${retrievedContext.context}\"",
                                color = Color(0xFFE0E0E0),
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (showDetails) {
                            AlertDialog(
                                onDismissRequest = { showDetails = false },
                                title = { Text("Citation Details") },
                                text = {
                                    Column {
                                        Text("Source: ${retrievedContext.fileName}", fontWeight = FontWeight.Bold, color = BrandOrange)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = retrievedContext.context,
                                            modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(onClick = { showDetails = false }) {
                                        Text("Close")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueryInput(onEvent: (ChatScreenUIEvent) -> Unit) {
    var questionText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextField(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, BrandOrange, RoundedCornerShape(16.dp)),
            value = questionText,
            onValueChange = { questionText = it },
            shape = RoundedCornerShape(16.dp),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.08f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledTextColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            placeholder = { Text(text = "Ask documents...", color = Color.Gray) },
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            modifier = Modifier.background(BrandOrange, CircleShape),
            onClick = {
                if (questionText.isNotBlank()) {
                    keyboardController?.hide()
                    try {
                        onEvent(
                            ChatScreenUIEvent.ResponseGeneration.Start(
                                questionText,
                                context.getString(R.string.prompt_1),
                            ),
                        )
                        questionText = ""
                    } catch (e: Exception) {
                        createAlertDialog(
                            dialogTitle = "Error",
                            dialogText = "An error occurred while generating the response: ${e.message}",
                            dialogPositiveButtonText = "Close",
                            onPositiveButtonClick = {},
                            dialogNegativeButtonText = null,
                            onNegativeButtonClick = {},
                        )
                    }
                }
            },
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Send query",
                tint = Color.White,
            )
        }
    }
}

@Composable
@Preview
private fun ChatScreenPreview() {
    ChatScreen(
        screenUiState = ChatScreenUIState(),
        onScreenEvent = { },
    )
}
