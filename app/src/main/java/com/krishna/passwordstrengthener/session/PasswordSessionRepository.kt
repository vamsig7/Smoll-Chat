package com.krishna.passwordstrengthener.session

import com.krishna.passwordstrengthener.scoring.ScoreState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class PasswordSessionRepository {
    private val _items = MutableStateFlow<List<PasswordItemUi>>(emptyList())
    val items: StateFlow<List<PasswordItemUi>> = _items

    fun setInitial(originals: List<String>) {
        _items.value = originals.map { orig ->
            PasswordItemUi(
                id = UUID.randomUUID().toString(),
                original = orig,
                score = ScoreState.Loading,
                currentImproved = null,
                state = RowState.Idle,
                candidates = emptyList()
            )
        }
    }

    fun setScore(id: String, score0to4: Int) {
        _items.update { list ->
            list.map { item ->
                if (item.id == id) item.copy(score = ScoreState.Value(score0to4)) else item
            }
        }
    }

    fun setRowState(id: String, state: RowState) {
        _items.update { list ->
            list.map { item -> if (item.id == id) item.copy(state = state) else item }
        }
    }

    fun addCandidateAndSelect(id: String, value: String, score0to4: Int) {
        _items.update { list ->
            list.map { item ->
                if (item.id == id) {
                    val newCandidate = CandidateUi(
                        id = UUID.randomUUID().toString(),
                        value = value,
                        score0to4 = score0to4,
                        isSelected = true
                    )
                    val updated = item.candidates.map { it.copy(isSelected = false) } + newCandidate
                    item.copy(
                        candidates = updated,
                        currentImproved = value
                    )
                } else item
            }
        }
    }

    fun selectCandidate(id: String, candidateId: String) {
        _items.update { list ->
            list.map { item ->
                if (item.id == id) {
                    val updated = item.candidates.map { c -> c.copy(isSelected = (c.id == candidateId)) }
                    val current = updated.firstOrNull { it.isSelected }?.value
                    item.copy(candidates = updated, currentImproved = current)
                } else item
            }
        }
    }
}
