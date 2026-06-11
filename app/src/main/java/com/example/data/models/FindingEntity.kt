package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "findings")
data class FindingEntity(
    @PrimaryKey
    val id: String,
    val documentId: String,
    val tag: String,
    val title: String,
    val description: String,
    val severity: String,
    val confidenceScore: Int,
    val actionRequired: String,
    val status: String
)
