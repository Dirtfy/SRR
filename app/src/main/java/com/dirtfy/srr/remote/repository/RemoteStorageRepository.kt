package com.dirtfy.srr.remote.repository

import android.content.Context
import android.net.Uri
import com.dirtfy.srr.core.repository.StorageRepository
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RemoteStorageRepository(private val context: Context) : StorageRepository {

    override suspend fun uploadItemImage(uri: Uri): Result<String> = runCatching {
        val contentType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val metadata = StorageMetadata.Builder().setContentType(contentType).build()
        val ref = Firebase.storage.reference.child("items/${UUID.randomUUID()}.jpg")
        ref.putFile(uri, metadata).await()
        ref.downloadUrl.await().toString()
    }

    override suspend fun deleteImage(url: String): Result<Unit> = runCatching {
        Firebase.storage.getReferenceFromUrl(url).delete().await()
    }
}
