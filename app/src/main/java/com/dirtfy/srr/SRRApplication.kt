package com.dirtfy.srr

import android.app.Application
import com.dirtfy.srr.BuildConfig
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SRRApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.USE_EMULATOR) {
            // Disable local persistence so every Firestore write completes only after the
            // emulator server acknowledges it. Required for Source.SERVER reads to see
            // documents that were just written in the same test. Must be set BEFORE
            // useEmulator() — both require the Firestore client to be uninitialized.
            Firebase.firestore.firestoreSettings =
                FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(false)
                    .build()
            Firebase.auth.useEmulator("localhost", 9099)
            Firebase.firestore.useEmulator("localhost", 8080)
        }
    }
}
