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
import com.dirtfy.srr.view.theme.SRRTheme // Replace with your actual theme package

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
                    // 1. Collect state from ViewModel
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    // 2. Use the new Main Screen
                    // We pass the items from our ViewModel state into the screen
                    MineMainScreen(items = uiState.items)
                }
            }
        }
    }
}