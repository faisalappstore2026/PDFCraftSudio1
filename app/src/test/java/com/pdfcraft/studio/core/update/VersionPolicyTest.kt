package com.pdfcraft.studio.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionPolicyTest {
    private val now = 1_000_000_000L

    private fun policy(min: Long, latest: Long = min, at: Long = now) =
        VersionPolicy(min, latest, true, at)

    @Test fun installed1_min2_updateRequired() =
        assertEquals(VersionStatus.UPDATE_REQUIRED, compareVersion(1L, 2L))

    @Test fun installed2_min2_supported() =
        assertEquals(VersionStatus.SUPPORTED, compareVersion(2L, 2L))

    @Test fun installed3_min2_supported() =
        assertEquals(VersionStatus.SUPPORTED, compareVersion(3L, 2L))

    @Test fun installed2_min3_updateRequired() =
        assertEquals(VersionStatus.UPDATE_REQUIRED, compareVersion(2L, 3L))

    @Test fun malformed_isInvalid_andIgnored() {
        assertFalse(policy(0L).isValid())
        assertFalse(policy(3L, 2L).isValid())
        assertFalse(policy(2L, 3L, 0L).isValid())
        assertTrue(policy(2L, 3L).isValid())
        val out = VersionDecision.decide(1L, PolicyFetchResult.Success(policy(5L, 2L)), null, now)
        assertEquals(VersionStatus.SUPPORTED, out.status)
        assertNull(out.policyToCache)
    }

    @Test fun networkError_freshCache_used() {
        val out = VersionDecision.decide(1L, PolicyFetchResult.NetworkError, policy(2L, 2L, now - 1000L), now)
        assertEquals(VersionStatus.UPDATE_REQUIRED, out.status)
    }

    @Test fun networkError_staleCache_allowed() {
        val old = now - VersionDecision.MAX_CACHE_AGE_MS - 1L
        val out = VersionDecision.decide(1L, PolicyFetchResult.NetworkError, policy(2L, 2L, old), now)
        assertEquals(VersionStatus.SUPPORTED, out.status)
    }

    @Test fun noPolicyEver_allowed() {
        val out = VersionDecision.decide(1L, PolicyFetchResult.NotConfigured, null, now)
        assertEquals(VersionStatus.SUPPORTED, out.status)
    }

    @Test fun remote_validValues_success_withMessageAndUrl() {
        val r = RemotePolicyParser.parse(2L, 3L, "1.1.0", " Please update ", "https://play.google.com/store/apps/details?id=com.pdfcraft.studio", now)
        assertTrue(r is PolicyFetchResult.Success)
        val p = (r as PolicyFetchResult.Success).policy
        assertEquals(2L, p.minimumSupportedVersionCode)
        assertEquals("Please update", p.updateMessage)
        assertEquals("https://play.google.com/store/apps/details?id=com.pdfcraft.studio", p.updateUrl)
        assertEquals(VersionStatus.UPDATE_REQUIRED, VersionDecision.decide(1L, r, null, now).status)
        assertEquals(VersionStatus.SUPPORTED, VersionDecision.decide(2L, r, null, now).status)
    }

    @Test fun remote_missingOrZero_isInvalid() {
        assertTrue(RemotePolicyParser.parse(0L, 0L, "", "", "", now) is PolicyFetchResult.Invalid)
        assertTrue(RemotePolicyParser.parse(-1L, 3L, "", "", "", now) is PolicyFetchResult.Invalid)
        assertTrue(RemotePolicyParser.parse(2L, 0L, "", "", "", now) is PolicyFetchResult.Invalid)
    }

    @Test fun remote_latestBelowMin_isInvalid_andAllowed() {
        val r = RemotePolicyParser.parse(5L, 2L, "", "", "", now)
        assertTrue(r is PolicyFetchResult.Invalid)
        assertEquals(VersionStatus.SUPPORTED, VersionDecision.decide(1L, r, null, now).status)
    }

    @Test fun remote_badUrl_isDropped_policyStillValid() {
        val r = RemotePolicyParser.parse(2L, 2L, "", "", "https://evil.example.com/app.apk", now)
        assertEquals("", (r as PolicyFetchResult.Success).policy.updateUrl)
    }

    @Test fun updateUrl_validation() {
        assertTrue(isAllowedUpdateUrl("https://play.google.com/store/apps/details?id=com.pdfcraft.studio"))
        assertTrue(isAllowedUpdateUrl("market://details?id=com.pdfcraft.studio"))
        assertFalse(isAllowedUpdateUrl(""))
        assertFalse(isAllowedUpdateUrl(null))
        assertFalse(isAllowedUpdateUrl("http://play.google.com/store/apps/details?id=x"))
        assertFalse(isAllowedUpdateUrl("https://play.google.com.evil.com/x"))
        assertFalse(isAllowedUpdateUrl("https://user@play.google.com/x"))
        assertFalse(isAllowedUpdateUrl("javascript:alert(1)"))
        assertFalse(isAllowedUpdateUrl("https://example.com/update.apk"))
    }

    @Test fun networkError_noCache_allowed() {
        val out = VersionDecision.decide(1L, PolicyFetchResult.NetworkError, null, now)
        assertEquals(VersionStatus.SUPPORTED, out.status)
        assertNull(out.usedPolicy)
    }
}
