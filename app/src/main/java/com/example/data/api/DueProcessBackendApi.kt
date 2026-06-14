package com.example.data.api

import com.example.data.models.CaseEntity
import com.example.data.models.DocumentEntity
import com.example.data.models.FindingEntity

/**
 * ANDROID DEVELOPER: 
 * This is the blueprint for the backend API connection.
 * Once Retrofit is added to app/build.gradle.kts, annotate these 
 * methods with @GET, @POST, @Body, etc.
 */
interface DueProcessBackendApi {
    
    // Auth
    suspend fun login(credentials: Map<String, String>): AuthResponse

    // Cases
    suspend fun fetchCases(updatedSince: Long? = null): List<CaseEntity>
    
    // Documents
    suspend fun getDocumentsForCase(caseId: String): List<DocumentEntity>
    
    // Presigned Upload Flow
    suspend fun requestUploadUrl(request: UploadUrlRequest): UploadUrlResponse
    suspend fun confirmUploadComplete(documentId: String)

    // Agents
    suspend fun startSwarmProtocol(request: SwarmRequest): SwarmResponse
    suspend fun getSwarmStatus(runId: String): SwarmStatusResponse

    // Findings
    suspend fun getFindings(caseId: String): List<FindingEntity>
}

// Data Transfer Objects (DTOs)
data class AuthResponse(val accessToken: String, val refreshToken: String, val userId: String)
data class UploadUrlRequest(val fileName: String, val mimeType: String, val caseId: String)
data class UploadUrlResponse(val documentId: String, val presignedUrl: String)
data class SwarmRequest(val caseId: String, val scopeDocumentIds: List<String>, val agentTypes: List<String>)
data class SwarmResponse(val runId: String, val status: String)
data class SwarmStatusResponse(val runId: String, val progress: Int, val logs: List<String>, val status: String)
