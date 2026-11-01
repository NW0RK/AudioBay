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

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { folders.folder } returns flowOf(null)
        every { network.wifiOnly } returns flowOf(false)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = SettingsViewModel(folders, manager, network)

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
}
