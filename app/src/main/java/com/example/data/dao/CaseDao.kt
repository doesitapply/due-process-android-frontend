package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.models.CaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CaseDao {
    @Query("SELECT * FROM cases")
    fun getAllCases(): Flow<List<CaseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCases(cases: List<CaseEntity>)
}
