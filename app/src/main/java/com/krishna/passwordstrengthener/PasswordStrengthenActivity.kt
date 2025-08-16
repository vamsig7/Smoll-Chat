package com.krishna.passwordstrengthener

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.krishna.passwordstrengthener.core.AppServices
import com.krishna.passwordstrengthener.navigation.AppNavHost
import com.krishna.smollm.GGUFReader
import com.krishna.smollm.SmolLM
import com.krishna.passwordstrengthener.ui.components.AppProgressDialog
import com.krishna.passwordstrengthener.ui.components.hideProgressDialog
import com.krishna.passwordstrengthener.ui.components.setProgressDialogText
import com.krishna.passwordstrengthener.ui.components.setProgressDialogTitle
import com.krishna.passwordstrengthener.ui.components.showProgressDialog
import com.krishna.passwordstrengthener.ui.theme.SmolLMAndroidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PasswordStrengthenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize lightweight service locator for this POC
        AppServices.init(applicationContext)
        setContent {
            SmolLMAndroidTheme {
                AppProgressDialog()
                AppNavHost()
            }
        }
    }
}

@Composable
private fun PasswordStrengthenScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // SmolLM instance lives across recompositions
    val smolLM = remember { SmolLM() }

    var modelPath by remember { mutableStateOf<String?>(null) }
    var modelLoaded by remember { mutableStateOf(false) }
    var isLoadingModel by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }

    var weakPassword by remember { mutableStateOf("") }
    var strongPassword by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    var selectedModelPath by remember { mutableStateOf("") }
    // File picker for .gguf
    val modelPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {

                try {
                    isLoadingModel = true
                    error = null
                    status = "Copying model…"

                    // Persist permission for future app sessions
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    val copied = withContext(Dispatchers.IO) {
                        copyModelFile(uri, context)
                    }

                    if (copied == null) {
                        throw RuntimeException("Model copy failed")
                    }
                    selectedModelPath = copied
                    status = "Loading model…"

                    // Minimal default inference params
                    smolLM.load(copied, SmolLM.InferenceParams())
                    // Add focused password-strengthening system prompt
//                    smolLM.addSystemPrompt(HARDCODED_SYSTEM_PROMPT)

                    modelPath = copied.substringAfterLast("/")
                    modelLoaded = true
                    status = "Model loaded"
                } catch (t: Throwable) {
                    hideProgressDialog()
                    error = t.message ?: t.toString()
                    modelLoaded = false
                } finally {
                    isLoadingModel = false
                }
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            // Ensure native resources are released
            try { smolLM.close() } catch (_: Throwable) {}
        }
    }

    SmolLMAndroidTheme {
        // Global progress dialog overlay
        AppProgressDialog()
        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Local model: ${modelPath ?: "Not selected"}")

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        modelPicker.launch(arrayOf("application/octet-stream", "*/*"))
                    }, enabled = !isLoadingModel && !isGenerating) {
                        Text("Pick model (.gguf)")
                    }

                    if (isLoadingModel) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }

                OutlinedTextField(
                    value = weakPassword,
                    onValueChange = { weakPassword = it },
                    label = { Text("Weak password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        scope.launch {
                            isGenerating = true
                            strongPassword = ""
                            error = null
                            status = "Generating…"
                            try {
                                try {
                                    smolLM.verifyHandle()
                                } catch (e: Throwable) {
                                    smolLM.load(selectedModelPath, SmolLM.InferenceParams())
                                }

                                withContext(Dispatchers.IO) {
                                    var acc = ""
                                    val disallowedSuffixes = listOf(
                                        "<", "<|", "<|e", "<|en", "<|end", "<|end|",
                                        "<|u", "<|us", "<|use", "<|user", "<|user|",
                                        "<|s", "<|sy", "<|sys", "<|syst",
                                        "<|syste", "<|system", "<|system|"
                                    )
                                    val beforeStart = System.currentTimeMillis()
                                    var firstInference : Long? = null
                                    smolLM.getResponseAsFlow(weakPassword).collect { piece ->
                                        if (firstInference == null) {
                                            firstInference = System.currentTimeMillis()
                                            Log.d("LLM", "First inference :" + (firstInference!! - beforeStart))
                                        }
                                        acc += piece
                                        withContext(Dispatchers.Main) {
                                            if (disallowedSuffixes.none { suffix -> acc.endsWith(suffix) }) {
                                                strongPassword = acc
                                            }
                                        }
                                    }
                                }
                                smolLM.close()
                                status = "Done"
                            } catch (t: Throwable) {
                                error = t.message ?: t.toString()
                                status = "Error"
                            } finally {
                                isGenerating = false
                            }
                        }
                    },
                    enabled = modelLoaded && weakPassword.isNotBlank() && !isLoadingModel && !isGenerating
                ) {
                    Text("Strengthen password")
                }

                if (strongPassword.isNotBlank()) {
                    Text(
                        text = "Strong password: $strongPassword",
                        modifier = Modifier.weight(1f)
                            .verticalScroll(rememberScrollState())
                        )
                }

                if (status.isNotBlank()) {
                    Text(text = status, style = MaterialTheme.typography.bodySmall)
                }

                if (error != null) {
                    Text(text = "Error: $error", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

suspend fun copyModelFile(
    uri: Uri,
    context: Context
): String? {
    var fileName = ""
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        fileName = cursor.getString(nameIndex)
    }
    if (fileName.isNotEmpty()) {
        setProgressDialogTitle(context.getString(R.string.dialog_progress_copy_model_title))
        setProgressDialogText(
            context.getString(R.string.dialog_progress_copy_model_text, fileName),
        )
        showProgressDialog()
        var filePath : String?
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri).use { inputStream ->
                FileOutputStream(File(context.filesDir, fileName)).use { outputStream ->
                    inputStream?.copyTo(outputStream)
                }
            }
            filePath = File(context.filesDir, fileName).absolutePath
            val ggufReader = GGUFReader()
            ggufReader.load(filePath!!)
            val contextSize = ggufReader.getContextSize() ?: SmolLM.DefaultInferenceParams.contextSize
            val chatTemplate = ggufReader.getChatTemplate() ?: SmolLM.DefaultInferenceParams.chatTemplate
            withContext(Dispatchers.Main) {
                hideProgressDialog()
            }
        }
        return filePath
    } else {
        Toast
            .makeText(
                context,
                context.getString(R.string.toast_invalid_file),
                Toast.LENGTH_SHORT,
            ).show()
    }
    return null
}

private const val HARDCODED_SYSTEM_PROMPT = """
You are a password-strengthening assistant. Given a weak password, output a significantly stronger password that preserves the recognizable intent of the original. Follow strictly:
- Output only the strengthened password, nothing else.
- At least 12 characters (extend if needed), include upper and lower case, digits, and symbols.
- Avoid dictionary words, common patterns, trivial suffixes (e.g., 123, !), or simple substitutions only. Be creative and unpredictable while still memorable.
"""
