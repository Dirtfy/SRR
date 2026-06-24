package com.dirtfy.srr.core.repository

import com.dirtfy.srr.core.model.Evaluation

interface EvaluationRepository {
    // Writes (or overwrites) this user's evaluation for the given feature.
    suspend fun submitEvaluation(evaluation: Evaluation): Result<Unit>
    // Returns all users' evaluations for the given feature.
    // Returns an empty list (not failure) if no evaluations exist yet.
    suspend fun getEvaluationsForFeature(featureId: String): Result<List<Evaluation>>
}
