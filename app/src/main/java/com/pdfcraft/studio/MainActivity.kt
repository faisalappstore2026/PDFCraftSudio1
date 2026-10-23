package com.pdfcraft.studio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.pdfcraft.studio.core.update.VersionCheckRepository
import com.pdfcraft.studio.core.update.VersionStatus
import com.pdfcraft.studio.ui.update.UpdateRequiredScreen
import com.pdfcraft.studio.ui.update.VersionCheckLoading
import com.pdfcraft.studio.ui.update.openUpdateUrl
import com.pdfcraft.studio.core.update.VersionPolicy
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.pdfcraft.studio.navigation.PdfCraftNavGraph
import com.pdfcraft.studio.ui.theme.PDFCraftStudioTheme

class MainActivity : ComponentActivity() {

    private var openPdfUri by mutableStateOf<String?>(null)

    // null = checking; gate only on a real UPDATE_REQUIRED decision
    private var versionStatus by mutableStateOf<VersionStatus?>(null)
    private var updatePolicy by mutableStateOf<VersionPolicy?>(null)
    private var checkJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openPdfUri = extractPdfUri(intent)
        enableEdgeToEdge()
        runVersionCheck()
        setContent {
            PDFCraftStudioTheme {
                Surface(modifier = Modifier) {
                    when (versionStatus) {
                        // Check runs in background; app opens at once (fail-open).
                        VersionStatus.UPDATE_REQUIRED -> UpdateRequiredScreen(
                            onUpdateNow = { openUpdateUrl(this@MainActivity, updatePolicy?.updateUrl) },
                            onRetry = { runVersionCheck() },
                            message = updatePolicy?.updateMessage
                        )
                        else -> PdfCraftNavGraph(openPdfUri = openPdfUri)
                    }
                }
            }
        }
    }

    private fun runVersionCheck() {
        if (checkJob?.isActive == true) return
        versionStatus = null
        checkJob = lifecycleScope.launch {
            val result = VersionCheckRepository(applicationContext).checkWithPolicy()
            updatePolicy = result.policy
            versionStatus = result.status
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openPdfUri = extractPdfUri(intent)
    }

    private fun extractPdfUri(intent: Intent?): String? {
        if (intent == null) return null
        if (intent.action != Intent.ACTION_VIEW && intent.action != Intent.ACTION_SEND) {
            // Still allow VIEW only for open-with
        }
        if (intent.action != Intent.ACTION_VIEW) return null
        val uri: Uri = intent.data ?: return null

        // Ensure we can read for this session (file managers usually grant this)
        try {
            val takeFlags = intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (takeFlags != 0) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    android.util.Log.w("MainActivity", "Open-with URI is not persistable", e)
                    // Non-persistable grant — openFileDescriptor still works this session
                }
            }
        } catch (_: Exception) {
        }
        return uri.toString()
    }
}
