package com.umairshahab.etea.studyplan.ui.screens.subjects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.umairshahab.etea.studyplan.domain.model.Subject
import com.umairshahab.etea.studyplan.data.model.Topic

/**
 * SubjectsScreen - Shows all 4 fixed subjects with topic counts (R2)
 */
@Composable
fun SubjectsScreen(
    topics: List<Topic> = emptyList(),
    onSubjectClick: (Subject) -> Unit = {},
    onAddTopic: () -> Unit = {}
) {
    val subjects = listOf(Subject.MATHS, Subject.PHYSICS, Subject.CHEMISTRY, Subject.ENGLISH)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Subjects",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Chip row for quick filter (Feature 5)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(subjects) { subject ->
                AssistChip(
                    onClick = { onSubjectClick(subject) },
                    label = { Text(subject.displayName) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subject cards with topic counts
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(subjects) { subject ->
                val subjectTopics = topics.filter { it.subject == subject }
                SubjectCard(subject, subjectTopics.size, onSubjectClick)
            }
        }
    }
}

@Composable
private fun SubjectCard(subject: Subject, topicCount: Int, onClick: (Subject) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick(subject) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = subject.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$topicCount topic(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onClick(subject) }) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Add,
                    contentDescription = "Add topic"
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
