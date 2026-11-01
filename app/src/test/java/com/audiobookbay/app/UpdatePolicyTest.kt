package com.audiobookbay.app

import com.audiobookbay.app.data.remote.*
import com.audiobookbay.app.domain.repository.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class UpdatePolicyTest {
    private val update = AppUpdate("0.1.1", "Notes", "AudioBay-0.1.1-preview.apk", "", "", 123)
    private fun version(text: String) = requireNotNull(ReleaseVersion.parse(text))
    private fun release(tag: String, draft: Boolean = false, apk: Boolean = true) = buildJsonObject {
        put("tag_name", tag); put("draft", draft); put("prerelease", true); put("body", "Notes")
        putJsonArray("assets") {
            for (name in listOfNotNull(if (apk) "AudioBay-${tag.removePrefix("v")}-preview.apk" else null, "SHA256SUMS.txt")) {
                addJsonObject {
                    put("name", name); put("state", "uploaded"); put("size", 123)
                    put("browser_download_url", "https://github.com/NW0RK/AudioBay/releases/download/$tag/$name")
                }
            }
        }
    }

    @Test fun semanticOrderingAndPrereleases() {
        assertTrue(version("0.1.10") > version("0.1.9"))
        assertTrue(version("1.0.0") > version("1.0.0-rc.9"))
        assertTrue(version("1.0.0-rc.10") > version("1.0.0-rc.2"))
        assertTrue(version("1.0.0-alpha.1") < version("1.0.0-alpha.beta"))
        assertEquals(version("v0.1.1+build.1"), version("0.1.1"))
        listOf("1.0", "1.00.0", "1.0.0-", "1.0.0-alpha..1", "1.0.0-01").forEach { assertNull(ReleaseVersion.parse(it)) }
    }

    @Test fun newestCompatiblePublishedPreviewWins() {
        val body = JsonArray(listOf(release("v0.1.1"), release("v0.1.10"), release("v2.0.0", draft = true),
            release("v3.0.0", apk = false), release("invalid"))).toString()
        assertEquals("0.1.10", UpdateReleaseParser.select(body, "0.1.0", listOf("arm64-v8a"))?.version)
        assertNull(UpdateReleaseParser.select(body, "4.0.0", listOf("arm64-v8a")))
        assertNull(UpdateReleaseParser.select(body, "0.1.0", listOf("armeabi-v7a")))
    }

    @Test fun checksumMustBeUniqueAndMatchExactFilename() {
        val hash = "ab".repeat(32)
        assertEquals(hash, UpdateReleaseParser.checksum("$hash  ${update.assetName}\n", update.assetName))
        assertThrows(IllegalArgumentException::class.java) { UpdateReleaseParser.checksum("$hash other.apk", update.assetName) }
        assertThrows(IllegalArgumentException::class.java) { UpdateReleaseParser.checksum("$hash ${update.assetName}\n$hash ${update.assetName}", update.assetName) }
        assertFalse(UpdateReleaseParser.validAssetUrl("https://github.com.evil.test/NW0RK/AudioBay/releases/download/v1/x.apk"))
        assertFalse(UpdateReleaseParser.validAssetUrl("http://github.com/NW0RK/AudioBay/releases/download/v1/x.apk"))
    }

    @Test fun automaticChecksAreDailyButClockRollbackRecovers() {
        assertTrue(UpdateReleaseParser.shouldCheck(100, 0))
        assertFalse(UpdateReleaseParser.shouldCheck(200, 100))
        assertTrue(UpdateReleaseParser.shouldCheck(86_400_100, 100))
        assertTrue(UpdateReleaseParser.shouldCheck(50, 100))
    }

    @Test fun apkIdentityMustMatchAndIncreaseVersion() {
        val installed = ApkIdentity("com.audiobookbay.app", "0.1.0", 1, 26, setOf("arm64-v8a"), setOf("signer"))
        val valid = installed.copy(version = "0.1.1", code = 2)
        fun validate(apk: ApkIdentity) = validateUpdateIdentity(apk, update, installed, 34, setOf("arm64-v8a"))
        validate(valid)
        listOf(valid.copy(packageName = "other"), valid.copy(code = 1), valid.copy(version = "9.0.0")).forEach {
            assertEquals(UpdateError.PACKAGE, assertThrows(UpdateFailure::class.java) { validate(it) }.kind)
        }
        assertEquals(UpdateError.SIGNATURE, assertThrows(UpdateFailure::class.java) { validate(valid.copy(signers = setOf("other"))) }.kind)
        assertEquals(UpdateError.INCOMPATIBLE, assertThrows(UpdateFailure::class.java) { validate(valid.copy(minSdk = 99)) }.kind)
        assertEquals(UpdateError.INCOMPATIBLE, assertThrows(UpdateFailure::class.java) { validate(valid.copy(abis = setOf("x86"))) }.kind)
    }
}
