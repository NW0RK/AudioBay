package com.audiobookbay.app

import com.audiobookbay.app.data.storage.DownloadFolderSettings
import com.audiobookbay.app.data.storage.DownloadNetworkSettings
import com.audiobookbay.app.domain.repository.TorrentDownloadManager
import com.audiobookbay.app.ui.screens.settings.SettingsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val folders = mockk<DownloadFolderSettings>()
    private val manager = mockk<TorrentDownloadManager>()
    private val network = mockk<DownloadNetworkSettings>()
    private val keys = mockk<com.audiobookbay.app.data.storage.ApiKeySettings>()

    private val formatting = mockk<com.audiobookbay.app.data.storage.SearchFormattingSettings>()

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { formatting.enabled } returns flowOf(false)
        every { folders.folder } returns flowOf(null)
        every { network.wifiOnly } returns flowOf(false)
        every { keys.configured } returns kotlinx.coroutines.flow.MutableStateFlow(false)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = SettingsViewModel(folders, manager, network, keys, formatting)

    @Test fun saveAndRemoveKeyWithoutExposingTheCredential() = runTest(dispatcher) {
        coEvery { keys.save(any()) } returns Unit
        val vm = vm()
        vm.saveApiKey("test-key")
        assertTrue(vm.keyBusy.value)
        runCurrent()
        coVerify(exactly = 1) { keys.save("test-key") }
        assertTrue(vm.keySaved.value)
        assertFalse(vm.keyError.value)
        assertFalse(vm.keyBusy.value)
        vm.saveApiKey("")
        runCurrent()
        coVerify(exactly = 1) { keys.save("") }
    }

    @Test fun failedKeySaveCanBeRetried() = runTest(dispatcher) {
        coEvery { keys.save(any()) } throws IllegalStateException("Storage unavailable")
        val vm = vm()
        vm.saveApiKey("test-key")
        runCurrent()
        assertTrue(vm.keyError.value)
        assertFalse(vm.keySaved.value)
        assertFalse(vm.keyBusy.value)
        coEvery { keys.save(any()) } returns Unit
        vm.saveApiKey("replacement-key")
        runCurrent()
        assertFalse(vm.keyError.value)
        assertTrue(vm.keySaved.value)
    }

    @Test fun keyFailuresExposeOnlyTheSafeCategory() = runTest(dispatcher) {
        val vm = vm()
        for (category in com.audiobookbay.app.data.storage.ApiKeyFailure.entries) {
            coEvery { keys.save(any()) } throws com.audiobookbay.app.data.storage.ApiKeySaveException(category)
            vm.saveApiKey("synthetic-key")
            runCurrent()
            org.junit.Assert.assertEquals(category, vm.keyFailure.value)
            assertFalse(vm.keySaved.value)
            assertFalse(vm.keyBusy.value)
        }
    }

    @Test fun togglingWifiOnlyUpdatesTheEngine() = runTest(dispatcher) {
        coEvery { manager.updateNetworkSettings(true, 0, 0) } returns Unit
        val vm = vm()
        vm.setWifiOnly(true)
        runCurrent()
        coVerify(exactly = 1) { manager.updateNetworkSettings(true, 0, 0) }
        assertFalse(vm.networkError.value)
    }

    @Test fun engineFailureIsReported() = runTest(dispatcher) {
        coEvery { manager.updateNetworkSettings(any(), any(), any()) } throws IllegalStateException("Unavailable")
        val vm = vm()
        vm.setWifiOnly(true)
        runCurrent()
        assertTrue(vm.networkError.value)
    }
    @Test fun formattingSettingFailureCanBeRetried() = runTest(dispatcher) {
        coEvery { formatting.setEnabled(true) } throws java.io.IOException("storage")
        val vm = vm(); vm.setFormatSearchResults(true); runCurrent()
        assertTrue(vm.formatSearchSaveFailed.value)
        coEvery { formatting.setEnabled(false) } returns Unit
        vm.setFormatSearchResults(false); runCurrent()
        coVerify { formatting.setEnabled(false) }
        assertFalse(vm.formatSearchSaveFailed.value)
    }
}
