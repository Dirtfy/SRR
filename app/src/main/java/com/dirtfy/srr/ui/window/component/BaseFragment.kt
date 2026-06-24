package com.dirtfy.srr.ui.window.component

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.dirtfy.srr.ui.performer.base.Item
import com.dirtfy.srr.ui.performer.base.SRRBaseScreen
import com.dirtfy.srr.ui.performer.base.theme.SRRTheme

abstract class BaseFragment : Fragment() {

    abstract val currentRoute: String

    @Composable
    abstract fun provideTitle(): String

    @Composable
    open fun shouldShowBackButton(): Boolean = false

    open fun onBackClick() {
        parentFragmentManager.popBackStack()
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
}
