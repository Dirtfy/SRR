package com.dirtfy.srr.viewmodel.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dirtfy.srr.view.mine.MineMainScreen
import com.dirtfy.srr.view.theme.SRRTheme

class MineFragment : Fragment() {

    private val viewModel: MineViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SRRTheme {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    MineMainScreen(
                        uiState = uiState,
                        onToggleView = { viewModel.toggleView() },
                        onItemClick = { item -> viewModel.selectItem(item) },
                        onFeatureClick = { feature -> viewModel.selectFeature(feature) },
                        // Removed onDismissPopup because we are now using Detail Navigation
                        onBackToGrid = { viewModel.clearSelection() }
                    )
                }
            }
        }
    }
}