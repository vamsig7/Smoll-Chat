package com.krishna.passwordstrengthener.model

import kotlinx.coroutines.flow.Flow

interface LocalModelClient {
    data class InferenceParams(
        val contextSize: Int? = null,
        val chatTemplate: String? = null
    )

    suspend fun load(modelPath: String, params: InferenceParams = InferenceParams())
    fun isLoaded(): Boolean
    suspend fun ensureLoadedOrReload(modelPath: String, params: InferenceParams = InferenceParams())
    fun setStopWords(words: List<String>)
    fun addSystemPrompt(prompt: String)
    fun getResponseAsFlow(prompt: String): Flow<String>
    fun close()
}
