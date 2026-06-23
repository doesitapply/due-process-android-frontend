package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.repository.CaseRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindingsDashboardScreen(navController: NavController, repository: CaseRepository) {
    val findings by repository.localFindings.collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        runCatching { repository.refreshFindingsFromServer() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text("MATTER REF: 2024-CV-118", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("Findings Dashboard", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Reviewing extracted facts and identified anomalies.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                // Provenance Knowledge Graph (Simulated)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha=0.5f), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text("PROVENANCE GRAPH", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        // Draw a quick simulated node network
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                Text("Ev", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal=4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha=0.3f), thickness = 2.dp)
                            Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(24.dp)).border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                                Text("! 14", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal=4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha=0.3f), thickness = 2.dp)
                            Box(modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                Text("Ts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("${findings.size} source-bound findings synced from DueProcess backend.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Filter Chips
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = true, onClick = {}, label = { Text("Critical") }, leadingIcon = { Icon(Icons.Default.Check, null) })
                    FilterChip(selected = false, onClick = {}, label = { Text("Contradictions") })
                    FilterChip(selected = false, onClick = {}, label = { Text("Missing Records") })
                }
            }

            if (findings.isEmpty()) {
                item {
                    Text("No synced findings yet. Run Legal Analysis after evidence is processed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(findings, key = { it.id }) { finding ->
                    val highRisk = finding.severity.equals("high", true) || finding.severity.equals("critical", true)
                    FindingCard(
                        tag = finding.tag.uppercase(),
                        tagColor = if (highRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        title = finding.title,
                        desc = finding.description,
                        sev = "${finding.severity.uppercase()} SEV",
                        sevColor = if (highRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer,
                        bgSev = if (highRisk) MaterialTheme.colorScheme.errorContainer.copy(0.2f) else MaterialTheme.colorScheme.secondaryContainer,
                        conf = "${finding.confidenceScore}%",
                        sources = finding.documentId,
                        action = finding.actionRequired.ifBlank { finding.status }
                    )
                }
            }
        }
    }
}

@Composable
fun FindingCard(
    tag: String, tagColor: androidx.compose.ui.graphics.Color,
    title: String, desc: String, 
    sev: String, sevColor: androidx.compose.ui.graphics.Color, bgSev: androidx.compose.ui.graphics.Color,
    conf: String, sources: String, action: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
    ) {
        Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(tagColor).align(Alignment.CenterStart))
        Column(modifier = Modifier.padding(12.dp).padding(start = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(tag, style = MaterialTheme.typography.labelSmall, color = tagColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Box(modifier = Modifier.background(bgSev, RoundedCornerShape(4.dp)).border(1.dp, sevColor.copy(0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(sev, style = MaterialTheme.typography.labelSmall, color = sevColor)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.border(width = 2.dp, color = MaterialTheme.colorScheme.surfaceVariant).padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
                 Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(conf, style = MaterialTheme.typography.labelSmall, color = if(conf != "N/A") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("|", color = MaterialTheme.colorScheme.outlineVariant)
                    Text(sources, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(action, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
