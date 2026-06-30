package com.dirtfy.srr.core.repository

import com.dirtfy.srr.core.model.Evaluation

interface EvaluationRepository {
    // Writes (or overwrites) this user's evaluation for the given feature.
    suspend fun submitEvaluation(evaluation: Evaluation): Result<Unit>
    // Returns all users' evaluations for the given feature.
    // Returns an empty list (not failure) if no evaluations exist yet.
    suspend fun getEvaluationsForFeature(featureId: String): Result<List<Evaluation>>
    // Deletes all evaluations under the given feature (cascade on feature delete).
    suspend fun deleteEvaluationsForFeature(featureId: String): Result<Unit>
    // Removes the given itemId from every evaluation across the provided features (cascade on item delete).
    suspend fun removeItemFromEvaluations(itemId: String, featureIds: List<String>): Result<Unit>
}
