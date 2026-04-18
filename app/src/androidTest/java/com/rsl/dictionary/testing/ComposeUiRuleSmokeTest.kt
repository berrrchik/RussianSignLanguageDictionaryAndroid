package com.rsl.dictionary.testing

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeUiRuleSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun composeRule_setsAndFindsContent() {
        composeRule.setContent {
            Text("Compose test infrastructure ready")
        }

        val nodes = composeRule
            .onAllNodesWithText("Compose test infrastructure ready")
            .fetchSemanticsNodes()

        assertTrue(nodes.isNotEmpty())
    }
}
