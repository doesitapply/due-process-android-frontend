package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.api.AgentOption
import com.example.data.api.SuperAgentOption
import com.example.data.models.DocumentEntity
import com.example.data.repository.CaseRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentRunnerScreen(navController: NavController, repository: CaseRepository) {
    val scope = rememberCoroutineScope()
    val documents by repository.localDocuments.collectAsState(initial = emptyList())
    val readyDocuments = documents.filter { it.analysisReady }
    val selectedDocumentIds = remember { mutableStateListOf<String>() }
    val selectedAgentIds = remember { mutableStateListOf<String>() }
    var selectedScope by remember { mutableStateOf("all") }
    var agents by remember { mutableStateOf<List<AgentOption>>(emptyList()) }
    var superAgents by remember { mutableStateOf<List<SuperAgentOption>>(emptyList()) }
    var isLoadingCatalog by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var runStatus by remember { mutableStateOf<String?>(null) }
    var runLogs by remember { mutableStateOf<List<String>>(emptyList()) }

    fun refreshCatalogAndDocuments() {
        scope.launch {
            isLoadingCatalog = true
            try {
                repository.refreshDocumentsFromServer()
                val catalog = repository.fetchAgentCatalog()
                agents = catalog.agents
                superAgents = catalog.superAgents
                runStatus = if (catalog.agents.isEmpty()) {
                    "Standard mode: backend will choose the legal agent team for your plan."
                } else {
                    "Admin catalog loaded: ${catalog.agents.size} agents, ${catalog.superAgents.size} strike teams."
                }
            } catch (error: Exception) {
                runStatus = error.message ?: "Unable to sync agent catalog."
            } finally {
                isLoadingCatalog = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshCatalogAndDocuments()
    }

    val scopedDocumentIds = if (selectedScope == "file") selectedDocumentIds.toList() else emptyList()
    val canRun = !isRunning &&
        readyDocuments.isNotEmpty() &&
        (selectedScope == "all" || selectedDocumentIds.isNotEmpty())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Legal Analysis", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary) },
                actions = {
                    IconButtonCompat(enabled = !isLoadingCatalog, onClick = { refreshCatalogAndDocuments() })
                },
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Text(
                    "Run source-bound legal agents only against processed evidence. Whole case is the default; selected files are safer when you are testing one issue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                AnalysisSection(title = "Scope") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedScope == "all",
                            onClick = { selectedScope = "all" },
                            label = { Text("Whole case") },
                            leadingIcon = if (selectedScope == "all") ({ Icon(Icons.Default.Check, null) }) else null
                        )
                        FilterChip(
                            selected = selectedScope == "file",
                            onClick = { selectedScope = "file" },
                            label = { Text("Selected files") },
                            leadingIcon = if (selectedScope == "file") ({ Icon(Icons.Default.Check, null) }) else null
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "${readyDocuments.size} ready / ${documents.size} total files",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (readyDocuments.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    if (selectedScope == "file") {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (readyDocuments.isEmpty()) {
                            Text("No files are ready for analysis yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                readyDocuments.take(8).forEach { document ->
                                    DocumentScopeRow(
                                        document = document,
                                        selected = selectedDocumentIds.contains(document.id),
                                        onCheckedChange = { checked ->
                                            if (checked) selectedDocumentIds.add(document.id) else selectedDocumentIds.remove(document.id)
                                        }
                                    )
                                }
                                if (readyDocuments.size > 8) {
                                    Text("+${readyDocuments.size - 8} more ready files. Use whole case for the full set.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            item {
                AnalysisSection(title = "Recommended teams") {
                    if (superAgents.isEmpty()) {
                        Text("Admin strike-team catalog is not available for this login. The backend will run the default legal team.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            superAgents.forEach { team ->
                                SuperAgentCard(
                                    team = team,
                                    selected = team.agentIds.isNotEmpty() && team.agentIds.all { selectedAgentIds.contains(it) },
                                    onUseTeam = {
                                        selectedAgentIds.clear()
                                        selectedAgentIds.addAll(team.agentIds)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                AnalysisSection(title = "Agents") {
                    if (agents.isEmpty()) {
                        Text("No custom agent list available. This is normal for non-admin plans.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            agents.forEach { agent ->
                                AgentCard(
                                    title = agent.name,
                                    description = agent.description,
                                    icon = iconForAgent(agent),
                                    active = selectedAgentIds.contains(agent.id),
                                    onActiveChange = { checked ->
                                        if (checked) selectedAgentIds.add(agent.id) else selectedAgentIds.remove(agent.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                if (isRunning) {
                    ExecutionStatusPanel(progress = progress, status = runStatus, logs = runLogs)
                } else {
                    Button(
                        onClick = {
                            isRunning = true
                            progress = 0
                            runStatus = null
                            runLogs = emptyList()
                            scope.launch {
                                try {
                                    val result = repository.runLegalAnalysisAndPoll(
                                        documentIds = scopedDocumentIds,
                                        agentTypes = selectedAgentIds.toList(),
                                        scope = selectedScope
                                    ) { status ->
                                        progress = status.progress
                                        runStatus = "Run ${status.runId}: ${status.status}"
                                        runLogs = status.logs
                                    }
                                    progress = 100
                                    runStatus = "Run ${result.runId}: ${result.status}"
                                    runLogs = result.logs
                                } catch (error: Exception) {
                                    runStatus = error.message ?: "Agent run failed"
                                } finally {
                                    isRunning = false
                                }
                            }
                        },
                        enabled = canRun,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Analysis", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    if (!canRun) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(disabledReason(readyDocuments, selectedScope, selectedDocumentIds), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (runStatus != null && !isRunning) {
                item {
                    Text(runStatus ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun IconButtonCompat(enabled: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.IconButton(enabled = enabled, onClick = onClick) {
        Icon(Icons.Default.Refresh, contentDescription = "Refresh catalog")
    }
}

@Composable
private fun AnalysisSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.FilterAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DocumentScopeRow(document: DocumentEntity, selected: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(checked = selected, onCheckedChange = onCheckedChange, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary))
        Column(modifier = Modifier.weight(1f)) {
            Text(document.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Text("${document.category} • ${document.qualityScore}% OCR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SuperAgentCard(team: SuperAgentOption, selected: Boolean, onUseTeam: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(team.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(team.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        OutlinedButton(onClick = onUseTeam, shape = RoundedCornerShape(6.dp)) {
            Text(if (selected) "Selected" else "Use")
        }
    }
}

@Composable
fun AgentCard(title: String, description: String, icon: ImageVector, active: Boolean, onActiveChange: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                1.dp,
                if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(8.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = active,
                        onCheckedChange = onActiveChange,
                        modifier = Modifier.height(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ExecutionStatusPanel(progress: Int, status: String?, logs: List<String>) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha), RoundedCornerShape(4.dp)))
                Text(status ?: "Analysis running", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.weight(1f))
                Text("${progress.coerceIn(0, 100)}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val visibleLogs = logs.takeLast(6).ifEmpty { listOf("Waiting for backend run status...") }
                visibleLogs.forEach { line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

private fun iconForAgent(agent: AgentOption): ImageVector {
    val text = "${agent.name} ${agent.division}".lowercase()
    return when {
        "timeline" in text -> Icons.Default.Timeline
        "precedent" in text || "statute" in text -> Icons.Default.Balance
        "criminal" in text || "civil" in text || "constitutional" in text -> Icons.Default.Gavel
        "motion" in text || "complaint" in text -> Icons.Default.Description
        else -> Icons.Default.AutoAwesome
    }
}

private fun disabledReason(
    readyDocuments: List<DocumentEntity>,
    selectedScope: String,
    selectedDocumentIds: List<String>
): String {
    if (readyDocuments.isEmpty()) return "Upload or process evidence before running agents."
    if (selectedScope == "file" && selectedDocumentIds.isEmpty()) return "Choose at least one ready file."
    return "Analysis is unavailable right now."
}
