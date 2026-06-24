package com.dirtfy.srr.remote.repository

import com.dirtfy.srr.core.repository.UserAccountRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class RemoteUserAccountRepository : UserAccountRepository {

    private val auth = Firebase.auth

    override suspend fun signIn(email: String, password: String): Result<Unit> =
        runCatching {
            auth.signInWithEmailAndPassword(email, password).await()
            Unit
        }

    override suspend fun signUp(email: String, password: String): Result<Unit> =
        runCatching {
            auth.createUserWithEmailAndPassword(email, password).await()
            Unit
        }

    override fun signOut() {
        auth.signOut()
    }

    override fun currentUserId(): String? = auth.currentUser?.uid
}
