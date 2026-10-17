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
import com.pdfcraft.studio.navigation.PdfCraftNavGraph
import com.pdfcraft.studio.ui.theme.PDFCraftStudioTheme

class MainActivity : ComponentActivity() {

    private var openPdfUri by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openPdfUri = extractPdfUri(intent)
        enableEdgeToEdge()
        setContent {
            PDFCraftStudioTheme {
                Surface(modifier = Modifier) {
                    PdfCraftNavGraph(openPdfUri = openPdfUri)
                }
            }
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
                } catch (_: SecurityException) {
                    // Non-persistable grant — openFileDescriptor still works this session
                }
            }
        } catch (_: Exception) {
        }
        return uri.toString()
    }
}
