package com.dirtfy.srr.core.repository

import com.dirtfy.srr.core.model.Feature

interface FeatureRepository {
    suspend fun getAllFeatures(): Result<List<Feature>>
}
