package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.models.ReportEntity
import com.example.data.repository.CaseRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsDashboardScreen(navController: NavController, repository: CaseRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reports by repository.localReports.collectAsState(initial = emptyList())
    val documents by repository.localDocuments.collectAsState(initial = emptyList())
    val readyDocuments = documents.filter { it.analysisReady }
    val selectedDocumentIds = remember { mutableStateListOf<String>() }
    var selectedTemplate by remember { mutableStateOf("War Room Summary") }
    val templates = listOf("War Room Summary", "Timeline/Gaps", "Discovery Demands", "Immunity Pathway", "Court Packet")
    var selectedScope by remember { mutableStateOf("Full Case") }
    var confidenceMin by remember { mutableStateOf(85f) }
    var includeBlocked by remember { mutableStateOf(false) }
    var includeSource by remember { mutableStateOf(true) }
    var isGenerating by remember { mutableStateOf(false) }
    var exportingReportId by remember { mutableStateOf<String?>(null) }
    var pendingExportContent by remember { mutableStateOf<String?>(null) }
    var pendingExportFileName by remember { mutableStateOf("dueprocess-report.md") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri: Uri? ->
        val content = pendingExportContent
        if (uri != null && content != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri).use { output ->
                    requireNotNull(output) { "Unable to open export destination." }
                    output.write(content.toByteArray())
                }
            }.onSuccess {
                statusMessage = "Report exported to selected file."
            }.onFailure { error ->
                statusMessage = error.message ?: "Report export failed."
            }
        }
        pendingExportContent = null
    }

    LaunchedEffect(Unit) {
        runCatching {
            repository.refreshReportsFromServer()
            repository.refreshDocumentsFromServer()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DueProcess AI", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text("Reports Builder", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("Configure and generate structured evidence reports.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            item {
                SectionCard(title = "Template") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(templates) { template ->
                            FilterChip(
                                selected = selectedTemplate == template,
                                onClick = { selectedTemplate = template },
                                label = { Text(template, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Scope") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        ScopeButton("Selected Files", selectedScope == "Selected Files", Modifier.weight(1f)) { selectedScope = "Selected Files" }
                        ScopeButton("Full Case", selectedScope == "Full Case", Modifier.weight(1f)) { selectedScope = "Full Case" }
                    }

                    if (selectedScope == "Selected Files") {
                        Spacer(modifier = Modifier.height(16.dp))
                        if (readyDocuments.isEmpty()) {
                            Text("No ready files are available for report scope.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                readyDocuments.take(8).forEach { document ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(
                                            checked = selectedDocumentIds.contains(document.id),
                                            onCheckedChange = { checked ->
                                                if (checked) selectedDocumentIds.add(document.id) else selectedDocumentIds.remove(document.id)
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                        )
                                        Column {
                                            Text(document.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                            Text("${document.category} • ${document.qualityScore}% OCR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Filters & Constraints", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Confidence Minimum", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        Text("${confidenceMin.toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = confidenceMin,
                        onValueChange = { confidenceMin = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primaryContainer,
                            activeTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Include Blocked Findings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        Switch(checked = includeBlocked, onCheckedChange = { includeBlocked = it })
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Include Source Appendix", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        Switch(checked = includeSource, onCheckedChange = { includeSource = it })
                    }
                }
            }

            item {
                SectionCard(title = "Saved Reports (${reports.size})") {
                    if (reports.isEmpty()) {
                        Text("No synced reports yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            reports.take(6).forEach { report ->
                                PreviewItem(
                                    report = report,
                                    isExporting = exportingReportId == report.id,
                                    onExport = {
                                        scope.launch {
                                            exportingReportId = report.id
                                            statusMessage = null
                                            try {
                                                val exported = repository.exportReport(report.id, "markdown")
                                                pendingExportContent = exported.content
                                                pendingExportFileName = exported.fileName.ifBlank { report.fileName.ifBlank { "dueprocess-report.md" } }
                                                exportLauncher.launch(pendingExportFileName)
                                            } catch (error: Exception) {
                                                statusMessage = error.message ?: "Report export failed."
                                            } finally {
                                                exportingReportId = null
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (statusMessage != null) {
                item {
                    Text(statusMessage ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            item {
                Button(
                    onClick = {
                        isGenerating = true
                        statusMessage = null
                        scope.launch {
                            try {
                                repository.generateReport(
                                    scope = if (selectedScope == "Full Case") "case" else "files",
                                    documentIds = if (selectedScope == "Full Case") emptyList() else selectedDocumentIds.mapNotNull { it.toIntOrNull() },
                                    template = templateKey(selectedTemplate),
                                    minConfidence = confidenceMin.toInt(),
                                    includeSources = includeSource,
                                    includeBlockedFindings = includeBlocked
                                )
                                statusMessage = "Report generated and synced."
                            } catch (error: Exception) {
                                statusMessage = error.message ?: "Report generation failed."
                            } finally {
                                isGenerating = false
                            }
                        }
                    },
                    enabled = !isGenerating && (selectedScope == "Full Case" || selectedDocumentIds.isNotEmpty()),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isGenerating) "Generating..." else "Generate Report", style = MaterialTheme.typography.headlineSmall)
                }
                if (selectedScope == "Selected Files" && selectedDocumentIds.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Choose at least one ready file for selected-file reports.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun ScopeButton(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PreviewItem(report: ReportEntity, isExporting: Boolean, onExport: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(4.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = true, onCheckedChange = { }, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary))
        Column(modifier = Modifier.weight(1f)) {
            Text(report.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                    Text(report.fileName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                    Text(report.format.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        OutlinedButton(enabled = !isExporting, onClick = onExport, shape = RoundedCornerShape(6.dp)) {
            Text(if (isExporting) "Exporting" else "Export")
        }
    }
}

private fun templateKey(label: String): String {
    return when (label) {
        "Timeline/Gaps" -> "evidence_chronology"
        "Discovery Demands" -> "discovery_demands"
        "Immunity Pathway" -> "immunity_relief"
        "Court Packet" -> "court_packet"
        else -> "executive_summary"
    }
}
