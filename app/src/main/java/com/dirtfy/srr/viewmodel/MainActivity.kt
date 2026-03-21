package com.dirtfy.srr.viewmodel

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.dirtfy.srr.viewmodel.login.LoginFragment
import com.dirtfy.srr.viewmodel.user.UserFragment

class MainActivity : FragmentActivity() {

    // Use a fixed ID constant so the FragmentManager can find the container
    // even after the screen rotates or the Activity is recreated.
    private val containerId = 10001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup the root container with the fixed ID
        val rootContainer = FrameLayout(this).apply {
            id = containerId
        }
        setContentView(rootContainer)

        // Initial navigation: Load Login screen
        if (savedInstanceState == null) {
            navigateToFragment(LoginFragment(), addToBackStack = false)
        }
    }

    /**
     * Public navigation methods called by Fragments
     */
    fun navigateToMine() {
        // Switch from Login flow to the Mine/Main flow
        navigateToFragment(UserFragment(), addToBackStack = false)
    }

    fun logout() {
        // Go back to login and clear the backstack
        navigateToFragment(LoginFragment(), addToBackStack = false)
    }

    /**
     * Core Fragment Controller Logic
     */
    private fun navigateToFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)

            // FIX: Use setTransition for standard Android animations.
            // This avoids the 'Unresolved reference' error caused by
            // trying to access internal library animation resources.
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)

            replace(containerId, fragment)

            if (addToBackStack) {
                addToBackStack(null)
            }
        }
    }
}