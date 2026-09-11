package com.umairshahab.etea.studyplan.ui.screens.topics

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
import com.umairshahab.etea.studyplan.domain.model.Subject

/**
 * TopicsScreen - Lists all topics with filters (Feature 14)
 * Filters: All | Upcoming | Missed | Done (single-select chips)
 */
@Composable
fun TopicsScreen(
    topics: List<Topic> = emptyList(),
    revisions: List<Revision> = emptyList(),
    selectedSubject: Subject? = null,
    onTopicClick: (Topic) -> Unit = {},
    onEditTopic: (Topic) -> Unit = {},
    onDeleteTopic: (Topic) -> Unit = {}
) {
    var selectedFilter by rememberSaveable { mutableStateOf(TopicFilter.ALL) }

    val filteredTopics = remember(topics, revisions, selectedFilter, selectedSubject) {
        topics.filter { topic ->
            // Filter by subject if selected
            if (selectedSubject != null && topic.subject != selectedSubject) return@filter false

            // Get revisions for this topic
            val topicRevisions = revisions.filter { it.topicId == topic.id }

            // Apply filter
            when (selectedFilter) {
                TopicFilter.ALL -> true
                TopicFilter.UPCOMING -> topicRevisions.any { it.status == RevisionStatus.SCHEDULED }
                TopicFilter.MISSED -> topicRevisions.any { it.status == RevisionStatus.MISSED }
                TopicFilter.DONE -> {
                    topicRevisions.any { it.status == RevisionStatus.DONE } &&
                    topicRevisions.none { it.status == RevisionStatus.SCHEDULED || it.status == RevisionStatus.MISSED }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Topics",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Filter chips (Feature 14)
        FilterChipRow(selectedFilter) { selectedFilter = it }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredTopics.isEmpty()) {
            // Empty state with exact caption
            Text(
                text = "No topics match this filter.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTopics) { topic ->
                    TopicCard(topic, revisions, onTopicClick, onEditTopic, onDeleteTopic)
                }
            }
        }
    }
}

enum class TopicFilter { ALL, UPCOMING, MISSED, DONE }

@Composable
private fun FilterChipRow(selectedFilter: TopicFilter, onFilterChange: (TopicFilter) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TopicFilter.values().forEach { filter ->
            FilterChip(
                onClick = { onFilterChange(filter) },
                label = { Text(filter.name) },
                selected = selectedFilter == filter,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
private fun TopicCard(
    topic: Topic,
    revisions: List<Revision>,
    onTopicClick: (Topic) -> Unit,
    onEditTopic: (Topic) -> Unit,
    onDeleteTopic: (Topic) -> Unit
) {
    val topicRevisions = revisions.filter { it.topicId == topic.id }
    val nextDue = topicRevisions
        .filter { it.status == RevisionStatus.SCHEDULED }
        .minByOrNull { it.dueAt }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onTopicClick(topic) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = topic.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    // Feature 3: Subtitle format
                    val subtitle = if (!topic.chapter.isNullOrBlank()) {
                        "Chapter ${topic.chapter} • ${topic.subject.displayName}"
                    } else {
                        topic.subject.displayName
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = { onEditTopic(topic) }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                            contentDescription = "Edit"
                        )
                    }
                    IconButton(onClick = { onDeleteTopic(topic) }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Next due info (Feature 8: Identity badges)
            nextDue?.let { revision ->
                Spacer(modifier = Modifier.height(8.dp))
                AssistChip(
                    onClick = {},
                    label = { Text("Rev ${revision.intervalIndex + 1} · +${revision.intervalDays}d") },
                    leadingIcon = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = androidx.compose.ui.Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

// Extension property for display name
val Subject.displayName: String
    get() = when (this) {
        Subject.MATHS -> "Maths"
        Subject.PHYSICS -> "Physics"
        Subject.CHEMISTRY -> "Chemistry"
        Subject.ENGLISH -> "English"
    }
