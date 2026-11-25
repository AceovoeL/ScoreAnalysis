package com.yg.scoreanalysis

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.yg.scoreanalysis.data.AppData
import com.yg.scoreanalysis.data.ScoreRepository
import com.yg.scoreanalysis.data.UpdateManager
import com.yg.scoreanalysis.ui.theme.ScoreAnalysisTheme
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = ScoreRepository(applicationContext)
        val updateManager = UpdateManager(applicationContext)
        setContent {
            val data by repository.state.collectAsState()
            ScoreAnalysisTheme(darkMode = data.settings.darkMode) {
                // 每次冷启动检查一次：有新版本才弹窗，失败静默
                androidx.compose.runtime.LaunchedEffect(Unit) { updateManager.checkOnLaunch() }
                RootContent(repository, updateManager)
            }
        }
    }
}

@Composable
private fun RootContent(repository: ScoreRepository, updateManager: UpdateManager) {
    val context = LocalContext.current
    val json = remember { Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true } }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    val text = json.encodeToString(AppData.serializer(), repository.state.value)
                    out.write(text.toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                }
                if (!text.isNullOrBlank()) {
                    val parsed = json.decodeFromString(AppData.serializer(), text)
                    if (parsed.version == 1) repository.replaceAll(parsed)
                }
            }
        }
    }

    ScoreAnalysisApp(
        repository = repository,
        updateManager = updateManager,
        onExport = { exportLauncher.launch("score-analysis-backup.json") },
        onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
    )
}
