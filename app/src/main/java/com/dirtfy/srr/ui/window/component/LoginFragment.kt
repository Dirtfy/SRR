package com.dirtfy.srr.ui.window.component

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirtfy.srr.ui.performer.base.theme.SRRTheme
import com.dirtfy.srr.ui.performer.login.LoginScreen
import com.dirtfy.srr.ui.performer.login.LoginViewModel
import com.dirtfy.srr.ui.window.MainActivity

class LoginFragment : Fragment() {

    companion object {
        private const val ARG_AUTO_LOGIN = "auto_login"

        fun newInstance(autoLogin: Boolean = true) = LoginFragment().apply {
            arguments = bundleOf(ARG_AUTO_LOGIN to autoLogin)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val autoLogin = arguments?.getBoolean(ARG_AUTO_LOGIN, true) ?: true
        return ComposeView(requireContext()).apply {
            setContent {
                SRRTheme {
                    LoginScreen(
                        viewModel    = viewModel(factory = LoginViewModel.factory()),
                        autoLogin    = autoLogin,
                        onLoginSuccess = {
                            (activity as? MainActivity)?.navigateToMine()
                        }
                    )
                }
            }
        }
    }
}
