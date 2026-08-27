package com.khaled.frais

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.khaled.frais.ui.main.MainActivity
import org.junit.Rule
import org.junit.Test

class AppNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testNavigation() {
        composeTestRule.onNodeWithText("Launcher").assertExists()

        composeTestRule.onNodeWithText("Categories").performClick()
        composeTestRule.onNodeWithText("Categories").assertExists()

        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Working mode").assertExists()

        composeTestRule.onNodeWithText("About").performClick()
        composeTestRule.onNodeWithText("Frais").assertExists()
        composeTestRule.onNodeWithText("Version").assertExists()
    }
}
