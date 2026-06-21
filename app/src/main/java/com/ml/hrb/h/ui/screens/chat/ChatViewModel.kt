// Author: Harshal R. Bisen
// ViewModel for handling the conversational chat logic and state.

package com.ml.hrb.h.ui.screens.chat

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ml.hrb.h.data.ChunksDB
import com.ml.hrb.h.data.DocumentsDB
import com.ml.hrb.h.data.GeminiAPIKey
import com.ml.hrb.h.data.RetrievedContext
import com.ml.hrb.h.domain.SentenceEmbeddingProvider
import com.ml.hrb.h.domain.llm.GeminiRemoteAPI
import com.ml.hrb.h.domain.llm.LLMInferenceAPI
import com.ml.hrb.h.domain.llm.LiteRTAPI
import com.ml.hrb.h.ui.components.createAlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import com.ml.hrb.h.data.ChatHistoryDB
import com.ml.hrb.h.data.ChatSession
import com.ml.hrb.h.data.ChatMessage

sealed interface ChatScreenUIEvent {
    data object OnEditCredentialsClick : ChatScreenUIEvent

    data object OnOpenDocsClick : ChatScreenUIEvent

    data object OnLocalModelsClick : ChatScreenUIEvent

    data class OnSessionSelected(val sessionId: Long) : ChatScreenUIEvent

    data object OnNewSessionClick : ChatScreenUIEvent

    data class OnDeleteSessionClick(val sessionId: Long) : ChatScreenUIEvent

    sealed class ResponseGeneration {
        data class Start(
            val query: String,
            val prompt: String,
        ) : ChatScreenUIEvent

        data class StopWithSuccess(
            val response: String,
            val retrievedContextList: List<RetrievedContext>,
        ) : ChatScreenUIEvent

        data class StopWithError(
            val errorMessage: String,
        ) : ChatScreenUIEvent
    }
}

sealed interface ChatNavEvent {
    data object None : ChatNavEvent

    data object ToEditAPIKeyScreen : ChatNavEvent

    data object ToDocsScreen : ChatNavEvent

    data object ToLocalModelsScreen : ChatNavEvent
}

data class ChatScreenUIState(
    val question: String = "",
    val response: String = "",
    val isGeneratingResponse: Boolean = false,
    val retrievedContextList: List<RetrievedContext> = emptyList(),
    val sessions: List<ChatSession> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val activeSessionId: Long = 0L,
)

@KoinViewModel
class ChatViewModel(
    private val context: Context,
    private val documentsDB: DocumentsDB,
    private val chunksDB: ChunksDB,
    private val geminiAPIKey: GeminiAPIKey,
    private val sentenceEncoder: SentenceEmbeddingProvider,
    private val liteRTAPI: LiteRTAPI,
    private val chatHistoryDB: ChatHistoryDB,
) : ViewModel() {
    private val _chatScreenUIState = MutableStateFlow(ChatScreenUIState())
    val chatScreenUIState: StateFlow<ChatScreenUIState> = _chatScreenUIState

    private val _navEventChannel = Channel<ChatNavEvent>()
    val navEventChannel = _navEventChannel.receiveAsFlow()

    private var messagesJob: Job? = null

    init {
        viewModelScope.launch {
            chatHistoryDB.getAllSessionsFlow().collect { list ->
                _chatScreenUIState.update { it.copy(sessions = list) }
            }
        }
    }

    fun selectSession(sessionId: Long) {
        _chatScreenUIState.update { it.copy(activeSessionId = sessionId, question = "", response = "") }
        messagesJob?.cancel()
        if (sessionId != 0L) {
            messagesJob = viewModelScope.launch {
                chatHistoryDB.getMessagesForSessionFlow(sessionId).collect { list ->
                    _chatScreenUIState.update { it.copy(messages = list) }
                }
            }
        } else {
            _chatScreenUIState.update { it.copy(messages = emptyList()) }
        }
    }

    fun onChatScreenEvent(event: ChatScreenUIEvent) {
        when (event) {
            is ChatScreenUIEvent.OnSessionSelected -> {
                selectSession(event.sessionId)
            }

            is ChatScreenUIEvent.OnNewSessionClick -> {
                selectSession(0L)
            }

            is ChatScreenUIEvent.OnDeleteSessionClick -> {
                viewModelScope.launch(Dispatchers.IO) {
                    chatHistoryDB.deleteSession(event.sessionId)
                    if (_chatScreenUIState.value.activeSessionId == event.sessionId) {
                        withContext(Dispatchers.Main) {
                            selectSession(0L)
                        }
                    }
                }
            }

            is ChatScreenUIEvent.ResponseGeneration.Start -> {
                if (!checkNumDocuments()) {
                    Toast
                        .makeText(
                            context,
                            "Add documents to execute queries",
                            Toast.LENGTH_LONG,
                        ).show()
                    return
                }
                if (!checkValidAPIKey()) {
                    createAlertDialog(
                        dialogTitle = "Invalid API Key",
                        dialogText = "Please enter a Gemini API key to use a LLM for generating responses.",
                        dialogPositiveButtonText = "Add API key",
                        onPositiveButtonClick = {
                            onChatScreenEvent(ChatScreenUIEvent.OnEditCredentialsClick)
                        },
                        dialogNegativeButtonText = "Open Gemini Console",
                        onNegativeButtonClick = {
                            Intent(Intent.ACTION_VIEW).apply {
                                data = "https://aistudio.google.com/apikey".toUri()
                                context.startActivity(this)
                            }
                        },
                    )
                    return
                }
                if (event.query.trim().isEmpty()) {
                    Toast
                        .makeText(context, "Enter a query to execute", Toast.LENGTH_LONG)
                        .show()
                    return
                }
                _chatScreenUIState.value =
                    _chatScreenUIState.value.copy(
                        isGeneratingResponse = true,
                        question = event.query,
                        response = "",
                        retrievedContextList = emptyList()
                    )

                val llm =
                    if (liteRTAPI.isLoaded) {
                        Toast.makeText(context, "Using local model...", Toast.LENGTH_LONG).show()
                        liteRTAPI
                    } else {
                        val apiKey = geminiAPIKey.getAPIKey() ?: throw Exception("Gemini API key is null")
                        Toast.makeText(context, "Using Gemini cloud model...", Toast.LENGTH_LONG).show()
                        GeminiRemoteAPI(apiKey)
                    }

                viewModelScope.launch(Dispatchers.Main) {
                    var sessionId = _chatScreenUIState.value.activeSessionId
                    if (sessionId == 0L) {
                        val title = event.query.split(" ").take(3).joinToString(" ")
                        sessionId = withContext(Dispatchers.IO) {
                            chatHistoryDB.createSession(title)
                        }
                        selectSession(sessionId)
                    }
                    withContext(Dispatchers.IO) {
                        chatHistoryDB.addMessage(
                            ChatMessage(
                                sessionId = sessionId,
                                role = "user",
                                text = event.query,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                    getAnswer(llm, event.query, event.prompt, sessionId)
                }
            }

            is ChatScreenUIEvent.ResponseGeneration.StopWithSuccess -> {
                _chatScreenUIState.value =
                    _chatScreenUIState.value.copy(isGeneratingResponse = false, response = "")
                val sessionId = _chatScreenUIState.value.activeSessionId
                viewModelScope.launch(Dispatchers.IO) {
                    chatHistoryDB.addMessage(
                        ChatMessage(
                            sessionId = sessionId,
                            role = "model",
                            text = event.response,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }

            is ChatScreenUIEvent.ResponseGeneration.StopWithError -> {
                _chatScreenUIState.value =
                    _chatScreenUIState.value.copy(isGeneratingResponse = false, response = "")
            }

            is ChatScreenUIEvent.OnOpenDocsClick -> {
                viewModelScope.launch {
                    _navEventChannel.send(ChatNavEvent.ToDocsScreen)
                }
            }

            is ChatScreenUIEvent.OnEditCredentialsClick -> {
                viewModelScope.launch {
                    _navEventChannel.send(ChatNavEvent.ToEditAPIKeyScreen)
                }
            }

            is ChatScreenUIEvent.OnLocalModelsClick -> {
                viewModelScope.launch {
                    _navEventChannel.send(ChatNavEvent.ToLocalModelsScreen)
                }
            }
        }
    }

    private fun getAnswer(
        llm: LLMInferenceAPI,
        query: String,
        prompt: String,
        sessionId: Long,
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                // Perform CPU-heavy embedding calculation and DB lookup on background thread
                val (inputPrompt, retrievedContextList) = withContext(Dispatchers.IO) {
                    val contextList = ArrayList<RetrievedContext>()
                    val queryEmbedding = sentenceEncoder.encodeText(query)
                    var jointContext = ""
                    chunksDB.getSimilarChunks(queryEmbedding, n = 5).forEach {
                        val score = it.first
                        jointContext += " " + it.second.chunkData
                        contextList.add(
                            RetrievedContext(
                                fileName = it.second.docFileName,
                                context = it.second.chunkData,
                                similarityScore = score
                            ),
                        )
                    }

                    // Fetch history and format
                    val pastMessages = chatHistoryDB.getMessagesForSession(sessionId)
                    val historyBuilder = StringBuilder()
                    // Exclude the last message which is the current query we just added
                    val historyMessages = if (pastMessages.size > 1) pastMessages.dropLast(1) else emptyList()
                    historyMessages.takeLast(6).forEach { msg ->
                        val speaker = if (msg.role == "user") "User" else "Assistant"
                        historyBuilder.append("$speaker: ${msg.text}\n")
                    }
                    val queryWithHistory = if (historyBuilder.isNotEmpty()) {
                        "Chat History:\n$historyBuilder\nUser: $query"
                    } else {
                        query
                    }

                    val formattedPrompt = prompt.replace("\$CONTEXT", jointContext).replace("\$QUERY", queryWithHistory)
                    Pair(formattedPrompt, contextList)
                }

                // Show retrieved context cards immediately
                _chatScreenUIState.update {
                    it.copy(retrievedContextList = retrievedContextList)
                }

                // Stream response on main thread (collection flow transitions from background threads automatically)
                llm.getResponseStream(inputPrompt).collect { chunk ->
                    _chatScreenUIState.update {
                        it.copy(response = it.response + chunk)
                    }
                }

                onChatScreenEvent(
                    ChatScreenUIEvent.ResponseGeneration.StopWithSuccess(
                        _chatScreenUIState.value.response,
                        retrievedContextList,
                    ),
                )
            } catch (e: Exception) {
                onChatScreenEvent(
                    ChatScreenUIEvent.ResponseGeneration.StopWithError(
                        e.message ?: "Unknown error"
                    ),
                )
            }
        }
    }

    fun checkNumDocuments(): Boolean = documentsDB.getDocsCount() > 0

    fun checkValidAPIKey(): Boolean = geminiAPIKey.getAPIKey() != null
}
