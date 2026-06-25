package com.dirtfy.srr.remote.repository

import com.dirtfy.srr.core.model.Evaluation
import com.dirtfy.srr.core.repository.EvaluationRepository
import com.dirtfy.srr.remote.model.EvaluationRecord
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class RemoteEvaluationRepository : EvaluationRepository {

    private val db = Firebase.firestore

    // Path: evaluations/{featureId}/userEvaluations/{userId}
    private fun evaluationDoc(featureId: String, userId: String) =
        db.collection("evaluations").document(featureId)
            .collection("userEvaluations").document(userId)

    override suspend fun submitEvaluation(evaluation: Evaluation): Result<Unit> =
        runCatching {
            val record = EvaluationRecord(
                userId        = evaluation.userId,
                featureId     = evaluation.featureId,
                orderedItemIds = evaluation.orderedItemIds
            )
            evaluationDoc(evaluation.featureId, evaluation.userId)
                .set(record).await()
            Unit
        }

    override suspend fun getEvaluationsForFeature(featureId: String): Result<List<Evaluation>> =
        runCatching {
            db.collection("evaluations").document(featureId)
                .collection("userEvaluations")
                .get().await()
                .documents
                .map { doc ->
                    @Suppress("UNCHECKED_CAST")
                    Evaluation(
                        userId         = doc.getString("userId") ?: "",
                        featureId      = featureId,
                        orderedItemIds = (doc.get("orderedItemIds") as? List<String>) ?: emptyList()
                    )
                }
        }
}
