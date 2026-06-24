package com.dirtfy.srr.ui.window.component

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirtfy.srr.ui.performer.base.theme.SRRTheme
import com.dirtfy.srr.ui.performer.login.LoginScreen
import com.dirtfy.srr.ui.performer.login.LoginViewModel
import com.dirtfy.srr.ui.window.MainActivity

class LoginFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SRRTheme {
                    LoginScreen(
                        viewModel = viewModel(factory = LoginViewModel.factory()),
                        onLoginSuccess = {
                            (activity as? MainActivity)?.navigateToMine()
                        }
                    )
                }
            }
        }
    }
}
