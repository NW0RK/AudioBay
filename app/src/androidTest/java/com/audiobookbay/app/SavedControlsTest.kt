package com.audiobookbay.app
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.domain.repository.SavedBooksRepository
import com.audiobookbay.app.ui.screens.saved.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
class SavedControlsTest {
    @get:Rule val compose = createComposeRule()
    @Test fun accessibleSaveRemoveAndRetry() {
        val book = Audiobook("id", "Example Book", "Author", language = "French", audioFormat = "M4B", formattedSize = "200 MB")
        val rows = MutableStateFlow<Resource<List<Audiobook>>>(Resource.Success(emptyList()))
        var fail = false
        val repo = object : SavedBooksRepository {
            override fun observeSaved() = rows
            override suspend fun setSaved(book: Audiobook, saved: Boolean): Resource<Unit> {
                if (fail) return Resource.Error("failure")
                rows.value = Resource.Success(if (saved) listOf(book) else emptyList())
                return Resource.Success(Unit)
            }
        }
        val vm = SavedViewModel(repo)
        compose.setContent { MaterialTheme { Column { BookSummaryMetadata(book); SaveBookControl(book, vm) } } }
        compose.onNodeWithText("French · M4B · 200 MB").assertIsDisplayed()
        compose.onNodeWithContentDescription("Save Example Book for later").assertIsEnabled().performClick()
        compose.onNodeWithContentDescription("Remove Example Book from Saved").assertIsDisplayed()
        compose.runOnIdle { fail = true }
        compose.onNodeWithContentDescription("Remove Example Book from Saved").performClick()
        compose.onNodeWithText("Could not update Saved.").assertIsDisplayed()
        compose.runOnIdle { fail = false }
        compose.onNodeWithText(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.retry)).performClick()
        compose.onNodeWithContentDescription("Save Example Book for later").assertIsDisplayed()
    }
}
