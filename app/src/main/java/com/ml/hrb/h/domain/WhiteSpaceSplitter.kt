// Author: Harshal R. Bisen
// Utility to split text into chunks based on whitespaces.

package com.ml.hrb.h.domain

import kotlin.math.max
import kotlin.math.min

class WhiteSpaceSplitter {
    companion object {
        fun createChunks(
            docText: String,
            chunkSize: Int,
            chunkOverlap: Int,
            separatorParagraph: String = "\n\n",
            separator: String = " ",
            strategy: String = "whitespace",
        ): List<String> {
            val textChunks = ArrayList<String>()
            docText.split(separatorParagraph).forEach { paragraph ->
                var currChunk = ""
                val chunks = ArrayList<String>()
                
                val elements = if (strategy == "sentence") {
                    paragraph.split(Regex("(?<=[.!?])\\s+"))
                } else {
                    paragraph.split(separator)
                }
                
                val delimiter = if (strategy == "sentence") " " else separator

                elements.forEach { element ->
                    if (element.trim().isEmpty()) return@forEach
                    val newChunk =
                        currChunk +
                            (
                                if (currChunk.isNotEmpty()) {
                                    delimiter
                                } else {
                                    ""
                                }
                            ) +
                            element
                    if (newChunk.length <= chunkSize) {
                        currChunk = newChunk
                    } else {
                        if (currChunk.isNotEmpty()) {
                            chunks.add(currChunk)
                        }
                        currChunk = element
                    }
                }
                if (currChunk.isNotEmpty()) {
                    chunks.add(currChunk)
                }

                val overlappingChunks = ArrayList<String>(chunks)
                if (chunkOverlap > 1 && chunks.isNotEmpty()) {
                    for (i in 0..<chunks.size - 1) {
                        val overlapStart = max(0, chunks[i].length - chunkOverlap)
                        val overlapEnd = min(chunkOverlap, chunks[i + 1].length)
                        overlappingChunks.add(
                            chunks[i].substring(overlapStart) +
                                " " +
                                chunks[i + 1].substring(0..<overlapEnd),
                        )
                    }
                }

                textChunks.addAll(overlappingChunks)
            }
            return textChunks
        }
    }
}
