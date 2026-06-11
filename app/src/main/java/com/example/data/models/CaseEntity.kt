package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cases")
data class CaseEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val court: String,
    val documentCount: Int,
    val findingCount: Int,
    val date: String,
    val status: String,
    val isActive: Boolean
)
