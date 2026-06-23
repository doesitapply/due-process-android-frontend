package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val template: String,
    val scope: String,
    val format: String,
    val fileName: String,
    val minConfidence: Int,
    val includeBlockedFindings: Boolean,
    val createdAt: String,
    val updatedAt: String
)
