package com.dirtfy.srr.remote.repository

import com.dirtfy.srr.core.model.Item
import com.dirtfy.srr.core.repository.ItemRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class RemoteItemRepository : ItemRepository {

    private val db = Firebase.firestore

    override suspend fun getAllItems(): Result<List<Item>> =
        runCatching {
            db.collection("items")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get(Source.SERVER).await()
                .documents
                .map { doc ->
                    Item(
                        id        = doc.id,
                        name      = doc.getString("name") ?: doc.id,
                        createdBy = doc.getString("createdBy") ?: "",
                        imageUrl  = doc.getString("imageUrl")
                    )
                }
        }

    override suspend fun createItem(name: String, createdBy: String, imageUrl: String?): Result<Item> =
        runCatching {
            val trimmed   = name.trim()
            val nameLower = trimmed.lowercase()
            // Source.SERVER bypasses in-memory cache (stale after emulator reset in tests)
            // and reads the authoritative emulator/server state. Since setPersistenceEnabled(false)
            // is set in SRRApplication, writes complete only after server ack, so this read
            // immediately sees any document that was just written.
            val all = db.collection("items").get(Source.SERVER).await()
            if (all.documents.any { it.getString("nameLower") == nameLower })
                throw IllegalArgumentException("An item named \"$trimmed\" already exists")
            val data = hashMapOf(
                "name"      to trimmed,
                "nameLower" to nameLower,
                "createdBy" to createdBy,
                "createdAt" to FieldValue.serverTimestamp()
            )
            val url = imageUrl?.trim()?.takeIf { it.isNotEmpty() }
            if (url != null) data["imageUrl"] = url
            val ref = db.collection("items").add(data).await()
            Item(id = ref.id, name = trimmed, createdBy = createdBy, imageUrl = url)
        }

    override suspend fun deleteItem(id: String): Result<Unit> =
        runCatching {
            db.collection("items").document(id).delete().await()
        }
}
