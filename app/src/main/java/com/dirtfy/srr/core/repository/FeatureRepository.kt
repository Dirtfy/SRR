package com.dirtfy.srr.core.repository

import com.dirtfy.srr.core.model.Feature

interface FeatureRepository {
    suspend fun getAllFeatures(): Result<List<Feature>>
    suspend fun createFeature(name: String, createdBy: String): Result<Feature>
    suspend fun deleteFeature(id: String): Result<Unit>
}
