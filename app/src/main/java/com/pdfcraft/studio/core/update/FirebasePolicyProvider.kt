package com.pdfcraft.studio.core.update

import android.content.Context
import android.content.pm.ApplicationInfo
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Step 2: reads the version policy from Firebase Remote Config.
 * Keys: minimum_supported_version_code, latest_version_code,
 * latest_version_name, update_message, update_url.
 * A failed fetch is NetworkError, never a made-up Success.
 */
class FirebasePolicyProvider(context: Context) : VersionPolicyProvider {
    private val appContext = context.applicationContext

    override suspend fun fetch(): PolicyFetchResult {
        val rc = try {
            FirebaseRemoteConfig.getInstance()
        } catch (e: Exception) {
            return PolicyFetchResult.NetworkError
        }
        val debuggable = (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (debuggable) 0L else 3600L)
            .setFetchTimeoutInSeconds(10L)
            .build()
        awaitTask(rc.setConfigSettingsAsync(settings))
        val fetched = awaitTask(rc.fetchAndActivate())
        if (!fetched) return PolicyFetchResult.NetworkError
        val min = rc.getValue(KEY_MIN)
        if (min.source != FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) {
            return PolicyFetchResult.NotConfigured
        }
        return try {
            RemotePolicyParser.parse(
                minCode = rc.getLong(KEY_MIN),
                latestCode = rc.getLong(KEY_LATEST),
                latestName = rc.getString(KEY_LATEST_NAME),
                message = rc.getString(KEY_MESSAGE),
                url = rc.getString(KEY_URL),
                now = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            PolicyFetchResult.Invalid
        }
    }

    /** true when the task completed successfully (result value ignored). */
    private suspend fun <T> awaitTask(task: Task<T>): Boolean =
        suspendCancellableCoroutine { cont ->
            task.addOnCompleteListener { t ->
                if (cont.isActive) cont.resume(t.isSuccessful)
            }
        }

    companion object {
        const val KEY_MIN = "minimum_supported_version_code"
        const val KEY_LATEST = "latest_version_code"
        const val KEY_LATEST_NAME = "latest_version_name"
        const val KEY_MESSAGE = "update_message"
        const val KEY_URL = "update_url"
    }
}
