package com.wordwaverise.wordwaveriseapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serverId: Int? = null,
    val name: String,
    val color: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
