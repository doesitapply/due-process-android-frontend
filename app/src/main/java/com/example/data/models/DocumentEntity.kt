package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    val caseId: String,
    val title: String,
    val category: String,
    val status: String,
    val qualityScore: Int,
    val aiSummary: String,
    val dateAdded: String,
    val analysisReady: Boolean = false
)
