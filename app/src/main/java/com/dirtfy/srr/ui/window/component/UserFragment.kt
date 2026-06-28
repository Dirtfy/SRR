package com.dirtfy.srr.ui.window.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dirtfy.srr.ui.performer.user.UserScreen
import com.dirtfy.srr.ui.performer.user.UserUiState
import com.dirtfy.srr.ui.performer.user.UserViewModel
import com.dirtfy.srr.ui.window.MainActivity

class UserFragment : BaseFragment() {

    private val viewModel: UserViewModel by viewModels { UserViewModel.factory(requireActivity().application) }

    override val currentRoute = "my"

    @Composable
    override fun provideTitle(): String {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val state = uiState as? UserUiState.Ready ?: return "My Dashboard"
        return when {
            state.evaluationEditor != null ->
                state.features.find { it.id == state.evaluationEditor.featureId }?.name ?: "Evaluate"
            state.selectedItem    != null -> state.selectedItem.name
            state.selectedFeature != null -> state.selectedFeature.name
            else -> "My Dashboard"
        }
    }

    @Composable
    override fun shouldShowBackButton(): Boolean {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val state = uiState as? UserUiState.Ready ?: return false
        return state.selectedItem != null || state.selectedFeature != null || state.evaluationEditor != null
    }

    override fun onBackClick() {
        val state = viewModel.uiState.value as? UserUiState.Ready
        if (state != null && (state.selectedItem != null || state.selectedFeature != null || state.evaluationEditor != null)) {
            viewModel.clearSelection()
        } else {
            super.onBackClick()
        }
    }

    @Composable
    override fun TopBarActions() {
        IconButton(onClick = { (activity as? MainActivity)?.signOut() }) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign out")
        }
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        UserScreen(
            modifier               = modifier,
            uiState                = uiState,
            onTabSelected          = viewModel::onTabSelected,
            onItemSelected         = viewModel::onItemSelected,
            onFeatureSelected      = viewModel::onFeatureSelected,
            onOpenEditor           = viewModel::onOpenEvaluationEditor,
            onEvaluationReorder    = viewModel::onEvaluationReorder,
            onSubmitEvaluation     = viewModel::onSubmitEvaluation,
            onRetryTap             = viewModel::onRetryTap,
            onOpenAddItemDialog    = viewModel::onOpenAddItemDialog,
            onOpenAddFeatureDialog = viewModel::onOpenAddFeatureDialog,
            onAddItemNameChange    = viewModel::onAddItemNameChange,
            onAddItemImagePicked   = viewModel::onAddItemImagePicked,
            onAddFeatureNameChange = viewModel::onAddFeatureNameChange,
            onDismissAddDialog     = viewModel::onDismissAddDialog,
            onAddItem                     = viewModel::onAddItem,
            onAddFeature                  = viewModel::onAddFeature,
            onRequestDeleteItem           = viewModel::onRequestDeleteItem,
            onRequestDeleteFeature        = viewModel::onRequestDeleteFeature,
            onConfirmDelete               = viewModel::onConfirmDelete,
            onDismissDeleteConfirmation   = viewModel::onDismissDeleteConfirmation
        )
    }
}
