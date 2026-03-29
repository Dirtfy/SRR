package com.dirtfy.srr.ui.window

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.dirtfy.srr.ui.window.component.LoginFragment
import com.dirtfy.srr.ui.window.component.UserFragment

class MainActivity : FragmentActivity() {

    /**
     * This ID is used by BaseFragment to find the container to replace itself.
     * We use a fixed integer so it remains consistent during configuration changes.
     */
    companion object {
        const val CONTAINER_ID = 10001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create the root container programmatically (No XML)
        val rootContainer = FrameLayout(this).apply {
            id = CONTAINER_ID
        }

        setContentView(rootContainer)

        // Initial navigation: Load Login screen if first launch
        if (savedInstanceState == null) {
            navigateToFragment(LoginFragment(), addToBackStack = false)
        }
    }

    /**
     * Public navigation method to switch to the Main "My" flow
     */
    fun navigateToMine() {
        navigateToFragment(UserFragment(), addToBackStack = false)
    }

    /**
     * Public navigation method to return to Login
     */
    fun logout() {
        navigateToFragment(LoginFragment(), addToBackStack = false)
    }

    /**
     * Core Fragment Controller Logic
     */
    private fun navigateToFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)

            // Provides a standard fade/slide transition between screens
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)

            replace(CONTAINER_ID, fragment)

            if (addToBackStack) {
                addToBackStack(null)
            }
        }
    }
}