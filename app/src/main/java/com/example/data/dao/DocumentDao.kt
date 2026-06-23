package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.models.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE caseId = :caseId")
    fun getDocumentsForCase(caseId: String): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<DocumentEntity>)

    @Query("DELETE FROM documents WHERE caseId = :caseId")
    suspend fun clearDocumentsForCase(caseId: String)

    @Transaction
    suspend fun replaceDocumentsForCase(caseId: String, documents: List<DocumentEntity>) {
        clearDocumentsForCase(caseId)
        insertDocuments(documents)
    }
}
