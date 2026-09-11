package com.umairshahab.etea.studyplan.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.umairshahab.etea.studyplan.data.model.Topic
import com.umairshahab.etea.studyplan.data.model.Revision
import com.umairshahab.etea.studyplan.domain.model.RevisionStatus
import java.time.LocalDate

/**
 * HomeScreen - Dashboard with topic counts, month strip, and quick stats
 */
@Composable
fun HomeScreen(
    topics: List<Topic> = emptyList(),
    revisions: List<Revision> = emptyList(),
    onNavigateToRevise: () -> Unit = {},
    onNavigateToTopics: () -> Unit = {},
    onNavigateToSubjects: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onAddTopic: () -> Unit = {}
) {
    val dueToday = revisions.count { 
        it.status == RevisionStatus.SCHEDULED && 
        LocalDate.parse(it.dueAt).isEqual(LocalDate.now()) 
    }
    val upcomingCount = revisions.count { 
        it.status == RevisionStatus.SCHEDULED && 
        LocalDate.parse(it.dueAt).isAfter(LocalDate.now()) 
    }
    val missedCount = revisions.count { it.status == RevisionStatus.MISSED }
    val doneCount = revisions.count { it.status == RevisionStatus.DONE }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Home",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (topics.isEmpty()) {
            // Onboarding card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
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
                        text = "📚 Welcome to Study Plan",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Add your first topic to start building your revision ladder.",
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
        } else {
            // Count cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CountCard("Due Today", dueToday.toString(), Modifier.weight(1f))
                CountCard("Upcoming", upcomingCount.toString(), Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CountCard("Missed", missedCount.toString(), Modifier.weight(1f))
                CountCard("Done", doneCount.toString(), Modifier.weight(1f))
            }

            // Total topics caption
            Text(
                text = "Total ${topics.size} topic(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
            )

            // Month strip placeholder
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Month Overview",
                        style = MaterialTheme.typography.titleSmall
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(31) { day ->
                            DayChip(day + 1, revisions, topics)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountCard(label: String, count: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DayChip(day: Int, revisions: List<Revision>, topics: List<Topic>) {
    val hasDue = revisions.any { 
        it.dueAt.startsWith("${LocalDate.now().year}-${LocalDate.now().monthValue.toString().padStart(2, '0')}-$day") 
    }
    Chip(
        onClick = {},
        colors = ChipDefaults.chipColors(
            containerColor = if (hasDue) 
                MaterialTheme.colorScheme.errorContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(text = day.toString(), style = MaterialTheme.typography.labelSmall)
    }
}
