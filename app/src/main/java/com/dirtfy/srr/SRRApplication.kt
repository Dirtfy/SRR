package com.dirtfy.srr

import android.app.Application
import com.dirtfy.srr.BuildConfig
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SRRApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            // Route debug builds to the Firebase Local Emulator Suite.
            // Requires `adb reverse tcp:9099 tcp:9099 && adb reverse tcp:8080 tcp:8080` on the host
            // before launching (works for both physical devices and AVDs).
            Firebase.auth.useEmulator("localhost", 9099)
            Firebase.firestore.useEmulator("localhost", 8080)
        }
    }
}
