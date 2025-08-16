package com.krishna.passwordstrengthener.session

import com.krishna.passwordstrengthener.scoring.ScoreState

sealed class RowState {
    data object Idle : RowState()
    data object Waiting : RowState() // queued
    data object Loading : RowState() // generating
    data class Success(val timestamp: Long) : RowState()
}

data class CandidateUi(
    val id: String,
    val value: String,
    val score0to4: Int,
    val isSelected: Boolean
)

data class PasswordItemUi(
    val id: String,
    val original: String,
    val score: ScoreState,
    val currentImproved: String?,
    val state: RowState,
    val candidates: List<CandidateUi>
)
