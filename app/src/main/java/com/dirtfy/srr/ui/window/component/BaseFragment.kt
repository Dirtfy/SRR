package com.dirtfy.srr.ui.window.component

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.dirtfy.srr.ui.performer.base.Item
import com.dirtfy.srr.ui.performer.base.SRRBaseScreen
import com.dirtfy.srr.ui.performer.base.theme.SRRTheme

abstract class BaseFragment : Fragment() {

    /**
     * The route ID for the current fragment (e.g., "my" or "result").
     * Used to highlight the correct tab in the Bottom Navigation.
     */
    abstract val currentRoute: String

    /**
     * Reactive title provided by the child fragment's ViewModel.
     */
    @Composable
    abstract fun provideTitle(): String

    /**
     * Reactive back button visibility provided by the child fragment.
     */
    @Composable
    open fun shouldShowBackButton(): Boolean = false

    /**
     * Logic for the Top Bar back arrow.
     */
    open fun onBackClick() {
        parentFragmentManager.popBackStack()
    }

    /**
     * Option 1: Fragment-based tab switching without XML.
     * It finds the ID of the container it is currently sitting in dynamically.
     */
    open fun onTabSelected(item: Item) {
        if (item.route == currentRoute) return

        val targetFragment = when (item.route) {
            "my" -> UserFragment()
            "result" -> CompilationFragment()
            else -> null
        }

        targetFragment?.let {
            // OPTION 1 FIX: Instead of R.id.main_container,
            // we find the ID of the view's parent programmatically.
            val containerId = (view?.parent as? ViewGroup)?.id ?: android.R.id.content

            parentFragmentManager.beginTransaction()
                .replace(containerId, it)
                .commit()
        }
    }

    /**
     * The main Compose content of the screen.
     */
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
                    val reactiveTitle = provideTitle()
                    val reactiveBack = shouldShowBackButton()

                    SRRBaseScreen(
                        title = reactiveTitle,
                        showBackButton = reactiveBack,
                        onBackClick = { onBackClick() },
                        navigationItems = listOf(
                            Item("my", "My", Icons.Default.Person),
                            Item("result", "Result", Icons.Default.List)
                        ),
                        currentRoute = currentRoute,
                        onTabClick = { selectedItem ->
                            onTabSelected(selectedItem)
                        },
                        content = { modifier -> ScreenContent(modifier) }
                    )
                }
            }
        }
    }
}