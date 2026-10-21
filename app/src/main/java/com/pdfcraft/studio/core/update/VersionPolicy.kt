package com.pdfcraft.studio.core.update

/**
 * Step 1: Minimum Version Check (pure logic, no Android imports).
 *
 * Rule: installed versionCode < minimumSupportedVersionCode => UPDATE_REQUIRED.
 * updateRequired is stored for future steps; the decision uses the min code only.
 */
data class VersionPolicy(
    val minimumSupportedVersionCode: Long,
    val latestVersionCode: Long,
    val updateRequired: Boolean,
    val fetchedAt: Long
) {
    fun isValid(): Boolean =
        minimumSupportedVersionCode > 0L &&
            latestVersionCode > 0L &&
            latestVersionCode >= minimumSupportedVersionCode &&
            fetchedAt > 0L
}

enum class VersionStatus { SUPPORTED, UPDATE_REQUIRED }

fun compareVersion(installedVersionCode: Long, minimumSupportedVersionCode: Long): VersionStatus =
    if (installedVersionCode < minimumSupportedVersionCode) VersionStatus.UPDATE_REQUIRED
    else VersionStatus.SUPPORTED

sealed class PolicyFetchResult {
    class Success(val policy: VersionPolicy) : PolicyFetchResult()
    object NetworkError : PolicyFetchResult()
    object Invalid : PolicyFetchResult()
    object NotConfigured : PolicyFetchResult()
}

/** Contract for a future online policy source (Step 2). */
interface VersionPolicyProvider {
    suspend fun fetch(): PolicyFetchResult
}

/**
 * The only provider in Step 1. No remote source exists yet, so it
 * always reports NotConfigured and the app runs as SUPPORTED.
 * Step 2 (pending): replace with a real provider.
 */
class NoRemotePolicyProvider : VersionPolicyProvider {
    override suspend fun fetch(): PolicyFetchResult = PolicyFetchResult.NotConfigured
}

object VersionDecision {
    /** A cached policy older than this is ignored (stale). */
    const val MAX_CACHE_AGE_MS: Long = 7L * 24L * 60L * 60L * 1000L

    class Outcome(val status: VersionStatus, val policyToCache: VersionPolicy?)

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
            return Outcome(compareVersion(installedVersionCode, p.minimumSupportedVersionCode), p)
        }
        if (cached != null && cached.isValid() && isFresh(cached, now, maxAgeMs)) {
            return Outcome(compareVersion(installedVersionCode, cached.minimumSupportedVersionCode), null)
        }
        return Outcome(VersionStatus.SUPPORTED, null)
    }
}
