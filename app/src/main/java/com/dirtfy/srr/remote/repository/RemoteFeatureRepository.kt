package com.dirtfy.srr.remote.repository

import com.dirtfy.srr.core.model.Feature
import com.dirtfy.srr.core.repository.FeatureRepository
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class RemoteFeatureRepository : FeatureRepository {

    private val db = Firebase.firestore

    override suspend fun getAllFeatures(): Result<List<Feature>> =
        runCatching {
            db.collection("features")
                .get().await()
                .documents
                .map { doc ->
                    Feature(
                        id        = doc.id,
                        name      = doc.getString("name") ?: doc.id,
                        createdBy = doc.getString("createdBy") ?: ""
                    )
                }
        }

    override suspend fun createFeature(name: String, createdBy: String): Result<Feature> =
        runCatching {
            val ref = db.collection("features")
                .add(hashMapOf("name" to name, "createdBy" to createdBy))
                .await()
            Feature(id = ref.id, name = name, createdBy = createdBy)
        }

    override suspend fun deleteFeature(id: String): Result<Unit> =
        runCatching {
            db.collection("features").document(id).delete().await()
        }
}
