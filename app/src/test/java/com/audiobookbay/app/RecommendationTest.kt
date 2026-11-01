package com.audiobookbay.app
import com.audiobookbay.app.data.repository.DiscoveryRepository
import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.domain.repository.*
import com.audiobookbay.app.ui.screens.home.HomeViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationTest {
    private val book = Audiobook("real", "Real title", "Author")
    @Test fun unknownIdsAndBlankReasonsNeverBecomeRecommendations() {
        val json = Json.parseToJsonElement("""{"picks":[{"id":99,"reason":"Invented"},{"id":0,"reason":" "}]}""").jsonObject
        assertTrue(DiscoveryRepository.selectPicks(json, listOf(book)).isEmpty())
    }
    @Test fun duplicateIdsUseActualCatalogMetadata() {
        val json = Json.parseToJsonElement("""{"picks":[{"id":0,"reason":"Adjacent interest"},{"id":0,"reason":"Duplicate"}]}""").jsonObject
        val picks = DiscoveryRepository.selectPicks(json, listOf(book))
        assertEquals(1, picks.size); assertEquals(book, picks.single().book)
    }
    @Test fun unicodeAndPunctuationAreNormalizedWithoutDiscardingNames() {
        assertEquals(DiscoveryRepository.identity("BOOK: One", "Émile"), DiscoveryRepository.identity("book one", "Émile"))
        assertNotEquals(DiscoveryRepository.identity("წიგნი", "ავტორი"), DiscoveryRepository.identity("Other", "Author"))
    }
    @Test fun numericReasonIsRejected() {
        val json = Json.parseToJsonElement("""{"picks":[{"id":0,"reason":12}]}""").jsonObject
        assertTrue(DiscoveryRepository.selectPicks(json, listOf(book)).isEmpty())
    }
    @Test fun openingAndRefreshingHomeNeverGenerateAndRepeatedPressIsSingleFlight() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val catalog = mockk<AudiobookRepository>()
            val discovery = mockk<RecommendationRepository>()
            coEvery { catalog.getRecentAudiobooks(any()) } returns Resource.Success(emptyList())
            coEvery { catalog.getCategories() } returns Resource.Success(emptyList())
            coEvery { discovery.restore() } just Runs
            coEvery { discovery.generate() } coAnswers { delay(100); Resource.Success(emptyList()) }
            val vm = HomeViewModel(catalog, discovery)
            runCurrent(); vm.refresh(); runCurrent()
            coVerify(exactly = 0) { discovery.generate() }
            vm.generatePicks(); vm.generatePicks(); runCurrent()
            coVerify(exactly = 1) { discovery.generate() }
            advanceUntilIdle(); assertFalse(vm.generating.value)
        } finally { Dispatchers.resetMain() }
    }
}
