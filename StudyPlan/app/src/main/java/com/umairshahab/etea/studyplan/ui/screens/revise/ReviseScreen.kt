package com.umairshahab.etea.studyplan.ui.screens.revise

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.umairshahab.etea.studyplan.data.model.Revision
import com.umairshahab.etea.studyplan.data.model.Topic
import com.umairshahab.etea.studyplan.domain.model.RevisionStatus
import java.time.LocalDate

/**
 * ReviseScreen - Shows due, missed, and upcoming revisions
 */
@Composable
fun ReviseScreen(
    topics: List<Topic> = emptyList(),
    revisions: List<Revision> = emptyList(),
    onMarkDone: (Revision) -> Unit = {},
    onAddTopic: () -> Unit = {}
) {
    val now = LocalDate.now()
    val dueToday = revisions.filter { 
        it.status == RevisionStatus.SCHEDULED && LocalDate.parse(it.dueAt).isEqual(now) 
    }
    val missed = revisions.filter { it.status == RevisionStatus.MISSED }
    val upcoming = revisions.filter { 
        it.status == RevisionStatus.SCHEDULED && LocalDate.parse(it.dueAt).isAfter(now) 
    }.sortedBy { it.dueAt }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Revise",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        if (topics.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Nothing due now.",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Add a topic to build your revision ladder.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onAddTopic,
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Add Topic")
                        }
                    }
                }
            }
        } else {
            // Missed section (always shown first if any exist)
            if (missed.isNotEmpty()) {
                item {
                    SectionHeader("Missed", missed.size)
                }
                items(missed) { revision ->
                    val topic = topics.find { it.id == revision.topicId }
                    if (topic != null) {
                        RevisionRow(revision, topic, onMarkDone, isMissed = true)
                    }
                }
            }

            // Due today section
            if (dueToday.isNotEmpty()) {
                item {
                    SectionHeader("Due Today", dueToday.size)
                }
                items(dueToday) { revision ->
                    val topic = topics.find { it.id == revision.topicId }
                    if (topic != null) {
                        RevisionRow(revision, topic, onMarkDone, isMissed = false)
                    }
                }
            }

            // Upcoming section
            if (upcoming.isNotEmpty()) {
                item {
                    SectionHeader("Upcoming", upcoming.size)
                }
                items(upcoming.take(5)) { revision ->
                    val topic = topics.find { it.id == revision.topicId }
                    if (topic != null) {
                        RevisionRow(revision, topic, onMarkDone, isMissed = false, showNextDate = true)
                    }
                }
            }

            // Next due info
            if (dueToday.isEmpty() && missed.isEmpty() && upcoming.isNotEmpty()) {
                item {
                    val nextRevision = upcoming.first()
                    val nextTopic = topics.find { it.id == nextRevision.topicId }
                    Text(
                        text = "Next: ${nextRevision.dueAt} – ${nextTopic?.title ?: \"Unknown\"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        AssistChip(
            onClick = {},
            label = { Text("$count") },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = when (title) {
                    "Missed" -> MaterialTheme.colorScheme.errorContainer
                    "Due Today" -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        )
    }
}

@Composable
private fun RevisionRow(
    revision: Revision,
    topic: Topic,
    onMarkDone: (Revision) -> Unit,
    isMissed: Boolean,
    showNextDate: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isMissed) 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Rev ${revision.intervalIndex + 1} · +${revision.intervalDays}d",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (showNextDate) {
                    Text(
                        text = "Due: ${revision.dueAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isMissed) {
                Text(
                    text = "MISSED",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Button(onClick = { onMarkDone(revision) }) {
                Text("Done")
            }
        }
    }
}
