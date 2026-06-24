package com.dirtfy.srr.core.repository

interface UserAccountRepository {
    // Returns Result.failure if credentials are wrong or the network is unavailable.
    suspend fun signIn(email: String, password: String): Result<Unit>
    // Returns Result.failure if email already exists or password is too weak.
    suspend fun signUp(email: String, password: String): Result<Unit>
    fun signOut()
    // Returns null if no user is currently signed in.
    fun currentUserId(): String?
}
