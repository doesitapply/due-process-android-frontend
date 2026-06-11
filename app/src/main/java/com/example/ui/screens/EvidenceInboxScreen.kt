package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

data class DocumentItem(
    val id: String,
    val title: String,
    val caseName: String,
    val category: String,
    val status: String,
    val quality: Int,
    val aiSummary: String,
    val dateAdded: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidenceInboxScreen(navController: NavController) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Filings", "Evidence", "Transcripts", "Media")
    
    val allDocuments = listOf(
        DocumentItem(
            id = "DOC-042",
            title = "Incident_Report_042.pdf",
            caseName = "State v. Hammond",
            category = "Evidence",
            status = "Ready",
            quality = 88,
            aiSummary = "The incident report details the events of Oct 12th at the warehouse. Officer Davis notes a forced entry at the rear loading dock and recovery of a discarded crowbar. No suspects apprehended on scene.",
            dateAdded = "2 hours ago"
        ),
        DocumentItem(
            id = "DOC-043",
            title = "Motion_To_Dismiss_Draft.docx",
            caseName = "Global Tech Litigation",
            category = "Filings",
            status = "Ready",
            quality = 95,
            aiSummary = "Defense motion to dismiss on grounds of improper venue and lack of personal jurisdiction over the corporate entity. Cites establishing precedent under TechCorp v. State.",
            dateAdded = "1 day ago"
        ),
        DocumentItem(
            id = "DOC-044",
            title = "Deposition_Jamison_Pt1.mp3",
            caseName = "State v. Jamison",
            category = "Transcripts",
            status = "Processing",
            quality = 0,
            aiSummary = "AI transcription and summarization in progress...",
            dateAdded = "5 mins ago"
        ),
        DocumentItem(
            id = "DOC-045",
            title = "Handwritten_Notes_Nov12.jpg",
            caseName = "Estate of Vance",
            category = "Evidence",
            status = "Need Review",
            quality = 42,
            aiSummary = "Illegible scrawlings referencing a 'safety deposit box' and numbers '4412'. High uncertainty in OCR extraction. Human review required to verify account details.",
            dateAdded = "3 days ago"
        )
    )

    val filteredDocs = if (selectedCategory == "All") allDocuments else allDocuments.filter { it.category == selectedCategory }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Document Management", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary) },
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

            // Documents List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredDocs, key = { it.id }) { doc ->
                    DocumentCard(doc)
                }
            }
        }
    }
}

@Composable
fun DocumentCard(doc: DocumentItem) {
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
                    val statusColor = when(doc.status) {
                        "Ready" -> MaterialTheme.colorScheme.primary
                        "Processing" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                    Text(doc.status, style = MaterialTheme.typography.labelSmall, color = statusColor)
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
                        doc.aiSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Added ${doc.dateAdded}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(
                            onClick = { /* Open Document */ },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text("View Full Document")
                        }
                    }
                }
            }
        }
    }
}
