package com.krishna.passwordstrengthener.scoring

import com.nulabinc.zxcvbn.Zxcvbn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface ScoreState {
    data object Loading : ScoreState
    data class Value(val score0to4: Int) : ScoreState
}

interface PasswordScoringService {
    suspend fun computeScore(password: String): Int
}

class DefaultPasswordScoringService : PasswordScoringService {
    private val zxcvbn by lazy { Zxcvbn() }

    override suspend fun computeScore(password: String): Int {
        return withContext(Dispatchers.Default) {
            try {
                zxcvbn.measure(password).score.coerceIn(0, 4)
            } catch (_: Throwable) {
                0
            }
        }
    }
}
