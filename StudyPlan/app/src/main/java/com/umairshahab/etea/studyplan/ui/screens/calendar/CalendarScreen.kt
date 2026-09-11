package com.umairshahab.etea.studyplan.ui.screens.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.umairshahab.etea.studyplan.data.model.Revision
import com.umairshahab.etea.studyplan.data.model.Topic
import com.umairshahab.etea.studyplan.domain.model.RevisionStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * CalendarScreen - Month view with day-sheet for revisions
 */
@Composable
fun CalendarScreen(
    topics: List<Topic> = emptyList(),
    revisions: List<Revision> = emptyList(),
    onDayClick: (LocalDate, List<Revision>) -> Unit = {}
) {
    val currentDate = remember { mutableStateOf(LocalDate.now()) }
    val monthRevisions = remember(revisions, currentDate.value) {
        revisions.filter { 
            it.status == RevisionStatus.SCHEDULED &&
            LocalDate.parse(it.dueAt).month == currentDate.value.month &&
            LocalDate.parse(it.dueAt).year == currentDate.value.year
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Month header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { 
                currentDate.value = currentDate.value.minusMonths(1) 
            }) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.ChevronLeft,
                    contentDescription = "Previous month"
                )
            }
            Text(
                text = currentDate.value.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = { 
                currentDate.value = currentDate.value.plusMonths(1) 
            }) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
                    contentDescription = "Next month"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Day headers
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar grid placeholder
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Calendar view - ${currentDate.value.month} ${currentDate.value.year}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${monthRevisions.size} revisions this month",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Upcoming revisions list
        Text(
            text = "Upcoming",
            style = MaterialTheme.typography.titleMedium
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(monthRevisions.take(10)) { revision ->
                val topic = topics.find { it.id == revision.topicId }
                if (topic != null) {
                    CalendarDayRow(revision, topic)
                }
            }
        }
    }
}

@Composable
private fun CalendarDayRow(revision: Revision, topic: Topic) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Rev ${revision.intervalIndex + 1} · +${revision.intervalDays}d",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AssistChip(
                onClick = {},
                label = { Text(LocalDate.parse(revision.dueAt).dayOfMonth.toString()) }
            )
        }
    }
}
