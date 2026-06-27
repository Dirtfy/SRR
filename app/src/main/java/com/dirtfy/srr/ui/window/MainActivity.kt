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
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth

class MainActivity : FragmentActivity() {

    companion object {
        const val CONTAINER_ID = 10001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val rootContainer = FrameLayout(this).apply {
            id = CONTAINER_ID
        }

        setContentView(rootContainer)

        if (savedInstanceState == null) {
            navigateToFragment(LoginFragment.newInstance(autoLogin = true), addToBackStack = false)
        }
    }

    fun navigateToMine() {
        navigateToFragment(UserFragment(), addToBackStack = false)
    }

    fun logout() {
        navigateToFragment(LoginFragment(), addToBackStack = false)
    }

    fun signOut() {
        Firebase.auth.signOut()
        navigateToFragment(LoginFragment.newInstance(autoLogin = false), addToBackStack = false)
    }

    private fun navigateToFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            replace(CONTAINER_ID, fragment)
            if (addToBackStack) addToBackStack(null)
        }
    }
}
