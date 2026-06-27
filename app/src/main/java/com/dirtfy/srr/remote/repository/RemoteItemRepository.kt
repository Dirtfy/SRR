package com.dirtfy.srr.remote.repository

import com.dirtfy.srr.core.model.Item
import com.dirtfy.srr.core.repository.ItemRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class RemoteItemRepository : ItemRepository {

    private val db = Firebase.firestore

    override suspend fun getAllItems(): Result<List<Item>> =
        runCatching {
            db.collection("items")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get().await()
                .documents
                .map { doc ->
                    Item(
                        id        = doc.id,
                        name      = doc.getString("name") ?: doc.id,
                        createdBy = doc.getString("createdBy") ?: ""
                    )
                }
        }

    override suspend fun createItem(name: String, createdBy: String): Result<Item> =
        runCatching {
            val ref = db.collection("items")
                .add(hashMapOf(
                    "name"      to name,
                    "createdBy" to createdBy,
                    "createdAt" to FieldValue.serverTimestamp()
                ))
                .await()
            Item(id = ref.id, name = name, createdBy = createdBy)
        }

    override suspend fun deleteItem(id: String): Result<Unit> =
        runCatching {
            db.collection("items").document(id).delete().await()
        }
}
