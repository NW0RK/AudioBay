package com.audiobookbay.app

import com.audiobookbay.app.data.storage.AppearanceSettings
import com.audiobookbay.app.ui.AppearanceViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceViewModelTest {
    @Test fun failedSaveCanBeRetriedAndRapidChangesKeepLatest() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val settings = mockk<AppearanceSettings>()
            val saved = MutableStateFlow(100)
            every { settings.fontPercent } returns saved
            coEvery { settings.setFontPercent(any()) } throws java.io.IOException("disk unavailable")
            val vm = AppearanceViewModel(settings)
            vm.setFontPercent(120)
            runCurrent()
            assertTrue(vm.saveFailed.value)
            coEvery { settings.setFontPercent(any()) } coAnswers { saved.value = firstArg(); Unit }
            vm.setFontPercent(130)
            vm.setFontPercent(100)
            runCurrent()
            coVerify { settings.setFontPercent(100) }
            assertEquals(100, saved.value)
            assertFalse(vm.saveFailed.value)
        } finally { Dispatchers.resetMain() }
    }
}
