package com.pdfcraft.studio.core.update

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

/**
 * Reads the installed versionCode, keeps the last valid policy in
 * SharedPreferences, and runs the startup check with a timeout.
 */
class VersionCheckRepository(
    context: Context,
    private val provider: VersionPolicyProvider = NoRemotePolicyProvider()
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun installedVersionCode(): Long = try {
        val info = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        PackageInfoCompat.getLongVersionCode(info)
    } catch (e: Exception) {
        -1L
    }

    suspend fun check(): VersionStatus = withContext(Dispatchers.IO) {
        try {
            val installed = installedVersionCode()
            if (installed <= 0L) {
                VersionStatus.SUPPORTED
            } else {
                val fetch = try {
                    withTimeoutOrNull(FETCH_TIMEOUT_MS) { provider.fetch() }
                        ?: PolicyFetchResult.NetworkError
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    PolicyFetchResult.NetworkError
                }
                val outcome = VersionDecision.decide(
                    installed, fetch, readCache(), System.currentTimeMillis()
                )
                outcome.policyToCache?.let { writeCache(it) }
                outcome.status
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("VersionCheck", "check failed", e)
            VersionStatus.SUPPORTED
        }
    }

    private fun readCache(): VersionPolicy? {
        if (!prefs.contains(KEY_MIN)) return null
        val p = VersionPolicy(
            minimumSupportedVersionCode = prefs.getLong(KEY_MIN, 0L),
            latestVersionCode = prefs.getLong(KEY_LATEST, 0L),
            updateRequired = prefs.getBoolean(KEY_REQUIRED, false),
            fetchedAt = prefs.getLong(KEY_FETCHED, 0L)
        )
        return if (p.isValid()) p else null
    }

    private fun writeCache(p: VersionPolicy) {
        prefs.edit()
            .putLong(KEY_MIN, p.minimumSupportedVersionCode)
            .putLong(KEY_LATEST, p.latestVersionCode)
            .putBoolean(KEY_REQUIRED, p.updateRequired)
            .putLong(KEY_FETCHED, p.fetchedAt)
            .apply()
    }

    companion object {
        const val FETCH_TIMEOUT_MS: Long = 3000L
        private const val PREFS = "version_policy"
        private const val KEY_MIN = "min_code"
        private const val KEY_LATEST = "latest_code"
        private const val KEY_REQUIRED = "update_required"
        private const val KEY_FETCHED = "fetched_at"
    }
}
