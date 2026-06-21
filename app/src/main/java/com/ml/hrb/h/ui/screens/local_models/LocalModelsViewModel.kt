// Author: Harshal R. Bisen
// ViewModel for managing the state and logic of local LLM models.

package com.ml.hrb.h.ui.screens.local_models

import android.content.Context
import android.os.storage.StorageManager
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ketch.Ketch
import com.ketch.Status
import com.ml.hrb.h.data.HFAccessToken
import com.ml.hrb.h.data.LocalModel
import com.ml.hrb.h.domain.llm.LiteRTAPI
import com.ml.hrb.h.ui.components.createAlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.android.annotation.KoinViewModel

import android.net.Uri
import androidx.core.net.toUri

sealed class LocalModelsUIEvent {
    data class OnModelDownloadClick(
        val model: LocalModel,
    ) : LocalModelsUIEvent()

    data class OnUseModelClick(
        val model: LocalModel,
    ) : LocalModelsUIEvent()

    data object RefreshModelsList : LocalModelsUIEvent()

    data object OnPauseDownloadClick : LocalModelsUIEvent()

    data object OnResumeDownloadClick : LocalModelsUIEvent()

    data class OnImportModel(val uri: Uri) : LocalModelsUIEvent()

    data class OnDownloadCustomModel(val url: String) : LocalModelsUIEvent()

    data object DismissDownloadDialog : LocalModelsUIEvent()
}

data class LocalModelsUIState(
    val models: List<LocalModel> = emptyList(),
    val downloadModelDialogState: DownloadModelDialogUIState = DownloadModelDialogUIState(),
)

data class DownloadModelDialogUIState(
    val isDialogVisible: Boolean = false,
    val showProgress: Boolean = false,
    val progress: Int = 0,
    val downloadId: Int? = null,
    val isPaused: Boolean = false,
)

sealed interface DownloadPrecheckError {
    data object InsufficientStorage : DownloadPrecheckError
    data object APIKeyRequired : DownloadPrecheckError
    data object ModelFileSizeUnknown : DownloadPrecheckError
    data object AvailableStorageUnknown : DownloadPrecheckError
}

@KoinViewModel
class LocalModelsViewModel(
    private val context: Context,
    private val liteRTAPI: LiteRTAPI,
    private val hfAccessToken: HFAccessToken,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            LocalModelsUIState(
                models = emptyList()
            ),
        )
    val uiState: StateFlow<LocalModelsUIState> = _uiState.asStateFlow()

    private var ketch: Ketch =
        Ketch
            .builder()
            .setOkHttpClient(
                OkHttpClient
                    .Builder()
                    .connectTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                    .build(),
            ).build(context)

    fun onEvent(event: LocalModelsUIEvent) {
        when (event) {
            is LocalModelsUIEvent.OnModelDownloadClick -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val error = checkIfStorageAvailable(event.model.downloadUrl)
                    if (error != null) {
                        val errorMessage = when (error) {
                            DownloadPrecheckError.InsufficientStorage -> "The device does not have enough storage space to download the model."
                            DownloadPrecheckError.APIKeyRequired -> "Downloading this model requires setting a HuggingFace API key."
                            DownloadPrecheckError.ModelFileSizeUnknown -> "The model file-size cannot be determined. Either the model is not available on HF or the app is not able to connect to HF."
                            DownloadPrecheckError.AvailableStorageUnknown -> "The available storage space on this device cannot be determined."
                        }
                        createAlertDialog(
                            dialogTitle = "Error",
                            dialogText = errorMessage,
                            dialogPositiveButtonText = "Ok",
                            onPositiveButtonClick = {},
                            dialogNegativeButtonText = null,
                            onNegativeButtonClick = null
                        )
                    } else {
                        downloadModel(event.model)
                    }
                }
            }

            is LocalModelsUIEvent.OnUseModelClick -> {
                if (liteRTAPI.isLoaded) {
                    liteRTAPI.unload()
                }
                viewModelScope.launch(Dispatchers.IO) {
                    loadModel(event.model)
                    onEvent(LocalModelsUIEvent.RefreshModelsList)
                }
            }

            is LocalModelsUIEvent.RefreshModelsList -> {
                _uiState.update {
                    it.copy(
                        models = getUpdatedModelsList()
                    )
                }
            }

            is LocalModelsUIEvent.OnPauseDownloadClick -> {
                val downloadId = _uiState.value.downloadModelDialogState.downloadId
                if (downloadId != null) {
                    ketch.pause(downloadId)
                    _uiState.update {
                        it.copy(
                            downloadModelDialogState = it.downloadModelDialogState.copy(
                                isPaused = true
                            )
                        )
                    }
                }
            }

            is LocalModelsUIEvent.OnResumeDownloadClick -> {
                val downloadId = _uiState.value.downloadModelDialogState.downloadId
                if (downloadId != null) {
                    ketch.resume(downloadId)
                    _uiState.update {
                        it.copy(
                            downloadModelDialogState = it.downloadModelDialogState.copy(
                                isPaused = false
                            )
                        )
                    }
                }
            }

            is LocalModelsUIEvent.OnImportModel -> {
                importModel(event.uri)
            }

            is LocalModelsUIEvent.OnDownloadCustomModel -> {
                downloadCustomModel(event.url)
            }

            is LocalModelsUIEvent.DismissDownloadDialog -> {
                _uiState.update {
                    it.copy(
                        downloadModelDialogState = it.downloadModelDialogState.copy(
                            isDialogVisible = false
                        )
                    )
                }
            }
        }
    }

    private suspend fun loadModel(model: LocalModel) =
        withContext(Dispatchers.IO) {
            liteRTAPI.load(
                context,
                model.getLocalModelPath(context.filesDir.absolutePath),
                onSuccess = {},
                onError = { exception ->
                    Log.e("APP", "Failed to load LiteRT model: ${exception.message}")
                },
            )
        }

    private fun getUpdatedModelsList(): List<LocalModel> {
        val defaultModels = listOf(
            LocalModel(
                name = "Qwen2.5 0.5B Instruct Q8",
                description = "A Qwen family model series",
                downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            ),
            LocalModel(
                name = "Qwen2.5 1.5B Instruct Q8",
                description = "A Qwen family model series",
                downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_seq128_q8_ekv4096.task",
            ),
            LocalModel(
                name = "Qwen2.5 3B Instruct Q8",
                description = "A Qwen family model series",
                downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-3B-Instruct/resolve/main/Qwen2.5-3B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            ),
            LocalModel(
                name = "Phi 4 Mini Instruct Q8",
                description = "A Microsoft Phi 4 model series",
                downloadUrl = "https://huggingface.co/litert-community/Phi-4-mini-instruct/resolve/main/Phi-4-mini-instruct_multi-prefill-seq_q8_ekv1280.task",
            ),
            LocalModel(
                name = "DeepSeek R1 Distill Qwen 1.5B Q8",
                description = "DeepSeek R1",
                downloadUrl = "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv4096.task",
            ),
            LocalModel(
                name = "Gemma3 1B IT",
                description = "Gemma 3 1B Instruction-Tuned (gated model)",
                downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q8_ekv4096.task",
            ),
            LocalModel(
                name = "Gemma3 4B IT",
                description = "Gemma 3 4B Instruction-Tuned (gated model)",
                downloadUrl = "https://huggingface.co/litert-community/Gemma3-4B-IT/resolve/main/gemma3-4b-it-int8-web.task",
            ),
            LocalModel(
                name = "Llama 3.2 1B Q8",
                description = "Llama 3.2 1B (gated model)",
                downloadUrl = "https://huggingface.co/litert-community/Llama-3.2-1B-Instruct/resolve/main/Llama-3.2-1B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            ),
            LocalModel(
                name = "Llama 3.2 3B Q8",
                description = "Llama 3.2 3B (gated model)",
                downloadUrl = "https://huggingface.co/litert-community/Llama-3.2-3B-Instruct/resolve/main/Llama-3.2-3B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            ),
        )
        val defaultFileNames = defaultModels.map { it.getFileName() }.toSet()

        val filesDir = context.filesDir
        val scannedModels = if (filesDir != null && filesDir.exists()) {
            filesDir.listFiles()?.filter {
                (it.name.endsWith(".task") || it.name.endsWith(".tflite")) && it.name !in defaultFileNames
            }?.map { file ->
                LocalModel(
                    name = file.name,
                    description = "Imported / Custom model",
                    downloadUrl = "https://local/${file.name}",
                )
            } ?: emptyList()
        } else {
            emptyList()
        }

        val allModels = defaultModels + scannedModels
        return allModels.map { model ->
            model.copy(
                isLoaded = liteRTAPI.loadedModelPath == model.getLocalModelPath(filesDir.absolutePath),
            )
        }
    }

    private fun importModel(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileName = getFileNameFromUri(context, uri)
                if (!fileName.endsWith(".task") && !fileName.endsWith(".tflite")) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Only .task or .tflite files are supported", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                val destFile = java.io.File(context.filesDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    java.io.FileOutputStream(destFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Model imported successfully: $fileName", Toast.LENGTH_LONG).show()
                }
                onEvent(LocalModelsUIEvent.RefreshModelsList)
            } catch (e: Exception) {
                Log.e("APP", "Failed to import model", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String {
        var name = ""
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        if (name.isEmpty()) {
            name = uri.lastPathSegment ?: "imported_model.task"
        }
        return name
    }

    private fun downloadCustomModel(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = url.toUri()
                val fileName = uri.lastPathSegment
                if (fileName.isNullOrBlank() || (!fileName.endsWith(".task") && !fileName.endsWith(".tflite"))) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Invalid download URL or file must end with .task or .tflite", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val model = LocalModel(
                    name = fileName,
                    description = "Custom downloaded model",
                    downloadUrl = url
                )

                val error = checkIfStorageAvailable(url)
                if (error != null) {
                    val errorMessage = when (error) {
                        DownloadPrecheckError.InsufficientStorage -> "The device does not have enough storage space to download the model."
                        DownloadPrecheckError.APIKeyRequired -> "Downloading this model requires setting a HuggingFace API key."
                        DownloadPrecheckError.ModelFileSizeUnknown -> "The model file-size cannot be determined."
                        DownloadPrecheckError.AvailableStorageUnknown -> "The available storage space on this device cannot be determined."
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                } else {
                    downloadModel(model)
                }
            } catch (e: Exception) {
                Log.e("APP", "Failed to download custom model", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun checkIfStorageAvailable(modelUrl: String): DownloadPrecheckError? =
        withContext(Dispatchers.IO) {
            val client = OkHttpClient()

            val requestBuilder = Request.Builder()
                .url(modelUrl)
                .head()

            val token = hfAccessToken.getToken()
            if (token != null) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val response = client.newCall(requestBuilder.build()).execute()

            if (response.code == 401) return@withContext DownloadPrecheckError.APIKeyRequired

            val fileSizeInBytes = response.headers["Content-Length"]?.toLong()
            Log.d("Doc-QA", "Model file size calculated from HEAD request: $fileSizeInBytes")

            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val availableSpaceInBytes = context.getExternalFilesDir(null)?.let {
                storageManager.getAllocatableBytes(storageManager.getUuidForPath(it))
            }
            Log.d("Doc-QA", "Available space on device: $availableSpaceInBytes")

            if (availableSpaceInBytes == null) return@withContext DownloadPrecheckError.AvailableStorageUnknown
            if (fileSizeInBytes == null) return@withContext DownloadPrecheckError.ModelFileSizeUnknown
            if (fileSizeInBytes > availableSpaceInBytes) return@withContext DownloadPrecheckError.InsufficientStorage

            return@withContext null
        }

    private suspend fun downloadModel(model: LocalModel) {
        val headers =
            if (hfAccessToken.getToken() != null) {
                HashMap(
                    mapOf("Authorization" to "Bearer ${hfAccessToken.getToken()}"),
                )
            } else {
                HashMap()
            }
        val downloadId =
            ketch.download(
                model.downloadUrl,
                context.filesDir.absolutePath,
                model.getFileName(),
                headers = headers,
            )
        _uiState.update {
            it.copy(
                downloadModelDialogState = it.downloadModelDialogState.copy(
                    downloadId = downloadId,
                    isPaused = false
                )
            )
        }
        ketch
            .observeDownloadById(downloadId)
            .flowOn(Dispatchers.IO)
            .collect { downloadModel ->
                downloadModel?.let { ketchDownload ->
                    when (ketchDownload.status) {
                        Status.QUEUED -> {
                            _uiState.update {
                                it.copy(
                                    downloadModelDialogState = it.downloadModelDialogState.copy(
                                        isDialogVisible = true
                                    ),
                                )
                            }
                        }

                        Status.PROGRESS -> {
                            _uiState.update {
                                it.copy(
                                    downloadModelDialogState = it.downloadModelDialogState.copy(
                                        progress = ketchDownload.progress
                                    ),
                                )
                            }
                        }

                        Status.SUCCESS -> {
                            _uiState.update {
                                it.copy(
                                    downloadModelDialogState = it.downloadModelDialogState.copy(
                                        isDialogVisible = false
                                    ),
                                )
                            }
                            onEvent(LocalModelsUIEvent.OnUseModelClick(model))
                            withContext(Dispatchers.Main) {
                                Toast
                                    .makeText(
                                        context,
                                        "Model downloaded successfully",
                                        Toast.LENGTH_LONG,
                                    ).show()
                            }
                        }

                        Status.FAILED -> {
                            _uiState.update {
                                it.copy(
                                    downloadModelDialogState = it.downloadModelDialogState.copy(
                                        isDialogVisible = false
                                    ),
                                )
                            }
                            withContext(Dispatchers.Main) {
                                Log.e("APP", "Failure reason ${ketchDownload.failureReason}")
                                Toast
                                    .makeText(
                                        context,
                                        "Model downloaded failed ${ketchDownload.failureReason}",
                                        Toast.LENGTH_LONG,
                                    ).show()
                            }
                        }

                        Status.STARTED -> {
                            _uiState.update {
                                it.copy(
                                    downloadModelDialogState =
                                        it.downloadModelDialogState.copy(
                                            isDialogVisible = true,
                                            showProgress = true,
                                        ),
                                )
                            }
                        }

                        else -> {}
                    }
                }
            }
    }
}
