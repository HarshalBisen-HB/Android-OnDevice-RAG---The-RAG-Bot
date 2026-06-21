// Author: Harshal R. Bisen
// Core data models and entities used across the application.

package com.ml.hrb.h.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

@Entity
data class Chunk(
    @Id var chunkId: Long = 0,
    @Index var docId: Long = 0,
    var docFileName: String = "",
    var chunkData: String = "",
    @HnswIndex(dimensions = 384) var chunkEmbedding: FloatArray = floatArrayOf(),
)

@Entity
data class Document(
    @Id var docId: Long = 0,
    var docText: String = "",
    var docFileName: String = "",
    var docAddedTime: Long = 0,
)

@Entity
data class ChatSession(
    @Id var id: Long = 0,
    var title: String = "",
    var createdTime: Long = 0,
)

@Entity
data class ChatMessage(
    @Id var id: Long = 0,
    var sessionId: Long = 0,
    var role: String = "", // "user" or "model"
    var text: String = "",
    var timestamp: Long = 0,
)

data class RetrievedContext(
    val fileName: String,
    val context: String,
    val similarityScore: Float = 0f,
)
