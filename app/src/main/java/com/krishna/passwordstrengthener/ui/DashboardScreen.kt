package com.krishna.passwordstrengthener.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.krishna.passwordstrengthener.core.AppServices
import com.krishna.passwordstrengthener.model.ModelRepository
import com.krishna.passwordstrengthener.navigation.NavRoutes
import com.krishna.passwordstrengthener.scoring.ScoreState
import com.krishna.passwordstrengthener.session.RowState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, vm: DashboardViewModel = viewModel()) {
    val items by vm.items.collectAsState()
    val improvingAll by vm.isImprovingAll.collectAsState()
    val modelName = ModelRepository.getModelFileName(AppServices.requireAppContext()) ?: "No model"
    val context = LocalContext.current
    var sheetOpenId by remember { mutableStateOf<String?>(null) }

    // Guard on resume: if model path missing, redirect to Model Picker
    LaunchedEffect(Unit) {
        val path = ModelRepository.getModelPath(context)
        if (path.isNullOrBlank()) {
            navController.navigate(NavRoutes.ModelPicker) {
                popUpTo(NavRoutes.Dashboard) { inclusive = true }
            }
        }
    }

    Scaffold(
        bottomBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { vm.scoreAllAsync() }) { Text("Recompute Scores") }
                Button(onClick = { vm.improveAll() }, enabled = !improvingAll) { Text(if (improvingAll) "Improving…" else "Improve All") }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(modifier = Modifier.padding(16.dp), text = "Model: $modelName", style = MaterialTheme.typography.titleMedium)
                TextButton(
                    onClick = {
                    // Reselect model
                    navController.navigate(NavRoutes.ModelPicker) {
                        popUpTo(NavRoutes.Dashboard) { inclusive = true }
                    }
                }) { Text("Reselect") }
            }
            HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(bottom = 10.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = item.original)
                            Crossfade(
                                targetState = item.currentImproved,
                                label = "improvedCrossfade"
                            ) { improved ->
                                if (improved != null) {
                                    Text("Improved: $improved")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                when (val s = item.score) {
                                    is ScoreState.Loading -> {
                                        AssistChip(
                                            onClick = {},
                                            enabled = false,
                                            label = { Text("score: computing…") })
                                    }

                                    is ScoreState.Value -> {
                                        val chipColor = when (s.score0to4) {
                                            0 -> Color(0xFFD32F2F) // red
                                            1 -> Color(0xFFF57C00) // orange
                                            2 -> Color(0xFFFBC02D) // yellow
                                            3 -> Color(0xFF7CB342) // light green
                                            else -> Color(0xFF388E3C) // green
                                        }
                                        AssistChip(
                                            onClick = {}, enabled = false,
                                            label = { Text("score: ${s.score0to4}/4") },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = chipColor,
                                                labelColor = Color.White
                                            )
                                        )
                                    }
                                }
                                val stateText = when (val st = item.state) {
                                    RowState.Idle -> ""
                                    RowState.Waiting -> "⏳ queued"
                                    RowState.Loading -> "🔄 improving…"
                                    is RowState.Success -> "✅"
                                }
                                if (stateText.isNotBlank()) Text(
                                    stateText,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TextButton(
                                    onClick = { vm.improveOne(item.id) },
                                    enabled = item.state !is RowState.Loading
                                ) {
                                    Text("Improve")
                                }
                                TextButton(
                                    onClick = { sheetOpenId = item.id },
                                    enabled = item.candidates.isNotEmpty()
                                ) {
                                    Text("History")
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
        // History bottom sheet
        val selectedItem = items.firstOrNull { it.id == sheetOpenId }
        if (sheetOpenId != null && selectedItem != null) {
            ModalBottomSheet(onDismissRequest = { sheetOpenId = null }) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("History for: ${selectedItem.original}", style = MaterialTheme.typography.titleMedium)
                    selectedItem.candidates.forEach { cand ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(cand.value)
                                val chipColor = when (cand.score0to4) {
                                    0 -> Color(0xFFD32F2F)
                                    1 -> Color(0xFFF57C00)
                                    2 -> Color(0xFFFBC02D)
                                    3 -> Color(0xFF7CB342)
                                    else -> Color(0xFF388E3C)
                                }
                                AssistChip(
                                    onClick = {}, enabled = false,
                                    label = { Text("score: ${cand.score0to4}/4") },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = chipColor, labelColor = Color.White)
                                )
                            }
                            val label = if (cand.isSelected) "Selected" else "Select"
                            Button(onClick = { vm.selectCandidate(selectedItem.id, cand.id) }) { Text(label) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
