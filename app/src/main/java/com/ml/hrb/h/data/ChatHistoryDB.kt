// Author: Harshal R. Bisen
// Database operations for persisting chat sessions and message histories.

package com.ml.hrb.h.data

import io.objectbox.kotlin.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.Single

@Single
class ChatHistoryDB {
    private val sessionBox = ObjectBoxStore.store.boxFor(ChatSession::class.java)
    private val messageBox = ObjectBoxStore.store.boxFor(ChatMessage::class.java)

    fun createSession(title: String): Long {
        val session = ChatSession(title = title, createdTime = System.currentTimeMillis())
        return sessionBox.put(session)
    }

    fun deleteSession(sessionId: Long) {
        sessionBox.remove(sessionId)
        val messageIds = messageBox.query(ChatMessage_.sessionId.equal(sessionId)).build().findIds()
        messageBox.removeByIds(messageIds.toList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllSessionsFlow(): Flow<MutableList<ChatSession>> {
        return sessionBox
            .query(ChatSession_.id.notNull())
            .orderDesc(ChatSession_.createdTime)
            .build()
            .flow()
            .flowOn(Dispatchers.IO)
    }

    fun addMessage(message: ChatMessage): Long {
        return messageBox.put(message)
    }

    fun getMessagesForSession(sessionId: Long): List<ChatMessage> {
        return messageBox.query(ChatMessage_.sessionId.equal(sessionId))
            .order(ChatMessage_.timestamp)
            .build()
            .find()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getMessagesForSessionFlow(sessionId: Long): Flow<MutableList<ChatMessage>> {
        return messageBox.query(ChatMessage_.sessionId.equal(sessionId))
            .order(ChatMessage_.timestamp)
            .build()
            .flow()
            .flowOn(Dispatchers.IO)
    }
}
