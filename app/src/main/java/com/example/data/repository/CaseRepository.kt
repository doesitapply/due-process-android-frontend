package com.example.data.repository

import com.example.data.api.DueProcessBackendApi
import com.example.data.dao.CaseDao
import com.example.data.models.CaseEntity
import kotlinx.coroutines.flow.Flow

/**
 * ANDROID DEVELOPER: 
 * This Repository pattern is the secret to a corruption-proof app.
 * It sits between your UI, your local SQLite Room database, and the Backend API.
 */
class CaseRepository(
    private val caseDao: CaseDao,
    private val api: DueProcessBackendApi
) {
    /**
     * 1. UI observes this Flow. 
     * It ALWAYS reads from the local database, ensuring instant load screens and offline mode.
     */
    val localCases: Flow<List<CaseEntity>> = caseDao.getAllCases()

    /**
     * 2. ViewModels call this to sync with the backend in the background.
     */
    suspend fun refreshCasesFromServer() {
        try {
            // Fetch updated cases from the backend API
            val remoteCases = api.fetchCases()
            
            // Save them to the local SQLite database.
            // Room will automatically notify the `localCases` Flow, instantly updating the UI!
            caseDao.insertCases(remoteCases)
        } catch (e: Exception) {
            // Handle Network Error (e.g., user is offline, plane mode)
            // The UI will just safely continue displaying whatever is currently in the local DB.
            e.printStackTrace()
        }
    }
}
