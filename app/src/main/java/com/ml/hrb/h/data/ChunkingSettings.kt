// Author: Harshal R. Bisen
// Storage and configuration manager for document chunking settings.

package com.ml.hrb.h.data

import android.content.Context
import android.content.SharedPreferences
import org.koin.core.annotation.Single

@Single
class ChunkingSettings(
    context: Context,
) {
    private val sharedPrefFileName = "chunking_settings"
    private val keyChunkSize = "chunk_size"
    private val keyChunkOverlap = "chunk_overlap"
    private val keySplitStrategy = "split_strategy"

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(sharedPrefFileName, Context.MODE_PRIVATE)

    fun saveSettings(chunkSize: Int, chunkOverlap: Int, strategy: String) {
        sharedPreferences.edit()
            .putInt(keyChunkSize, chunkSize)
            .putInt(keyChunkOverlap, chunkOverlap)
            .putString(keySplitStrategy, strategy)
            .apply()
    }

    fun getChunkSize(): Int = sharedPreferences.getInt(keyChunkSize, 500)

    fun getChunkOverlap(): Int = sharedPreferences.getInt(keyChunkOverlap, 50)

    fun getSplitStrategy(): String = sharedPreferences.getString(keySplitStrategy, "whitespace") ?: "whitespace"
}
