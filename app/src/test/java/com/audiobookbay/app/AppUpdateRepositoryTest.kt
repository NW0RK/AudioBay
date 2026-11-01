package com.audiobookbay.app

import android.content.Context
import android.content.SharedPreferences
import com.audiobookbay.app.data.remote.*
import com.audiobookbay.app.data.repository.GitHubAppUpdateRepository
import com.audiobookbay.app.domain.repository.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import okhttp3.*
import okhttp3.Call
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateRepositoryTest {
    @get:Rule val folder = TemporaryFolder()
    private val context = mockk<Context>()
    private val prefs = mockk<SharedPreferences>()
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val client = mockk<OkHttpClient>()
    private val builder = mockk<OkHttpClient.Builder>(relaxed = true)
    private val verifier = mockk<UpdateApkVerifier>()
    private val device = mockk<UpdateDevice>()
    private val calls = mutableListOf<Call>()
    private var lastAttempt = 0L
    private val asset = "AudioBay-0.1.2-preview.apk"
    private val base = "https://github.com/NW0RK/AudioBay/releases/download/v0.1.2"
    private val release = """[{"tag_name":"v0.1.2","draft":false,"prerelease":true,"body":"Notes","assets":[{"name":"$asset","state":"uploaded","size":3,"browser_download_url":"$base/$asset"},{"name":"SHA256SUMS.txt","state":"uploaded","size":90,"browser_download_url":"$base/SHA256SUMS.txt"}]}]"""
    private var response: (Request, Callback, Call) -> Unit = { request, callback, call ->
        val body = when {
            request.url.encodedPath.endsWith("SHA256SUMS.txt") -> "${"ab".repeat(32)}  $asset"
            request.url.encodedPath.endsWith(".apk") -> "apk"
            else -> release
        }
        callback.onResponse(call, http(request, 200, body))
    }

    private fun http(request: Request, code: Int, body: String) = Response.Builder().request(request)
        .protocol(Protocol.HTTP_1_1).code(code).message("Response").body(body.toResponseBody()).build()

    private fun repository(dispatcher: CoroutineDispatcher): GitHubAppUpdateRepository {
        every { context.cacheDir } returns folder.root
        every { context.getSharedPreferences("app_updates", Context.MODE_PRIVATE) } returns prefs
        every { prefs.getLong(any(), any()) } answers { lastAttempt }
        every { prefs.edit() } returns editor
        every { editor.putLong(any(), any()) } answers { lastAttempt = secondArg(); editor }
        every { device.abis } returns listOf("arm64-v8a")
        every { client.newBuilder() } returns builder
        every { builder.interceptors() } returns mutableListOf()
        every { builder.networkInterceptors() } returns mutableListOf()
        every { builder.followSslRedirects(any()) } returns builder
        every { builder.callTimeout(any<Long>(), any()) } returns builder
        every { builder.readTimeout(any<Long>(), any()) } returns builder
        every { builder.build() } returns client
        every { client.newCall(any()) } answers {
            val request = firstArg<Request>()
            mockk<Call>(relaxed = true).also { call ->
                calls += call
                every { call.enqueue(any()) } answers { response(request, firstArg(), call) }
            }
        }
        coEvery { verifier.verify(any(), any(), any()) } returns Unit
        return GitHubAppUpdateRepository(context, client, verifier, device, dispatcher)
    }

    @Test fun automaticChecksThrottleAndManualChecksBypass() = runTest {
        val repo = repository(StandardTestDispatcher(testScheduler))
        repo.check(automatic = true)
        assertEquals(UpdatePhase.AVAILABLE, repo.state.value.phase)
        assertTrue(repo.state.value.announce)
        assertEquals(1, calls.size)
        repo.dismissAnnouncement()
        repo.check(automatic = true)
        assertEquals(1, calls.size)
        repo.check()
        assertEquals(2, calls.size)
        assertFalse(repo.state.value.announce)
    }

    @Test fun offlineAutomaticCheckIsQuietButManualCheckReportsFailure() = runTest {
        val repo = repository(StandardTestDispatcher(testScheduler))
        response = { _, callback, call -> callback.onFailure(call, IOException("Offline")) }
        repo.check(automatic = true)
        assertEquals(UpdatePhase.IDLE, repo.state.value.phase)
        repo.check()
        assertEquals(UpdateError.NETWORK, repo.state.value.error)
    }

    @Test fun rateLimitsAndMalformedResponsesAreRecoverable() = runTest {
        val repo = repository(StandardTestDispatcher(testScheduler))
        response = { request, callback, call -> callback.onResponse(call, http(request, 429, "")) }
        repo.check()
        assertEquals(UpdateError.RATE_LIMIT, repo.state.value.error)
        response = { request, callback, call -> callback.onResponse(call, http(request, 200, "invalid")) }
        repo.check()
        assertEquals(UpdateError.RELEASE, repo.state.value.error)
    }

    @Test fun downloadVerifiesAndRetainsReadyFileForInstallation() = runTest {
        val repo = repository(StandardTestDispatcher(testScheduler))
        repo.check()
        repo.download()
        runCurrent()
        assertEquals(UpdatePhase.READY, repo.state.value.phase)
        val file = repo.installationFile()
        assertEquals("apk", file?.readText())
        coVerify(exactly = 2) { verifier.verify(any(), any(), "ab".repeat(32)) }
        assertFalse(java.io.File(folder.root, "updates/update.part.apk").exists())
    }

    @Test fun corruptOrDifferentlySignedUpdatesCannotBeInstalled() = runTest {
        val repo = repository(StandardTestDispatcher(testScheduler))
        repo.check()
        coEvery { verifier.verify(any(), any(), any()) } throws UpdateFailure(UpdateError.SIGNATURE)
        repo.download(); runCurrent()
        assertEquals(UpdateError.SIGNATURE, repo.state.value.error)
        assertNull(repo.installationFile())
        assertFalse(java.io.File(folder.root, "updates/update.part.apk").exists())
    }

    @Test fun cancelClosesRequestDeletesPartialAndAllowsRetry() = runTest {
        val repo = repository(StandardTestDispatcher(testScheduler))
        repo.check()
        val successfulResponse = response
        response = { _, _, _ -> /* Remain pending until cancelled. */ }
        repo.download(); runCurrent()
        assertEquals(UpdatePhase.DOWNLOADING, repo.state.value.phase)
        repo.cancelDownload(); runCurrent()
        assertEquals(UpdatePhase.AVAILABLE, repo.state.value.phase)
        verify { calls.last().cancel() }
        assertFalse(java.io.File(folder.root, "updates/update.part.apk").exists())
        response = successfulResponse
        repo.download(); runCurrent()
        assertEquals(UpdatePhase.READY, repo.state.value.phase)
    }

    @Test fun missingChecksumNeverDownloadsTheApk() = runTest {
        val repo = repository(StandardTestDispatcher(testScheduler))
        repo.check()
        response = { request, callback, call -> callback.onResponse(call, http(request, 200, "invalid checksum")) }
        repo.download(); runCurrent()
        assertEquals(UpdateError.CHECKSUM, repo.state.value.error)
        assertEquals(2, calls.size)
        assertFalse(java.io.File(folder.root, "updates/update.apk").exists())
    }

    @Test fun storageFailureIsDistinctFromNetworkFailure() = runTest {
        val repo = repository(StandardTestDispatcher(testScheduler))
        runCurrent()
        java.io.File(folder.root, "updates").writeText("Blocks directory creation")
        repo.check()
        repo.download(); runCurrent()
        assertEquals(UpdateError.STORAGE, repo.state.value.error)
    }

    @Test fun readyUpdateSurvivesProcessRecreationWithoutAnotherDownload() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repo = repository(dispatcher)
        repo.check(); repo.download(); runCurrent()
        assertEquals(UpdatePhase.READY, repo.state.value.phase)
        val requestCount = calls.size
        val reopened = repository(dispatcher)
        runCurrent()
        assertEquals(UpdatePhase.READY, reopened.state.value.phase)
        assertEquals("apk", reopened.installationFile()?.readText())
        assertEquals(requestCount, calls.size)
    }

    @Test fun apkChecksumVerificationUsesActualBytesBeforePackageInspection() = runTest {
        val apk = folder.newFile("candidate.apk").apply { writeText("apk") }
        val realVerifier = UpdateApkVerifier(context, StandardTestDispatcher(testScheduler))
        try {
            realVerifier.verify(apk, AppUpdate("0.1.2", "", asset, "", "", 3), "00".repeat(32))
            fail("Corrupt update must fail")
        } catch (e: UpdateFailure) { assertEquals(UpdateError.CHECKSUM, e.kind) }
    }
}
