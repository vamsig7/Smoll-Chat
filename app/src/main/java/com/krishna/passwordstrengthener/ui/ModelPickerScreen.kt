package com.krishna.passwordstrengthener.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.krishna.passwordstrengthener.PASSWORD_STRENGTH_SYSTEM_PROMPT
import com.krishna.passwordstrengthener.copyModelFile
import com.krishna.passwordstrengthener.core.AppServices
import com.krishna.passwordstrengthener.model.ModelRepository
import com.krishna.passwordstrengthener.navigation.NavRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ModelPickerScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoadingModel by remember { mutableStateOf(false) }
    var selectedModelPath by remember { mutableStateOf("") }

    val modelPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                try {
                    isLoadingModel = true
                    // Persist permission for future sessions
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    val copied = withContext(Dispatchers.IO) { copyModelFile(uri, context) }
                    if (copied == null) throw RuntimeException("Model copy failed")

                    selectedModelPath = copied

                    // Load model via Service Locator
                    AppServices.modelClient.load(copied)
                    AppServices.modelClient.setStopWords(listOf("<|end|>", "<|user|>", "<|system|>"))
//                    AppServices.modelClient.addSystemPrompt(PASSWORD_STRENGTH_SYSTEM_PROMPT)

                    val fileName = copied.substringAfterLast('/')
                    ModelRepository.saveModel(context, copied, fileName)
                    Toast.makeText(context, "Model loaded: $fileName", Toast.LENGTH_SHORT).show()

                    navController.navigate(NavRoutes.Dashboard) {
                        popUpTo(NavRoutes.ModelPicker) { inclusive = true }
                    }
                } catch (t: Throwable) {
                    Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isLoadingModel = false
                }
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            // no-op here; App root will manage lifecycle
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Pick a model (.gguf)", style = MaterialTheme.typography.titleLarge)
            Text("Select a local GGUF model. We'll copy it to app storage and load it for local inference.")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    modelPicker.launch(arrayOf("application/octet-stream", "*/*"))
                }, enabled = !isLoadingModel) {
                    Icon(Icons.Default.InsertDriveFile, contentDescription = null)
                    Text("  Pick model (.gguf)")
                }
                if (isLoadingModel) {
                    CircularProgressIndicator()
                }
            }

            if (selectedModelPath.isNotBlank()) {
                Text("Selected: ${selectedModelPath.substringAfterLast('/')}")
            }
        }
    }
}
