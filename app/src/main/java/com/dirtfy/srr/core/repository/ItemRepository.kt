package com.dirtfy.srr.core.repository

import com.dirtfy.srr.core.model.Item

interface ItemRepository {
    suspend fun getAllItems(): Result<List<Item>>
    suspend fun createItem(name: String, createdBy: String, imageUrl: String? = null): Result<Item>
    suspend fun deleteItem(id: String): Result<Unit>
    suspend fun updateItemImage(id: String, imageUrl: String?): Result<Unit>
}
