// Author: Harshal R. Bisen
// Data model and utilities for sentence embeddings.

package com.ml.hrb.h.domain

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import java.nio.LongBuffer
import kotlin.math.max
import kotlin.math.min

class SentenceEmbedding {
    private lateinit var ortEnv: OrtEnvironment
    private lateinit var ortSession: OrtSession
    private lateinit var tokenizer: WordPieceTokenizer
    private var normalizeEmbeddings: Boolean = false
    private var outputTensorName: String = "last_hidden_state"

    fun init(
        modelPath: String,
        tokenizerBytes: ByteArray,
        useTokenTypeIds: Boolean = false,
        outputTensorName: String = "last_hidden_state",
        normalizeEmbeddings: Boolean = false
    ) {
        ortEnv = OrtEnvironment.getEnvironment()
        ortSession = ortEnv.createSession(modelPath)
        this.normalizeEmbeddings = normalizeEmbeddings
        this.outputTensorName = outputTensorName

        val vocab = parseTokenizerVocab(tokenizerBytes)
        tokenizer = WordPieceTokenizer(vocab)
    }

    fun encode(text: String): FloatArray {
        val tokenized = tokenizer.tokenize(text)
        val seqLen = tokenized.inputIds.size

        val shape = longArrayOf(1, seqLen.toLong())

        val inputIdsTensor = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(tokenized.inputIds), shape)
        val attentionMaskTensor = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(tokenized.attentionMask), shape)

        val inputs = mapOf(
            "input_ids" to inputIdsTensor,
            "attention_mask" to attentionMaskTensor
        )

        ortSession.run(inputs).use { result ->
            val outputValue = result.get(outputTensorName).orElse(result.get(0))
            val outputTensor = outputValue as OnnxTensor
            val floatBuffer = outputTensor.floatBuffer
            val data = FloatArray(floatBuffer.remaining())
            floatBuffer.get(data)
            
            val shape = outputTensor.info.shape
            val hiddenDim = if (shape.size == 3) shape[2].toInt() else shape[1].toInt()

            val pooled = if (shape.size == 3) {
                meanPooling(data, tokenized.attentionMask, seqLen, hiddenDim)
            } else {
                data
            }

            return if (normalizeEmbeddings) {
                normalize(pooled)
            } else {
                pooled
            }
        }
    }

    private fun parseTokenizerVocab(bytes: ByteArray): Map<String, Int> {
        val jsonString = String(bytes, Charsets.UTF_8)
        val root = Json.parseToJsonElement(jsonString).jsonObject
        val model = root["model"]?.jsonObject ?: throw Exception("Invalid tokenizer.json format")
        val vocab = model["vocab"]?.jsonObject ?: throw Exception("Vocab object not found in tokenizer.json")
        val vocabMap = HashMap<String, Int>()
        vocab.forEach { (key, value) ->
            vocabMap[key] = value.jsonPrimitive.int
        }
        return vocabMap
    }

    private fun meanPooling(
        lastHiddenState: FloatArray,
        attentionMask: LongArray,
        seqLen: Int,
        hiddenDim: Int
    ): FloatArray {
        val meanEmbedding = FloatArray(hiddenDim)
        var sumMask = 0f
        for (i in 0 until seqLen) {
            val maskVal = attentionMask[i].toFloat()
            sumMask += maskVal
            val offset = i * hiddenDim
            for (j in 0 until hiddenDim) {
                meanEmbedding[j] += lastHiddenState[offset + j] * maskVal
            }
        }
        if (sumMask > 0f) {
            for (j in 0 until hiddenDim) {
                meanEmbedding[j] /= sumMask
            }
        }
        return meanEmbedding
    }

    private fun normalize(vector: FloatArray): FloatArray {
        var squareSum = 0.0
        for (value in vector) {
            squareSum += value * value
        }
        val norm = Math.sqrt(squareSum).toFloat()
        if (norm > 0f) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
        return vector
    }
}

class TokenizedOutput(
    val inputIds: LongArray,
    val attentionMask: LongArray
)

class WordPieceTokenizer(private val vocab: Map<String, Int>) {
    private val unkToken = "[UNK]"
    private val unkTokenId = vocab[unkToken] ?: 100
    private val clsTokenId = vocab["[CLS]"] ?: 101
    private val sepTokenId = vocab["[SEP]"] ?: 102

    fun tokenize(text: String, maxLength: Int = 128): TokenizedOutput {
        val inputIds = ArrayList<Int>()

        val words = splitWordsAndPunctuation(text.lowercase())

        inputIds.add(clsTokenId)

        for (word in words) {
            val subTokens = wordPieceTokenize(word)
            for (subToken in subTokens) {
                val id = vocab[subToken] ?: unkTokenId
                inputIds.add(id)
            }
        }

        inputIds.add(sepTokenId)

        val finalInputIds = if (inputIds.size > maxLength) {
            inputIds.subList(0, maxLength - 1) + sepTokenId
        } else {
            inputIds
        }

        val attentionMask = List(finalInputIds.size) { 1 }

        return TokenizedOutput(
            inputIds = finalInputIds.map { it.toLong() }.toLongArray(),
            attentionMask = attentionMask.map { it.toLong() }.toLongArray()
        )
    }

    private fun splitWordsAndPunctuation(text: String): List<String> {
        val result = ArrayList<String>()
        val currentWord = StringBuilder()
        for (char in text) {
            if (char.isWhitespace()) {
                if (currentWord.isNotEmpty()) {
                    result.add(currentWord.toString())
                    currentWord.clear()
                }
            } else if (isPunctuation(char)) {
                if (currentWord.isNotEmpty()) {
                    result.add(currentWord.toString())
                    currentWord.clear()
                }
                result.add(char.toString())
            } else {
                currentWord.append(char)
            }
        }
        if (currentWord.isNotEmpty()) {
            result.add(currentWord.toString())
        }
        return result
    }

    private fun isPunctuation(char: Char): Boolean {
        val type = Character.getType(char).toByte()
        return type == Character.CONNECTOR_PUNCTUATION ||
                type == Character.DASH_PUNCTUATION ||
                type == Character.START_PUNCTUATION ||
                type == Character.END_PUNCTUATION ||
                type == Character.INITIAL_QUOTE_PUNCTUATION ||
                type == Character.FINAL_QUOTE_PUNCTUATION ||
                type == Character.OTHER_PUNCTUATION ||
                (char in "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~")
    }

    private fun wordPieceTokenize(word: String): List<String> {
        if (vocab.containsKey(word)) {
            return listOf(word)
        }
        val subTokens = ArrayList<String>()
        var start = 0
        var isBad = false
        while (start < word.length) {
            var end = word.length
            var curSubstr = ""
            while (start < end) {
                var substr = word.substring(start, end)
                if (start > 0) {
                    substr = "##$substr"
                }
                if (vocab.containsKey(substr)) {
                    curSubstr = substr
                    break
                }
                end--
            }
            if (curSubstr.isEmpty()) {
                isBad = true
                break
            }
            subTokens.add(curSubstr)
            start = end
        }
        if (isBad) {
            return listOf(unkToken)
        }
        return subTokens
    }
}
