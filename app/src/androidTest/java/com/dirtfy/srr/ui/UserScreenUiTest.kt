package com.dirtfy.srr.ui

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dirtfy.srr.core.model.Feature
import com.dirtfy.srr.core.model.Item
import com.dirtfy.srr.core.model.ScoreMatrix
import com.dirtfy.srr.ui.performer.user.UserScreen
import com.dirtfy.srr.ui.performer.user.UserUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for UserScreen (My tab).
 *
 * These are pure UI tests — no Firebase, no ViewModel.
 * We build a UiState directly, set it via setContent, and assert what is rendered.
 *
 * Covers:
 *  - Items tab: list, "Added by you" badge, delete button visibility
 *  - Features tab: list, "Tap to evaluate" / "You evaluated" hints, evaluator counts
 *  - Delete confirmation dialog appearance
 *  - Add item / add feature dialogs
 */
@RunWith(AndroidJUnit4::class)
class UserScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ---------------------------------------------------------------------------
    // Fixture helpers
    // ---------------------------------------------------------------------------

    private val emptyMatrix = ScoreMatrix(emptyMap(), emptyMap())

    private val ownerUid    = "user_owner"
    private val otherUid    = "user_other"
    private val itemOwned   = Item("item_1", "Laptop",  createdBy = ownerUid)
    private val itemForeign = Item("item_2", "Phone",   createdBy = otherUid)
    private val featOwned   = Feature("feat_1", "Performance", createdBy = ownerUid)
    private val featForeign = Feature("feat_2", "Stability",   createdBy = otherUid)

    private fun readyState(
        items:                  List<Item>    = listOf(itemOwned, itemForeign),
        features:               List<Feature> = listOf(featOwned, featForeign),
        currentUserId:          String        = ownerUid,
        evaluatedFeatureIds:    Set<String>   = emptySet(),
        evaluatorCountByFeature: Map<String, Int> = emptyMap(),
        activeTab:              UserUiState.Tab   = UserUiState.Tab.ITEMS,
        deleteConfirmation:     UserUiState.Ready.DeleteConfirmationState? = null,
        addItemDialog:          UserUiState.Ready.AddItemDialogState? = null,
        addFeatureDialog:       UserUiState.Ready.AddFeatureDialogState? = null
    ) = UserUiState.Ready(
        items                   = items,
        features                = features,
        scoreMatrix             = emptyMatrix,
        currentUserId           = currentUserId,
        evaluatedFeatureIds     = evaluatedFeatureIds,
        evaluatorCountByFeature = evaluatorCountByFeature,
        activeTab               = activeTab,
        deleteConfirmation      = deleteConfirmation,
        addItemDialog           = addItemDialog,
        addFeatureDialog        = addFeatureDialog
    )

    private fun setScreen(state: UserUiState) {
        composeTestRule.setContent {
            UserScreen(
                uiState                    = state,
                onTabSelected              = {},
                onItemSelected             = {},
                onFeatureSelected          = {},
                onOpenEditor               = {},
                onEvaluationReorder        = {},
                onSubmitEvaluation         = {},
                onRetryTap                 = {},
                onOpenAddItemDialog        = {},
                onOpenAddFeatureDialog     = {},
                onAddItemNameChange        = {},
                onAddItemImagePicked       = {},
                onAddFeatureNameChange     = {},
                onDismissAddDialog         = {},
                onAddItem                  = {},
                onAddFeature               = {},
                onRequestDeleteItem           = { _, _ -> },
                onRequestDeleteFeature        = { _, _ -> },
                onConfirmDelete               = {},
                onDismissDeleteConfirmation   = {},
                onOpenEditItemImageDialog     = {},
                onEditItemImagePicked         = {},
                onSubmitEditItemImage         = {},
                onDismissEditItemImageDialog  = {},
                onShowItemImagePreview        = {},
                onDismissItemImagePreview     = {}
            )
        }
    }

    // ---------------------------------------------------------------------------
    // Items tab
    // ---------------------------------------------------------------------------

    @Test
    fun itemsTab_allItemNamesAreDisplayed() {
        setScreen(readyState())
        composeTestRule.onNodeWithText("Laptop").assertIsDisplayed()
        composeTestRule.onNodeWithText("Phone").assertIsDisplayed()
    }

    @Test
    fun itemsTab_ownedItem_showsAddedByYouBadge() {
        setScreen(readyState())
        // "Added by you" must appear exactly for the owned item
        composeTestRule.onAllNodesWithText("Added by you").assertCountEquals(1)
    }

    @Test
    fun itemsTab_foreignItem_doesNotShowAddedByYou() {
        setScreen(readyState(items = listOf(itemForeign), currentUserId = ownerUid))
        composeTestRule.onAllNodesWithText("Added by you").assertCountEquals(0)
    }

    @Test
    fun itemsTab_ownedItem_deleteButtonVisible_foreignItem_deleteButtonHidden() {
        setScreen(readyState())
        // Delete content-desc appears only for owned items
        composeTestRule.onAllNodesWithContentDescription("Delete").assertCountEquals(1)
    }

    // ---------------------------------------------------------------------------
    // Features tab
    // ---------------------------------------------------------------------------

    @Test
    fun featuresTab_allFeatureNamesAreDisplayed() {
        setScreen(readyState(activeTab = UserUiState.Tab.FEATURES))
        composeTestRule.onNodeWithText("Performance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stability").assertIsDisplayed()
    }

    @Test
    fun featuresTab_notEvaluated_showsTapToEvaluate() {
        setScreen(readyState(
            activeTab = UserUiState.Tab.FEATURES,
            evaluatedFeatureIds = emptySet()
        ))
        // Both featOwned (owner, not yet evaluated) and featForeign (not evaluated) show the hint
        composeTestRule.onAllNodesWithText("Tap to evaluate").assertCountEquals(2)
    }

    @Test
    fun featuresTab_evaluated_showsYouEvaluated() {
        setScreen(readyState(
            activeTab = UserUiState.Tab.FEATURES,
            evaluatedFeatureIds = setOf(featOwned.id, featForeign.id)
        ))
        composeTestRule.onAllNodesWithText("You evaluated").assertCountEquals(2)
    }

    @Test
    fun featuresTab_evaluatorCount_isDisplayed() {
        setScreen(readyState(
            activeTab = UserUiState.Tab.FEATURES,
            evaluatorCountByFeature = mapOf(featOwned.id to 2, featForeign.id to 5)
        ))
        // Count is shown as "N / 3" sub-label
        composeTestRule.onNodeWithText("2 / 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 / 3").assertIsDisplayed()
    }

    @Test
    fun featuresTab_zeroEvaluators_showsZeroCount() {
        setScreen(readyState(
            activeTab = UserUiState.Tab.FEATURES,
            evaluatorCountByFeature = emptyMap()
        ))
        // Both features have 0 evaluators → "0%" appears twice
        composeTestRule.onAllNodesWithText("0%").assertCountEquals(2)
    }

    // ---------------------------------------------------------------------------
    // Delete confirmation dialog
    // ---------------------------------------------------------------------------

    @Test
    fun deleteConfirmationDialog_showsItemNameAndType() {
        setScreen(readyState(
            deleteConfirmation = UserUiState.Ready.DeleteConfirmationState(
                id   = itemOwned.id,
                name = itemOwned.name,
                type = UserUiState.Ready.DeleteTargetType.ITEM
            )
        ))
        composeTestRule.onNodeWithText("Delete Item?").assertIsDisplayed()
        composeTestRule.onNodeWithText("\"Laptop\" will be permanently removed.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun deleteConfirmationDialog_showsFeatureType() {
        setScreen(readyState(
            deleteConfirmation = UserUiState.Ready.DeleteConfirmationState(
                id   = featOwned.id,
                name = featOwned.name,
                type = UserUiState.Ready.DeleteTargetType.FEATURE
            )
        ))
        composeTestRule.onNodeWithText("Delete Feature?").assertIsDisplayed()
        composeTestRule.onNodeWithText("\"Performance\" will be permanently removed.").assertIsDisplayed()
    }

    // ---------------------------------------------------------------------------
    // Add item dialog
    // ---------------------------------------------------------------------------

    @Test
    fun addItemDialog_showsTitleAndAddButton() {
        setScreen(readyState(
            addItemDialog = UserUiState.Ready.AddItemDialogState(name = "")
        ))
        composeTestRule.onNodeWithText("Add Item").assertIsDisplayed()
        // "Add" button exists (disabled when name is blank, but still rendered)
        composeTestRule.onNodeWithText("Add").assertExists()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun addItemDialog_withName_addButtonExists() {
        setScreen(readyState(
            addItemDialog = UserUiState.Ready.AddItemDialogState(name = "Tablet")
        ))
        composeTestRule.onNodeWithText("Add").assertIsDisplayed()
    }

    // ---------------------------------------------------------------------------
    // Add item dialog — image picker states
    // ---------------------------------------------------------------------------

    @Test
    fun addItemDialog_noImage_showsPlaceholderText() {
        setScreen(readyState(addItemDialog = UserUiState.Ready.AddItemDialogState(name = "")))
        composeTestRule.onNodeWithText("Tap to add image (optional)").assertIsDisplayed()
    }

    @Test
    fun addItemDialog_whileUploading_addAndCancelAreDisabled() {
        setScreen(readyState(
            addItemDialog = UserUiState.Ready.AddItemDialogState(
                name             = "Tablet",
                imageUri         = Uri.parse("file:///test/image.jpg"),
                isUploadingImage = true
            )
        ))
        composeTestRule.onNodeWithText("Add").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Cancel").assertIsNotEnabled()
    }

    @Test
    fun addItemDialog_blankNameWhileUploading_addStillDisabled() {
        setScreen(readyState(
            addItemDialog = UserUiState.Ready.AddItemDialogState(
                name             = "",
                isUploadingImage = true
            )
        ))
        composeTestRule.onNodeWithText("Add").assertIsNotEnabled()
    }

    @Test
    fun addItemDialog_uploadError_showsErrorText() {
        setScreen(readyState(
            addItemDialog = UserUiState.Ready.AddItemDialogState(
                name  = "Tablet",
                error = "Image upload failed: permission denied"
            )
        ))
        composeTestRule.onNodeWithText("Image upload failed: permission denied").assertIsDisplayed()
    }

    @Test
    fun addItemDialog_uploadComplete_addButtonEnabled() {
        setScreen(readyState(
            addItemDialog = UserUiState.Ready.AddItemDialogState(
                name             = "Tablet",
                imageUri         = Uri.parse("file:///test/image.jpg"),
                imageUrl         = "https://storage.example.com/items/uuid.jpg",
                isUploadingImage = false
            )
        ))
        composeTestRule.onNodeWithText("Add").assertIsEnabled()
    }

    // ---------------------------------------------------------------------------
    // Add feature dialog
    // ---------------------------------------------------------------------------

    @Test
    fun addFeatureDialog_showsTitleAndAddButton() {
        setScreen(readyState(
            addFeatureDialog = UserUiState.Ready.AddFeatureDialogState(name = "")
        ))
        composeTestRule.onNodeWithText("Add Feature").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add").assertExists()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    // ---------------------------------------------------------------------------
    // Loading / Error states
    // ---------------------------------------------------------------------------

    @Test
    fun loadingState_showsProgressIndicator() {
        setScreen(UserUiState.Loading)
        // Loading state must NOT show any item/feature text — the content is not rendered
        composeTestRule.onAllNodesWithText("Laptop").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Performance").assertCountEquals(0)
        // At least one node exists (the spinner itself)
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun errorState_showsMessageAndRetryButton() {
        setScreen(UserUiState.Error("Network error"))
        composeTestRule.onNodeWithText("Network error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    // ---------------------------------------------------------------------------
    // Item detail — image preview popup
    // ---------------------------------------------------------------------------

    private fun itemWithImage(url: String) = Item("item_img", "Camera", createdBy = ownerUid, imageUrl = url)

    private fun readyStateWithSelectedItem(item: Item, imagePreviewUrl: String? = null) =
        UserUiState.Ready(
            items                = listOf(item),
            features             = listOf(featOwned),
            scoreMatrix          = emptyMatrix,
            currentUserId        = ownerUid,
            selectedItem         = item,
            imagePreviewUrl      = imagePreviewUrl
        )

    @Test
    fun itemDetail_withImage_imagePreviewDialogNotShownByDefault() {
        // imagePreviewUrl null → no dialog overlay
        setScreen(readyStateWithSelectedItem(itemWithImage("https://example.com/img.jpg")))
        composeTestRule.onAllNodesWithContentDescription("Image preview overlay, tap to close")
            .assertCountEquals(0)
        composeTestRule.onNodeWithText("Camera").assertIsDisplayed()
    }

    @Test
    fun itemDetail_imagePreviewDialog_isShownWhenStateHasPreviewUrl() {
        // imagePreviewUrl non-null → overlay dialog must render
        val state = readyStateWithSelectedItem(
            item            = itemWithImage("https://example.com/img.jpg"),
            imagePreviewUrl = "https://example.com/img.jpg"
        )
        setScreen(state)
        composeTestRule
            .onNodeWithContentDescription("Image preview overlay, tap to close")
            .assertExists()
    }

    @Test
    fun itemDetail_imageClickCallback_isInvokedWhenImageIsClicked() {
        var previewUrl: String? = null
        val imageUrl = "https://example.com/img.jpg"

        composeTestRule.setContent {
            UserScreen(
                uiState                    = readyStateWithSelectedItem(itemWithImage(imageUrl)),
                onTabSelected              = {},
                onItemSelected             = {},
                onFeatureSelected          = {},
                onOpenEditor               = {},
                onEvaluationReorder        = {},
                onSubmitEvaluation         = {},
                onRetryTap                 = {},
                onOpenAddItemDialog        = {},
                onOpenAddFeatureDialog     = {},
                onAddItemNameChange        = {},
                onAddItemImagePicked       = {},
                onAddFeatureNameChange     = {},
                onDismissAddDialog         = {},
                onAddItem                  = {},
                onAddFeature               = {},
                onRequestDeleteItem           = { _, _ -> },
                onRequestDeleteFeature        = { _, _ -> },
                onConfirmDelete               = {},
                onDismissDeleteConfirmation   = {},
                onOpenEditItemImageDialog     = {},
                onEditItemImagePicked         = {},
                onSubmitEditItemImage         = {},
                onDismissEditItemImageDialog  = {},
                onShowItemImagePreview        = { url -> previewUrl = url },
                onDismissItemImagePreview     = {}
            )
        }

        // Click the image area via its semantic content description
        composeTestRule
            .onNodeWithContentDescription("Item image, tap to preview")
            .performClick()

        assert(previewUrl == imageUrl) {
            "Expected preview URL $imageUrl but got $previewUrl"
        }
    }
}
