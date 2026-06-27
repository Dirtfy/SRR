package com.dirtfy.srr.core.repository

import com.dirtfy.srr.core.model.Item

interface ItemRepository {
    suspend fun getAllItems(): Result<List<Item>>
    suspend fun createItem(name: String, createdBy: String): Result<Item>
    suspend fun deleteItem(id: String): Result<Unit>
}
