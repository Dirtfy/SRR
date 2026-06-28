package com.dirtfy.srr.ui.window.component

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextOverflow
import androidx.fragment.app.Fragment
import com.dirtfy.srr.ui.performer.base.Item
import com.dirtfy.srr.ui.performer.base.SRRBaseScreen
import com.dirtfy.srr.ui.performer.base.theme.SRRTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

abstract class BaseFragment : Fragment() {

    abstract val currentRoute: String

    private var systemBackCallback: OnBackPressedCallback? = null

    @Composable
    abstract fun provideTitle(): String

    @Composable
    open fun shouldShowBackButton(): Boolean = false

    open fun onBackClick() {
        // Disable our interceptor so the system dispatcher handles it natively
        // (pops fragment back stack if non-empty, otherwise finishes the activity).
        systemBackCallback?.isEnabled = false
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    @Composable
    protected fun AccountLabel() {
        val email = remember { Firebase.auth.currentUser?.email.orEmpty() }
        if (email.isNotEmpty()) {
            Text(
                text     = email,
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    open fun TopBarActions() {}

    open fun onTabSelected(item: Item) {
        if (item.route == currentRoute) return

        val targetFragment = when (item.route) {
            "my"     -> UserFragment()
            "result" -> CompilationFragment()
            else     -> null
        }

        targetFragment?.let {
            val containerId = (view?.parent as? ViewGroup)?.id ?: android.R.id.content
            parentFragmentManager.beginTransaction()
                .replace(containerId, it)
                .commit()
        }
    }

    @Composable
    abstract fun ScreenContent(modifier: androidx.compose.ui.Modifier)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SRRTheme {
                    SRRBaseScreen(
                        title          = provideTitle(),
                        showBackButton = shouldShowBackButton(),
                        onBackClick    = { onBackClick() },
                        navigationItems = listOf(
                            Item("my",     "My",     Icons.Default.Person),
                            Item("result", "Result", Icons.AutoMirrored.Filled.List)
                        ),
                        currentRoute = currentRoute,
                        onTabClick   = { onTabSelected(it) },
                        actions      = { TopBarActions() },
                        content      = { modifier -> ScreenContent(modifier) }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        systemBackCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackClick()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, systemBackCallback!!)
    }
}
