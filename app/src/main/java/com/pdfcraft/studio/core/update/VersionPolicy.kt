package com.pdfcraft.studio.core.update

import java.net.URI

/**
 * Minimum Version Check (pure logic, no Android imports).
 *
 * Rule: installed versionCode < minimumSupportedVersionCode => UPDATE_REQUIRED.
 * Step 2: the policy also carries an optional message and update URL from
 * Firebase Remote Config. The decision still uses the min code only.
 */
data class VersionPolicy(
    val minimumSupportedVersionCode: Long,
    val latestVersionCode: Long,
    val updateRequired: Boolean,
    val fetchedAt: Long,
    val latestVersionName: String = "",
    val updateMessage: String = "",
    val updateUrl: String = ""
) {
    fun isValid(): Boolean =
        minimumSupportedVersionCode > 0L &&
            latestVersionCode > 0L &&
            latestVersionCode >= minimumSupportedVersionCode &&
            fetchedAt > 0L
}

enum class VersionStatus { SUPPORTED, UPDATE_REQUIRED }

/** Status plus the policy that produced it (null when no policy was used). */
class VersionCheckResult(val status: VersionStatus, val policy: VersionPolicy?)

fun compareVersion(installedVersionCode: Long, minimumSupportedVersionCode: Long): VersionStatus =
    if (installedVersionCode < minimumSupportedVersionCode) VersionStatus.UPDATE_REQUIRED
    else VersionStatus.SUPPORTED

sealed class PolicyFetchResult {
    class Success(val policy: VersionPolicy) : PolicyFetchResult()
    object NetworkError : PolicyFetchResult()
    object Invalid : PolicyFetchResult()
    object NotConfigured : PolicyFetchResult()
}

/** Contract for an online policy source. */
interface VersionPolicyProvider {
    suspend fun fetch(): PolicyFetchResult
}

/** Provider that never has a policy. Kept for tests and as a safe fallback. */
class NoRemotePolicyProvider : VersionPolicyProvider {
    override suspend fun fetch(): PolicyFetchResult = PolicyFetchResult.NotConfigured
}

object RemotePolicyParser {
    const val MAX_TEXT = 500

    /**
     * Builds a policy from raw remote values. Missing values arrive as 0 / "",
     * which are not a real policy, so they return Invalid (never Success).
     */
    fun parse(
        minCode: Long,
        latestCode: Long,
        latestName: String,
        message: String,
        url: String,
        now: Long
    ): PolicyFetchResult {
        val p = VersionPolicy(
            minimumSupportedVersionCode = minCode,
            latestVersionCode = latestCode,
            updateRequired = false,
            fetchedAt = now,
            latestVersionName = latestName.trim().take(MAX_TEXT),
            updateMessage = message.trim().take(MAX_TEXT),
            updateUrl = if (isAllowedUpdateUrl(url)) url.trim() else ""
        )
        return if (p.isValid()) PolicyFetchResult.Success(p) else PolicyFetchResult.Invalid
    }
}

/**
 * Only the official store may be opened from a remote value:
 * https://play.google.com/... or market://...
 */
fun isAllowedUpdateUrl(raw: String?): Boolean {
    val s = raw?.trim() ?: return false
    if (s.isEmpty() || s.length > 2000) return false
    if (s.any { it.isWhitespace() }) return false
    val uri = try {
        URI(s)
    } catch (e: Exception) {
        return false
    }
    val scheme = uri.scheme?.lowercase() ?: return false
    return when (scheme) {
        "https" -> uri.host?.lowercase() == "play.google.com" && uri.userInfo == null &&
            (uri.port == -1 || uri.port == 443)
        "market" -> true
        else -> false
    }
}

object VersionDecision {
    /** A cached policy older than this is ignored (stale). */
    const val MAX_CACHE_AGE_MS: Long = 7L * 24L * 60L * 60L * 1000L

    class Outcome(
        val status: VersionStatus,
        val policyToCache: VersionPolicy?,
        val usedPolicy: VersionPolicy? = null
    )

    fun isFresh(policy: VersionPolicy, now: Long, maxAgeMs: Long = MAX_CACHE_AGE_MS): Boolean {
        val age = now - policy.fetchedAt
        return age >= 0L && age <= maxAgeMs
    }

    /**
     * Rules:
     * - Valid fresh policy from provider: compare, and cache it.
     * - Malformed policy: ignored, never treated as update-required.
     * - Network error / invalid / not configured: use cache only if valid and fresh.
     * - No valid policy at all, or stale cache: allow (fail-open). Next launch retries.
     */
    fun decide(
        installedVersionCode: Long,
        fetch: PolicyFetchResult,
        cached: VersionPolicy?,
        now: Long,
        maxAgeMs: Long = MAX_CACHE_AGE_MS
    ): Outcome {
        if (fetch is PolicyFetchResult.Success && fetch.policy.isValid()) {
            val p = fetch.policy
            return Outcome(compareVersion(installedVersionCode, p.minimumSupportedVersionCode), p, p)
        }
        if (cached != null && cached.isValid() && isFresh(cached, now, maxAgeMs)) {
            return Outcome(compareVersion(installedVersionCode, cached.minimumSupportedVersionCode), null, cached)
        }
        return Outcome(VersionStatus.SUPPORTED, null, null)
    }
}
