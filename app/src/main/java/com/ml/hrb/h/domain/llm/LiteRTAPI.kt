// Author: Harshal R. Bisen
// Implementation for interacting with the LiteRT (TensorFlow Lite) API for local LLM inference.

package com.ml.hrb.h.domain.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class LiteRTAPI : LLMInferenceAPI() {
    private lateinit var llmInference: LlmInference
    var isLoaded = false
    var loadedModelPath: String? = null

    class PartialProgressListener(
        private val onPartialResponseGenerated: (String) -> Unit,
        private val onSuccess: (String) -> Unit,
    ) : ProgressListener<String> {
        private var result = ""

        override fun run(
            partialResult: String?,
            done: Boolean,
        ) {
            if (done) {
                onSuccess(result)
                result = ""
            } else {
                result += partialResult ?: ""
                onPartialResponseGenerated(result)
            }
        }
    }

    fun load(
        context: Context,
        modelPath: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val taskOptions =
            LlmInference.LlmInferenceOptions
                .builder()
                .setModelPath(modelPath)
                .setMaxTopK(64)
                .setMaxTokens(2048)
                .build()
        llmInference = LlmInference.createFromOptions(context, taskOptions)
        isLoaded = true
        loadedModelPath = modelPath
        onSuccess()
    }

    override suspend fun getResponse(prompt: String): String? =
        withContext(Dispatchers.Default) {
            Log.e("APP", "Prompt given: $prompt")
            llmInference.generateResponse(prompt)
        }

    override suspend fun getResponseStream(prompt: String): Flow<String> =
        callbackFlow {
            Log.e("APP", "Streaming Prompt given locally: $prompt")
            try {
                llmInference.generateResponseAsync(prompt, object : ProgressListener<String> {
                    override fun run(partialResult: String?, done: Boolean) {
                        if (!partialResult.isNullOrEmpty()) {
                            trySend(partialResult)
                        }
                        if (done) {
                            close()
                        }
                    }
                })
            } catch (e: Exception) {
                close(e)
            }
            awaitClose { }
        }

    fun unload() {
        llmInference.close()
        isLoaded = false
        loadedModelPath = null
    }
}
