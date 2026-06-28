package com.dirtfy.srr.ui.window.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dirtfy.srr.ui.performer.compilation.CompilationScreen
import com.dirtfy.srr.ui.performer.compilation.CompilationUiState
import com.dirtfy.srr.ui.performer.compilation.CompilationViewModel
import com.dirtfy.srr.ui.window.MainActivity

class CompilationFragment : BaseFragment() {

    private val viewModel: CompilationViewModel by viewModels { CompilationViewModel.factory() }

    override val currentRoute: String = "result"

    @Composable
    override fun provideTitle(): String {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val state = uiState as? CompilationUiState.Ready ?: return "Results"
        return when {
            state.selectedItem    != null -> state.selectedItem.name
            state.selectedFeature != null -> state.selectedFeature.name
            else -> "Results"
        }
    }

    @Composable
    override fun shouldShowBackButton(): Boolean {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val state = uiState as? CompilationUiState.Ready ?: return false
        return state.selectedItem != null || state.selectedFeature != null
    }

    override fun onBackClick() {
        val state = viewModel.uiState.value as? CompilationUiState.Ready
        if (state != null && (state.selectedItem != null || state.selectedFeature != null)) {
            viewModel.clearSelection()
        } else {
            super.onBackClick()
        }
    }

    @Composable
    override fun TopBarActions() {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AccountLabel()
            IconButton(onClick = { (activity as? MainActivity)?.signOut() }) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign out")
            }
        }
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        CompilationScreen(
            modifier              = modifier,
            uiState               = uiState,
            onTabSelected         = viewModel::onTabSelected,
            onItemSelected        = viewModel::onItemSelected,
            onFeatureSelected     = viewModel::onFeatureSelected,
            onMapXFeatureSelected = viewModel::onMapXFeatureSelected,
            onMapYFeatureSelected = viewModel::onMapYFeatureSelected,
            onRetryTap            = viewModel::onRetryTap
        )
    }
}
