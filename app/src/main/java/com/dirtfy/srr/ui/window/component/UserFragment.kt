package com.dirtfy.srr.ui.window.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dirtfy.srr.ui.performer.user.MineMainScreen
import com.dirtfy.srr.ui.performer.user.UserViewModel

class UserFragment : BaseFragment() {

    private val viewModel: UserViewModel by viewModels()

    override val currentRoute = "my"

    @Composable
    override fun provideTitle(): String {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        return when {
            uiState.selectedItem != null -> uiState.selectedItem!!.title
            uiState.selectedFeature != null -> uiState.selectedFeature!!.name
            else -> "My Dashboard"
        }
    }

    @Composable
    override fun shouldShowBackButton(): Boolean {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        return uiState.selectedFeature != null || uiState.selectedItem != null
    }

    override fun onBackClick() {
        val uiState = viewModel.uiState.value
        if (uiState.selectedFeature != null || uiState.selectedItem != null) {
            viewModel.clearSelection()
        } else {
            super.onBackClick()
        }
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        MineMainScreen(
            modifier = modifier,
            uiState = uiState,
            onToggleView = { viewModel.toggleView() },
            onItemClick = { viewModel.selectItem(it) },
            onFeatureClick = { viewModel.selectFeature(it) },
            onSubItemClick = { viewModel.selectSubItem(it) },
            onDismissSubPopup = { viewModel.dismissSubItemPopup() },
            onBackToGrid = { viewModel.clearSelection() }
        )
    }
}