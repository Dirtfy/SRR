package com.dirtfy.srr.ui.window.component

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirtfy.srr.ui.performer.login.LoginScreen
import com.dirtfy.srr.ui.performer.theme.SRRTheme
import com.dirtfy.srr.ui.window.MainActivity

class LoginFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SRRTheme {
                    // We pass the callback to the Activity to handle fragment switching
                    LoginScreen(
                        viewModel = viewModel(),
                        onLoginSuccess = {
                            (activity as? MainActivity)?.navigateToMine()
                        }
                    )
                }
            }
        }
    }
}