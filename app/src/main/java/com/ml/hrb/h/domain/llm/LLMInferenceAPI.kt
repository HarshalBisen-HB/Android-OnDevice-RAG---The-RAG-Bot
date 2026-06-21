// Author: Harshal R. Bisen
// Interface for running Large Language Model inference.

package com.ml.hrb.h.domain.llm

import kotlinx.coroutines.flow.Flow

abstract class LLMInferenceAPI {
    abstract suspend fun getResponse(prompt: String): String?
    abstract suspend fun getResponseStream(prompt: String): Flow<String>
}
