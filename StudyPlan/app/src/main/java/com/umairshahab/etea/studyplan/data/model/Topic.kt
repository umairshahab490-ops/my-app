package com.umairshahab.etea.studyplan.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.umairshahab.etea.studyplan.domain.model.Subject
import java.time.ZonedDateTime

/**
 * Topic entity - user-created topics with custom intervals
 * R1: Fresh install = 0 topics (no seed data)
 * R3: User creates all topics; intervals fully editable
 */
@Entity(tableName = "topics")
data class Topic(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val subject: Subject,
    
    val title: String,
    
    val chapter: String? = null,  // Nullable - optional chapter
    
    val createdAt: ZonedDateTime,  // R5: Base timestamp for schedule calculation
    
    val revisionHour: Int,  // Hour of day for revision (0-23)
    val revisionMinute: Int,  // Minute of hour (0-59)
    
    val intervals: String  // Comma-separated positive integers (e.g., "1,3,7,14")
)
