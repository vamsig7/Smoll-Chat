package com.krishna.passwordstrengthener.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krishna.passwordstrengthener.core.AppServices
import com.krishna.passwordstrengthener.model.ModelRepository
import com.krishna.passwordstrengthener.session.RowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val session = AppServices.sessionRepository
    val items = session.items

    private val _isImprovingAll = MutableStateFlow(false)
    val isImprovingAll: StateFlow<Boolean> = _isImprovingAll.asStateFlow()

    // Common token suffixes to ignore when constructing the final response
    private val disallowedSuffixes = listOf(
        "<", "<|", "<|e", "<|en", "<|end", "<|end|",
        "<|u", "<|us", "<|use", "<|user", "<|user|",
        "<|s", "<|sy", "<|sys", "<|syst",
        "<|syste", "<|system", "<|system|"
    )

    init {
        // Initialize session passwords and kick off async scoring on first use
        if (items.value.isEmpty()) {
            val list = AppServices.weakPasswordProvider.getSessionPasswords()
            session.setInitial(list)
            scoreAllAsync()
        }
    }

    fun scoreAllAsync() {
        viewModelScope.launch(Dispatchers.Default) {
            // sequential scoring to keep it simple (can optimize with limited concurrency)
            items.value.forEach { item ->
                val score = AppServices.scoringService.computeScore(item.original)
                session.setScore(item.id, score)
            }
        }
    }

    fun improveOne(id: String) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                session.setRowState(id, RowState.Waiting)
                session.setRowState(id, RowState.Loading)

                if (!ensureModelReady()) {
                    session.setRowState(id, RowState.Idle)
                    return@launch
                }

                val item = items.value.firstOrNull { it.id == id } ?: return@launch
                val result = runLLM(item.original)
                val score = AppServices.scoringService.computeScore(result)
                session.addCandidateAndSelect(id, result, score)
                session.setRowState(id, RowState.Success(System.currentTimeMillis()))

                // auto hide success after 3s
                viewModelScope.launch(Dispatchers.Default) {
                    delay(3000)
                    session.setRowState(id, RowState.Idle)
                }
            } catch (_: Throwable) {
                session.setRowState(id, RowState.Idle)
            }
        }
    }

    fun improveAll() {
        if (_isImprovingAll.value) return
        viewModelScope.launch(Dispatchers.Default) {
            _isImprovingAll.value = true
            try {
                val snapshot = items.value.toList()
                snapshot.forEach { item ->
                    session.setRowState(item.id, RowState.Waiting)

                }
                snapshot.forEach { item ->
                    improveOneBlocking(item.id)
                }
            } finally {
                _isImprovingAll.value = false
            }
        }
    }

    // Ensures model path is set and model is loaded. Returns false if unavailable.
    private suspend fun ensureModelReady(): Boolean {
        val context = AppServices.requireAppContext()
        val modelPath = ModelRepository.getModelPath(context)
        if (modelPath.isNullOrBlank()) return false
        AppServices.modelClient.ensureLoadedOrReload(modelPath)
        return true
    }

    // Unified LLM call with suffix filtering. Returns the last "safe" text.
    private suspend fun runLLM(prompt: String): String {
        var acc = ""
        var result = ""
        val beforeStart = System.currentTimeMillis()
        var firstInference : Long? = null


        AppServices.modelClient.getResponseAsFlow(prompt).collect { piece ->
            if (firstInference == null) {
                firstInference = System.currentTimeMillis()
                Log.d("LLM", "First inference :" + (firstInference!! - beforeStart))
            }
            acc += piece
            if (disallowedSuffixes.none { suffix -> acc.endsWith(suffix) }) {
                result = acc
            }
        }
        AppServices.modelClient.close()
        return result.ifBlank { acc }
    }

    private suspend fun improveOneBlocking(id: String) {
        session.setRowState(id, RowState.Loading)

        if (!ensureModelReady()) {
            session.setRowState(id, RowState.Idle)
            return
        }

        val item = items.value.firstOrNull { it.id == id } ?: return
        val text = runLLM(item.original)
        val score = AppServices.scoringService.computeScore(text)
        session.addCandidateAndSelect(id, text, score)
        session.setRowState(id, RowState.Success(System.currentTimeMillis()))
        delay(3000)
        session.setRowState(id, RowState.Idle)
    }

    fun selectCandidate(rowId: String, candidateId: String) {
        session.selectCandidate(rowId, candidateId)
    }
}
