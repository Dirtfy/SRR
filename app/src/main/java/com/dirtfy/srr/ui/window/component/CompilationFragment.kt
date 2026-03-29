package com.dirtfy.srr.ui.window.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dirtfy.srr.ui.performer.compilation.CompilationMainScreen
import com.dirtfy.srr.ui.performer.compilation.CompilationViewModel
import com.dirtfy.srr.ui.performer.compilation.ViewMode

class CompilationFragment : BaseFragment() {

    private val viewModel: CompilationViewModel by viewModels()

    override val currentRoute: String = "result"

    @Composable
    override fun provideTitle(): String {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        return when (uiState.viewMode) {
            ViewMode.ITEMS -> "Compilation Items"
            ViewMode.FEATURES -> "Feature Analysis"
            ViewMode.MAP -> "Location Map"
        }
    }

    /**
     * Checks if any of the specialized item types are currently selected.
     */
    private fun isSomethingSelected(): Boolean {
        val uiState = viewModel.uiState.value
        return uiState.selectedGridItem != null ||
                uiState.selectedFeatureItem != null ||
                uiState.selectedMapItem != null
    }

    @Composable
    override fun shouldShowBackButton(): Boolean {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        // Reactively show back button if any specialized selection is active
        return uiState.selectedGridItem != null ||
                uiState.selectedFeatureItem != null ||
                uiState.selectedMapItem != null
    }

    override fun onBackClick() {
        if (isSomethingSelected()) {
            // Clear whichever selection is currently active
            viewModel.clearGridItem()
            viewModel.clearFeatureItem()
            viewModel.clearMapItem()
        } else {
            super.onBackClick()
        }
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        CompilationMainScreen(
            modifier = modifier,
            uiState = uiState,
            onViewModeChange = { newMode ->
                viewModel.setViewMode(newMode)
            },
            // Routes specialized clicks to specialized ViewModel functions
            onGridItemClick = { item ->
                viewModel.selectGridItem(item)
            },
            onFeatureItemClick = { item ->
                viewModel.selectFeatureItem(item)
            },
            onMapItemClick = { item ->
                viewModel.selectMapItem(item)
            },
            onBackToMain = {
                // Clear all selection states
                viewModel.clearGridItem()
                viewModel.clearFeatureItem()
                viewModel.clearMapItem()
            }
        )
    }
}