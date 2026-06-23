package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.CaseDao
import com.example.data.dao.DocumentDao
import com.example.data.dao.FindingDao
import com.example.data.dao.ReportDao
import com.example.data.models.CaseEntity
import com.example.data.models.DocumentEntity
import com.example.data.models.FindingEntity
import com.example.data.models.ReportEntity

@Database(
    entities = [CaseEntity::class, DocumentEntity::class, FindingEntity::class, ReportEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun caseDao(): CaseDao
    abstract fun documentDao(): DocumentDao
    abstract fun findingDao(): FindingDao
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dueprocess_database"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
