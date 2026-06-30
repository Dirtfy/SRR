package com.dirtfy.srr.core.repository

import android.net.Uri

interface StorageRepository {
    suspend fun uploadItemImage(uri: Uri): Result<String>
    suspend fun deleteImage(url: String): Result<Unit>
}
