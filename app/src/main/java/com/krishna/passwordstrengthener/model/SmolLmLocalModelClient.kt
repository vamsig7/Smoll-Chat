package com.krishna.passwordstrengthener.model

import com.krishna.passwordstrengthener.core.AppServices
import com.krishna.smollm.SmolLM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SmolLmLocalModelClient : LocalModelClient {
    private var smolLM: SmolLM? = SmolLM()
    private var loadedPath: String? = null

    override suspend fun load(modelPath: String, params: LocalModelClient.InferenceParams) {
        val instance = smolLM ?: SmolLM().also { smolLM = it }
        // Map minimal params if available (ignored for now; rely on SmolLM defaults)
        instance.load(modelPath, SmolLM.InferenceParams())
        instance.setStopWords(listOf("<|end|>", "<|user|>", "<|system|>"))
        loadedPath = modelPath
    }

    override fun isLoaded(): Boolean = try {
        smolLM?.verifyHandle()
        true
    } catch (_: Throwable) {
        false
    }

    override suspend fun ensureLoadedOrReload(modelPath: String, params: LocalModelClient.InferenceParams) = withContext(Dispatchers.Default){
        val instance = smolLM ?: SmolLM().also { smolLM = it }
        try {
            instance.verifyHandle()
        } catch (_: Throwable) {
            // reload
            instance.load(modelPath.ifBlank { loadedPath ?: modelPath }, SmolLM.InferenceParams())
            instance.setStopWords(listOf("<|end|>", "<|user|>", "<|system|>"))
            loadedPath = modelPath
        }
    }

    override fun setStopWords(words: List<String>) {
        smolLM?.setStopWords(words)
    }

    override fun addSystemPrompt(prompt: String) {
        // If available in SmolLM; ignore if not
        try {
            smolLM?.addSystemPrompt(prompt)
        } catch (_: Throwable) {
            // no-op if method not supported
        }
    }

    override fun getResponseAsFlow(prompt: String): Flow<String> {
        val instance = smolLM ?: throw IllegalStateException("Model not loaded")

        return instance.getResponseAsFlow(prompt)
    }

    override fun close() {
        try { smolLM?.close() } catch (_: Throwable) {}
        smolLM = null
        loadedPath = null
    }
}
