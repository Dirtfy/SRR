package com.dirtfy.srr.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dirtfy.srr.core.model.Feature
import com.dirtfy.srr.core.model.Item
import com.dirtfy.srr.core.model.ScoreMatrix
import com.dirtfy.srr.ui.performer.compilation.CompilationScreen
import com.dirtfy.srr.ui.performer.compilation.CompilationUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for CompilationScreen (Result tab).
 *
 * These are pure UI tests — no Firebase, no ViewModel.
 * We build CompilationUiState directly and assert what is rendered.
 *
 * Covers:
 *  - Items tab: list display
 *  - Features tab: percentage display, X/3 users sub-label, "Scores available" badge
 *  - Score threshold boundary values (0, 1, 2, 3 evaluators)
 *  - Item detail: score display for each feature
 *  - Feature detail: items sorted by score, scores displayed
 *  - Loading / Error states
 */
@RunWith(AndroidJUnit4::class)
class CompilationScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ---------------------------------------------------------------------------
    // Fixture helpers
    // ---------------------------------------------------------------------------

    private val phone   = Item("item_phone",   "Phone")
    private val laptop  = Item("item_laptop",  "Laptop")
    private val tablet  = Item("item_tablet",  "Tablet")
    private val perf    = Feature("feat_perf", "Performance")
    private val stab    = Feature("feat_stab", "Stability")

    /** ScoreMatrix where phone > laptop > tablet for Performance; Stability below threshold (null). */
    private val scoreMatrix = ScoreMatrix(
        scores = mapOf(
            phone.id  to mapOf(perf.id to 8.0,  stab.id to null),
            laptop.id to mapOf(perf.id to 6.0,  stab.id to null),
            tablet.id to mapOf(perf.id to 3.5,  stab.id to null)
        ),
        voteCounts = mapOf(
            phone.id  to mapOf(perf.id to 3, stab.id to 2),
            laptop.id to mapOf(perf.id to 3, stab.id to 2),
            tablet.id to mapOf(perf.id to 3, stab.id to 2)
        )
    )

    private fun readyState(
        items:                  List<Item>    = listOf(phone, laptop, tablet),
        features:               List<Feature> = listOf(perf, stab),
        matrix:                 ScoreMatrix   = scoreMatrix,
        evaluatorCountByFeature: Map<String, Int> = mapOf(perf.id to 3, stab.id to 2),
        activeTab:              CompilationUiState.Tab = CompilationUiState.Tab.ITEMS,
        selectedItem:           Item?    = null,
        selectedFeature:        Feature? = null
    ) = CompilationUiState.Ready(
        items                   = items,
        features                = features,
        scoreMatrix             = matrix,
        evaluatorCountByFeature = evaluatorCountByFeature,
        activeTab               = activeTab,
        selectedItem            = selectedItem,
        selectedFeature         = selectedFeature
    )

    private fun setScreen(state: CompilationUiState) {
        composeTestRule.setContent {
            CompilationScreen(
                uiState               = state,
                onTabSelected         = {},
                onItemSelected        = {},
                onFeatureSelected     = {},
                onMapXFeatureSelected = {},
                onMapYFeatureSelected = {},
                onRetryTap            = {}
            )
        }
    }

    // ---------------------------------------------------------------------------
    // Items tab
    // ---------------------------------------------------------------------------

    @Test
    fun itemsTab_allItemNamesDisplayed() {
        setScreen(readyState())
        composeTestRule.onNodeWithText("Phone").assertIsDisplayed()
        composeTestRule.onNodeWithText("Laptop").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tablet").assertIsDisplayed()
    }

    // ---------------------------------------------------------------------------
    // Features tab — percentage display
    // ---------------------------------------------------------------------------

    @Test
    fun featuresTab_allFeatureNamesDisplayed() {
        setScreen(readyState(activeTab = CompilationUiState.Tab.FEATURES))
        composeTestRule.onNodeWithText("Performance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stability").assertIsDisplayed()
    }

    @Test
    fun featuresTab_threeEvaluators_shows100Percent() {
        setScreen(readyState(
            activeTab = CompilationUiState.Tab.FEATURES,
            evaluatorCountByFeature = mapOf(perf.id to 3, stab.id to 0)
        ))
        composeTestRule.onNodeWithText("100%").assertIsDisplayed()
    }

    @Test
    fun featuresTab_twoEvaluators_shows66Percent() {
        setScreen(readyState(
            activeTab = CompilationUiState.Tab.FEATURES,
            evaluatorCountByFeature = mapOf(perf.id to 2, stab.id to 0)
        ))
        // 2 * 100 / 3 = 66 (integer division)
        composeTestRule.onNodeWithText("66%").assertIsDisplayed()
    }

    @Test
    fun featuresTab_oneEvaluator_shows33Percent() {
        setScreen(readyState(
            activeTab = CompilationUiState.Tab.FEATURES,
            evaluatorCountByFeature = mapOf(perf.id to 1, stab.id to 0)
        ))
        // 1 * 100 / 3 = 33 (integer division)
        composeTestRule.onNodeWithText("33%").assertIsDisplayed()
    }

    @Test
    fun featuresTab_zeroEvaluators_shows0Percent() {
        setScreen(readyState(
            activeTab = CompilationUiState.Tab.FEATURES,
            evaluatorCountByFeature = mapOf(perf.id to 0, stab.id to 0)
        ))
        // Two features both at 0% → two "0%" nodes
        composeTestRule.onAllNodesWithText("0%").assertCountEquals(2)
    }

    @Test
    fun featuresTab_subLabel_showsCorrectDenominator() {
        setScreen(readyState(
            activeTab = CompilationUiState.Tab.FEATURES,
            evaluatorCountByFeature = mapOf(perf.id to 3, stab.id to 2)
        ))
        composeTestRule.onNodeWithText("3 / 3 users").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 / 3 users").assertIsDisplayed()
    }

    @Test
    fun featuresTab_atThreshold_showsScoresAvailableBadge() {
        setScreen(readyState(
            activeTab = CompilationUiState.Tab.FEATURES,
            evaluatorCountByFeature = mapOf(perf.id to 3, stab.id to 2)
        ))
        // Only Performance meets threshold → exactly 1 "Scores available" badge
        composeTestRule.onAllNodesWithText("Scores available").assertCountEquals(1)
    }

    @Test
    fun featuresTab_aboveThreshold_stillShows100PercentAndBadge() {
        // 5 evaluators for one feature — should clamp to 100% (not 167%)
        setScreen(readyState(
            activeTab = CompilationUiState.Tab.FEATURES,
            evaluatorCountByFeature = mapOf(perf.id to 5, stab.id to 0)
        ))
        composeTestRule.onNodeWithText("100%").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Scores available").assertCountEquals(1)
    }

    // ---------------------------------------------------------------------------
    // Item detail view
    // ---------------------------------------------------------------------------

    @Test
    fun itemDetail_showsItemNameAndFeatureScores() {
        setScreen(readyState(selectedItem = phone))
        // Headline
        composeTestRule.onNodeWithText("Phone").assertIsDisplayed()
        // Feature labels
        composeTestRule.onNodeWithText("Performance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stability").assertIsDisplayed()
        // Scores: phone has 8.0 for Performance, null for Stability (voteCount=2, needs 1 more)
        composeTestRule.onNodeWithText("8.0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Needs 1 more").assertIsDisplayed()
    }

    @Test
    fun itemDetail_scoresAreFormattedToOneDecimal() {
        setScreen(readyState(selectedItem = tablet))
        // tablet has 3.5 for Performance
        composeTestRule.onNodeWithText("3.5").assertIsDisplayed()
    }

    // ---------------------------------------------------------------------------
    // Feature detail view
    // ---------------------------------------------------------------------------

    @Test
    fun featureDetail_showsFeatureNameAndAllItems() {
        setScreen(readyState(selectedFeature = perf))
        composeTestRule.onNodeWithText("Performance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Phone").assertIsDisplayed()
        composeTestRule.onNodeWithText("Laptop").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tablet").assertIsDisplayed()
    }

    @Test
    fun featureDetail_itemsSortedByScore_highestFirst() {
        setScreen(readyState(selectedFeature = perf))
        // Scores: Phone=8.0, Laptop=6.0, Tablet=3.5 → Phone must appear before Tablet
        val phone_pos  = composeTestRule.onNodeWithText("Phone").fetchSemanticsNode().positionInRoot.y
        val laptop_pos = composeTestRule.onNodeWithText("Laptop").fetchSemanticsNode().positionInRoot.y
        val tablet_pos = composeTestRule.onNodeWithText("Tablet").fetchSemanticsNode().positionInRoot.y
        assertTrue("Phone must appear above Laptop",  phone_pos  < laptop_pos)
        assertTrue("Laptop must appear above Tablet", laptop_pos < tablet_pos)
    }

    @Test
    fun featureDetail_scoresDisplayedForEachItem() {
        setScreen(readyState(selectedFeature = perf))
        composeTestRule.onNodeWithText("8.0").assertIsDisplayed()
        composeTestRule.onNodeWithText("6.0").assertIsDisplayed()
        composeTestRule.onNodeWithText("3.5").assertIsDisplayed()
    }

    @Test
    fun featureDetail_nullScoresShowDash() {
        setScreen(readyState(selectedFeature = stab))
        // Stability has null for all 3 items (voteCount=2, threshold=3 → needs 1 more each)
        composeTestRule.onAllNodesWithText("Needs 1 more").assertCountEquals(3)
    }

    // ---------------------------------------------------------------------------
    // Loading / Error states
    // ---------------------------------------------------------------------------

    @Test
    fun loadingState_showsProgressIndicator() {
        setScreen(CompilationUiState.Loading)
        // Loading state must NOT show any item/feature text — the content is not rendered
        composeTestRule.onAllNodesWithText("Phone").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Performance").assertCountEquals(0)
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun errorState_showsMessageAndRetryButton() {
        setScreen(CompilationUiState.Error("Load failed"))
        composeTestRule.onNodeWithText("Load failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }
}
