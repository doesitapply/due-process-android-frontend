package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.repository.CaseRepository
import kotlinx.coroutines.launch
import java.io.File

data class DocumentItem(
    val id: String,
    val title: String,
    val caseName: String,
    val category: String,
    val status: String,
    val quality: Int,
    val aiSummary: String,
    val dateAdded: String,
    val analysisReady: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidenceInboxScreen(navController: NavController, repository: CaseRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Filings", "Evidence", "Transcripts", "Media")
    val documents by repository.localDocuments.collectAsState(initial = emptyList())
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var deletingDocumentId by remember { mutableStateOf<String?>(null) }

    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isUploading = true
            statusMessage = null
            try {
                val tempFile = copyUriToTempFile(context, uri)
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                repository.uploadEvidenceFile(file = tempFile, mimeType = mimeType)
                statusMessage = "Uploaded and processed ${tempFile.name}."
            } catch (error: Exception) {
                statusMessage = error.message ?: "Upload failed."
            } finally {
                isUploading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        runCatching { repository.refreshDocumentsFromServer() }
    }

    val allDocuments = documents.map { document ->
        DocumentItem(
            id = document.id,
            title = document.title,
            caseName = document.caseId,
            category = document.category,
            status = document.status,
            quality = document.qualityScore,
            aiSummary = document.aiSummary,
            dateAdded = document.dateAdded.take(10),
            analysisReady = document.analysisReady
        )
    }

    val filteredDocs = if (selectedCategory == "All") allDocuments else allDocuments.filter { it.category == selectedCategory }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evidence Intake", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary) },
                actions = {
                    IconButton(
                        enabled = !isUploading,
                        onClick = { documentPicker.launch(arrayOf("*/*")) }
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Upload evidence")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by name, case, or keyword...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )

            // Category Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${documents.count { it.analysisReady }} ready / ${documents.size} total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Agents only run on completed, source-anchored files.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    enabled = !isUploading,
                    onClick = { documentPicker.launch(arrayOf("*/*")) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isUploading) "Uploading" else "Upload")
                }
            }

            if (statusMessage != null) {
                Text(
                    statusMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (statusMessage?.contains("failed", true) == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Documents List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredDocs.isEmpty()) {
                    item {
                        Text("No synced evidence yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(filteredDocs, key = { it.id }) { doc ->
                        DocumentCard(
                            doc = doc,
                            isDeleting = deletingDocumentId == doc.id,
                            onDelete = {
                                scope.launch {
                                    deletingDocumentId = doc.id
                                    statusMessage = null
                                    try {
                                        repository.deleteEvidenceDocument(documentId = doc.id)
                                        statusMessage = "Deleted ${doc.title}."
                                    } catch (error: Exception) {
                                        statusMessage = error.message ?: "Delete failed."
                                    } finally {
                                        deletingDocumentId = null
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentCard(doc: DocumentItem, isDeleting: Boolean, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                1.dp, 
                if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, 
                RoundedCornerShape(12.dp)
            )
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when(doc.category) {
                            "Filings" -> Icons.Default.Gavel
                            "Transcripts" -> Icons.Default.Forum
                            "Media" -> Icons.Default.PermMedia
                            else -> Icons.AutoMirrored.Filled.InsertDriveFile
                        }
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column {
                        Text(doc.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${doc.caseName}  •  ${doc.category}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    val normalizedStatus = when {
                        doc.analysisReady -> "READY"
                        doc.status.equals("completed", true) -> "NEEDS TEXT"
                        doc.status.equals("processing", true) -> "PROCESSING"
                        doc.status.equals("failed", true) -> "FAILED"
                        else -> doc.status.uppercase()
                    }
                    val statusColor = when {
                        doc.analysisReady -> MaterialTheme.colorScheme.primary
                        doc.status.equals("processing", true) -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                    Text(normalizedStatus, style = MaterialTheme.typography.labelSmall, color = statusColor)
                    if (doc.quality > 0) {
                        Text("Quality: ${doc.quality}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text("AI Summary", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        doc.aiSummary.ifBlank { "No summary available yet." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Added ${doc.dateAdded}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.weight(1f))
                        OutlinedButton(
                            onClick = { /* Open Document */ },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text("Details")
                        }
                        OutlinedButton(
                            enabled = !isDeleting,
                            onClick = onDelete,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(if (isDeleting) "Deleting" else "Delete")
                        }
                    }
                }
            }
        }
    }
}

private fun copyUriToTempFile(context: Context, uri: Uri): File {
    val displayName = queryDisplayName(context, uri)
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .ifBlank { "evidence-upload" }
    val target = File(context.cacheDir, "dueprocess-upload-${System.currentTimeMillis()}-$displayName")
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Unable to open selected file." }
        target.outputStream().use { output -> input.copyTo(output) }
    }
    return target
}

private fun queryDisplayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) {
            return cursor.getString(index) ?: "evidence-upload"
        }
    }
    return uri.lastPathSegment ?: "evidence-upload"
}
