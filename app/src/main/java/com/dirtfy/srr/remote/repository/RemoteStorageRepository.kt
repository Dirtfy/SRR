package com.dirtfy.srr.remote.repository

import android.net.Uri
import com.dirtfy.srr.core.repository.StorageRepository
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RemoteStorageRepository : StorageRepository {

    private val storage = Firebase.storage

    override suspend fun uploadItemImage(uri: Uri): Result<String> = runCatching {
        val ref = storage.reference.child("items/${UUID.randomUUID()}.jpg")
        ref.putFile(uri).await()
        ref.downloadUrl.await().toString()
    }
}
