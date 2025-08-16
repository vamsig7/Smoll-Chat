package com.krishna.passwordstrengthener.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

private val progressDialogVisibleState = mutableStateOf(false)
private val progressDialogText = mutableStateOf("")
private val progressDialogTitle = mutableStateOf("")

@Composable
fun AppProgressDialog() {
    val isVisible by remember { progressDialogVisibleState }
    if (isVisible) {
        Surface {
            Dialog(onDismissRequest = { /* Progress dialogs are non-cancellable */ }) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(8.dp)),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 24.dp),
                    ) {
                        Text(text = progressDialogTitle.value)
                        Spacer(modifier = Modifier.padding(4.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            text = progressDialogText.value,
                            textAlign = TextAlign.Center,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

fun setProgressDialogText(message: String) {
    progressDialogText.value = message
}

fun setProgressDialogTitle(title: String) {
    progressDialogTitle.value = title
}

fun showProgressDialog() {
    progressDialogVisibleState.value = true
}

fun hideProgressDialog() {
    progressDialogVisibleState.value = false
}
