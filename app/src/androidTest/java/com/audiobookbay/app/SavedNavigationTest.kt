package com.audiobookbay.app
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.audiobookbay.app.ui.MainActivity
import org.junit.Rule
import org.junit.Test
class SavedNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun fifthTabOpensSavedAndReturnsHome() {
        listOf(R.string.nav_home, R.string.nav_search, R.string.nav_downloads, R.string.nav_saved, R.string.nav_settings).forEach { label ->
            compose.onNodeWithContentDescription(compose.activity.getString(label)).assertIsDisplayed()
        }
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.nav_saved)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.nav_saved)).assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.nav_home)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.discover_title)).assertIsDisplayed()
    }
}
