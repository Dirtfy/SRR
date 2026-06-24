package com.dirtfy.srr.remote.repository

import com.dirtfy.srr.core.model.Item
import com.dirtfy.srr.core.repository.ItemRepository
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class RemoteItemRepository : ItemRepository {

    private val db = Firebase.firestore

    override suspend fun getAllItems(): Result<List<Item>> =
        runCatching {
            db.collection("items")
                .get().await()
                .documents
                .map { doc ->
                    Item(
                        id   = doc.id,
                        name = doc.getString("name") ?: doc.id
                    )
                }
        }
}
