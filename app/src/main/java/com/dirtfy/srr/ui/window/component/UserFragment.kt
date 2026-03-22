package com.dirtfy.srr.ui.window.component

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue // ADD THIS
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dirtfy.srr.ui.performer.user.MineMainScreen
import com.dirtfy.srr.ui.performer.user.UserViewModel
import com.dirtfy.srr.ui.performer.theme.SRRTheme

class UserFragment : Fragment() {

    private val viewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SRRTheme {
                    // This 'by' delegate now works because of the 'getValue' import
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    MineMainScreen(
                        uiState = uiState,
                        onToggleView = { viewModel.toggleView() },
                        onItemClick = { item -> viewModel.selectItem(item) },
                        onFeatureClick = { feature -> viewModel.selectFeature(feature) },
                        onSubItemClick = { subItem -> viewModel.selectSubItem(subItem) },
                        onDismissSubPopup = { viewModel.dismissSubItemPopup() },
                        onBackToGrid = { viewModel.clearSelection() }
                    )
                }
            }
        }
    }
}