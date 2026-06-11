package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.models.FindingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FindingDao {
    @Query("SELECT * FROM findings")
    fun getAllFindings(): Flow<List<FindingEntity>>

    @Query("SELECT * FROM findings WHERE documentId = :documentId")
    fun getFindingsForDocument(documentId: String): Flow<List<FindingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFindings(findings: List<FindingEntity>)
}
