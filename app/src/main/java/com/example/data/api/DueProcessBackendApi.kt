package com.example.data.api

import com.example.data.models.CaseEntity
import com.example.data.models.DocumentEntity
import com.example.data.models.FindingEntity
import com.example.data.models.ReportEntity
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface DueProcessBackendApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): AuthResponse

    @GET("auth/me")
    suspend fun me(): MobileUser

    @GET("cases")
    suspend fun fetchCases(@Query("updated_since") updatedSince: Long? = null): List<CaseEntity>

    @GET("cases/{caseId}/documents")
    suspend fun getDocumentsForCase(
        @Path("caseId") caseId: String,
        @Query("updated_since") updatedSince: Long? = null
    ): List<DocumentEntity>

    @GET("cases/{caseId}/findings")
    suspend fun getFindings(
        @Path("caseId") caseId: String,
        @Query("updated_since") updatedSince: Long? = null
    ): List<FindingEntity>

    @POST("documents/upload-url")
    suspend fun requestUploadUrl(@Body request: UploadUrlRequest): UploadUrlResponse

    @PUT("documents/uploads/{uploadId}")
    suspend fun uploadToIssuedUrl(
        @Path("uploadId") uploadId: String,
        @Body body: RequestBody
    ): UploadResult

    @POST("documents/confirm")
    suspend fun confirmUploadComplete(@Body request: ConfirmUploadRequest): UploadResult

    @DELETE("documents/{documentId}")
    suspend fun deleteDocument(@Path("documentId") documentId: String): DeleteResponse

    @GET("agents/catalog")
    suspend fun getAgentCatalog(): AgentCatalogResponse

    @POST("agents/run")
    suspend fun startSwarmProtocol(@Body request: SwarmRequest): SwarmResponse

    @GET("agents/run/{runId}/status")
    suspend fun getSwarmStatus(@Path("runId") runId: String): SwarmStatusResponse

    @GET("reports")
    suspend fun getReports(): List<ReportEntity>

    @POST("reports")
    suspend fun generateReport(@Body request: GenerateReportRequest): GeneratedReportResponse

    @GET("reports/{reportId}/export")
    suspend fun exportReport(
        @Path("reportId") reportId: String,
        @Query("format") format: String = "markdown"
    ): ReportExportResponse
}

data class LoginRequest(
    val email: String? = null,
    val accessKey: String? = null,
    val name: String? = null
)

data class RefreshRequest(val refreshToken: String)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val user: MobileUser? = null
)

data class MobileUser(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val role: String? = null
)

data class UploadUrlRequest(
    val fileName: String,
    val mimeType: String,
    val caseId: String = "default"
)

data class UploadUrlResponse(
    val documentId: String,
    val uploadId: String,
    val method: String,
    val presignedUrl: String,
    val expiresInSeconds: Int,
    val requiresAuthorization: Boolean
)

data class ConfirmUploadRequest(val documentId: String)

data class UploadResult(
    val success: Boolean? = null,
    val extractionCompleted: Boolean? = null,
    val duplicate: Boolean? = null,
    val existingDocumentId: Int? = null,
    val fileUrl: String? = null,
    val documentHash: String? = null,
    val status: String? = null,
    val extractionMethod: String? = null,
    val extractionNote: String? = null,
    val textLength: Int? = null,
    val extractionQualityScore: Int? = null,
    val summary: String? = null
)

data class DeleteResponse(val success: Boolean)

data class AgentCatalogResponse(
    val agents: List<AgentOption> = emptyList(),
    val superAgents: List<SuperAgentOption> = emptyList()
)

data class AgentOption(
    val id: String,
    val name: String,
    val division: String? = null,
    val description: String = "",
    val capabilities: List<String> = emptyList()
)

data class SuperAgentOption(
    val id: String,
    val name: String,
    val description: String = "",
    val agentIds: List<String> = emptyList()
)

data class SwarmRequest(
    val caseId: String = "default",
    val scopeDocumentIds: List<String> = emptyList(),
    val agentTypes: List<String> = emptyList(),
    val sector: String = "legal",
    val scope: String? = null,
    val fromDate: String? = null,
    val toDate: String? = null
)

data class SwarmResponse(
    val runId: String,
    val status: String,
    val progress: Int,
    val logs: List<String>
)

data class SwarmStatusResponse(
    val runId: String,
    val progress: Int,
    val logs: List<String>,
    val status: String,
    val synthesis: String? = null
)

data class GenerateReportRequest(
    val scope: String = "case",
    val documentIds: List<Int> = emptyList(),
    val template: String = "executive_summary",
    val format: String = "markdown",
    val minConfidence: Int = 0,
    val includeSources: Boolean = true,
    val includeBlockedFindings: Boolean = false
)

data class GeneratedReportResponse(
    val reportId: Int,
    val fileName: String,
    val content: String
)

data class ReportExportResponse(
    val id: Int,
    val title: String,
    val fileName: String,
    val mimeType: String,
    val format: String,
    val encoding: String,
    val content: String
)
