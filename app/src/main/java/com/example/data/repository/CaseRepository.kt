package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.AuthTokenStore
import com.example.data.api.ConfirmUploadRequest
import com.example.data.api.DueProcessBackendApi
import com.example.data.api.GenerateReportRequest
import com.example.data.api.LoginRequest
import com.example.data.api.ReportExportResponse
import com.example.data.api.SwarmRequest
import com.example.data.api.SwarmResponse
import com.example.data.api.SwarmStatusResponse
import com.example.data.api.UploadUrlRequest
import com.example.data.dao.CaseDao
import com.example.data.dao.DocumentDao
import com.example.data.dao.FindingDao
import com.example.data.dao.ReportDao
import com.example.data.models.CaseEntity
import com.example.data.models.DocumentEntity
import com.example.data.models.FindingEntity
import com.example.data.models.ReportEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class CaseRepository(
    private val caseDao: CaseDao,
    private val documentDao: DocumentDao,
    private val findingDao: FindingDao,
    private val reportDao: ReportDao,
    private val api: DueProcessBackendApi,
    private val tokenStore: AuthTokenStore
) {
    val localCases: Flow<List<CaseEntity>> = caseDao.getAllCases()
    val localDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()
    val localFindings: Flow<List<FindingEntity>> = findingDao.getAllFindings()
    val localReports: Flow<List<ReportEntity>> = reportDao.getAllReports()

    fun hasSession(): Boolean = !tokenStore.accessToken().isNullOrBlank()

    private fun defaultAccessKey(): String? {
        return BuildConfig.DUEPROCESS_MOBILE_ACCESS_KEY
            .takeUnless { it.isBlank() || it == "__NONE__" }
    }

    suspend fun login(email: String?, accessKey: String?) {
        val response = api.login(
            LoginRequest(
                email = email?.ifBlank { null },
                accessKey = accessKey?.ifBlank { defaultAccessKey() }
            )
        )
        tokenStore.save(response.accessToken, response.refreshToken)
    }

    fun logout() {
        tokenStore.clear()
    }

    suspend fun refreshAll(caseId: String = "default") {
        refreshCasesFromServer()
        refreshDocumentsFromServer(caseId)
        refreshFindingsFromServer(caseId)
        refreshReportsFromServer()
    }

    suspend fun refreshCasesFromServer() {
        val remoteCases = api.fetchCases()
        caseDao.replaceCases(remoteCases)
    }

    suspend fun refreshDocumentsFromServer(caseId: String = "default") {
        val remoteDocuments = api.getDocumentsForCase(caseId)
        documentDao.replaceDocumentsForCase(caseId, remoteDocuments)
    }

    suspend fun refreshFindingsFromServer(caseId: String = "default") {
        val remoteFindings = api.getFindings(caseId)
        findingDao.replaceFindings(remoteFindings)
    }

    suspend fun refreshReportsFromServer() {
        val remoteReports = api.getReports()
        reportDao.replaceReports(remoteReports)
    }

    suspend fun uploadEvidenceFile(caseId: String = "default", file: File, mimeType: String) {
        val issued = api.requestUploadUrl(
            UploadUrlRequest(
                fileName = file.name,
                mimeType = mimeType,
                caseId = caseId
            )
        )
        api.uploadToIssuedUrl(
            uploadId = issued.uploadId,
            body = file.asRequestBody(mimeType.toMediaTypeOrNull())
        )
        api.confirmUploadComplete(ConfirmUploadRequest(issued.documentId))
        refreshDocumentsFromServer(caseId)
    }

    suspend fun deleteEvidenceDocument(caseId: String = "default", documentId: String) {
        api.deleteDocument(documentId)
        refreshDocumentsFromServer(caseId)
    }

    suspend fun fetchAgentCatalog() = api.getAgentCatalog()

    suspend fun startLegalAnalysis(
        caseId: String = "default",
        documentIds: List<String> = emptyList(),
        agentTypes: List<String> = emptyList(),
        scope: String? = null
    ) = api.startSwarmProtocol(
        SwarmRequest(
            caseId = caseId,
            scopeDocumentIds = documentIds,
            agentTypes = agentTypes,
            sector = "legal",
            scope = scope
        )
    )

    suspend fun pollAnalysis(runId: String) = api.getSwarmStatus(runId)

    suspend fun runLegalAnalysisAndPoll(
        caseId: String = "default",
        documentIds: List<String> = emptyList(),
        agentTypes: List<String> = emptyList(),
        scope: String? = null,
        onUpdate: (SwarmStatusResponse) -> Unit = {}
    ): SwarmResponse {
        val started = startLegalAnalysis(
            caseId = caseId,
            documentIds = documentIds,
            agentTypes = agentTypes,
            scope = scope
        )
        var lastStatus = SwarmStatusResponse(
            runId = started.runId,
            progress = started.progress,
            logs = started.logs,
            status = started.status
        )
        onUpdate(lastStatus)

        var attempts = 0
        while (
            attempts < 20 &&
            !lastStatus.status.equals("completed", true) &&
            !lastStatus.status.equals("failed", true)
        ) {
            delay(1_500)
            lastStatus = pollAnalysis(started.runId)
            onUpdate(lastStatus)
            attempts += 1
        }

        refreshFindingsFromServer(caseId)
        refreshReportsFromServer()
        return started
    }

    suspend fun generateReport(
        caseId: String = "default",
        scope: String = "case",
        documentIds: List<Int> = emptyList(),
        template: String = "executive_summary",
        minConfidence: Int = 0,
        includeSources: Boolean = true,
        includeBlockedFindings: Boolean = false
    ) {
        api.generateReport(
            GenerateReportRequest(
                scope = scope,
                documentIds = documentIds,
                template = template,
                format = "markdown",
                minConfidence = minConfidence,
                includeSources = includeSources,
                includeBlockedFindings = includeBlockedFindings
            )
        )
        refreshReportsFromServer()
    }

    suspend fun generateExecutiveReport(caseId: String = "default") {
        generateReport(caseId = caseId)
    }

    suspend fun exportReport(reportId: String, format: String = "markdown"): ReportExportResponse {
        return api.exportReport(reportId, format)
    }
}
