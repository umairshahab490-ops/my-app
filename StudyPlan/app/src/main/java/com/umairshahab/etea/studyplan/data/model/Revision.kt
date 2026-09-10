package com.umairshahab.etea.studyplan.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.umairshahab.etea.studyplan.domain.model.RevisionStatus
import java.time.ZonedDateTime

/**
 * Revision entity - individual revision sessions for topics
 * R4: Static schedule - completing never shifts future dates
 * R7: Delete cascades from topic (via foreign key)
 * R8: MISSED stays missed until Done
 */
@Entity(
    tableName = "revisions",
    foreignKeys = [
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE  // R7: Cascade delete
        )
    ],
    indices = [Index("topicId")]
)
data class Revision(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val topicId: Long,
    
    val intervalIndex: Int,  // Which interval in the sequence (0-based)
    
    val intervalDays: Int,  // Number of days for this interval
    
    val dueAt: ZonedDateTime,  // When revision is due
    
    val alertAt: ZonedDateTime,  // When to show notification (dueAt - 120000ms)
    
    val status: RevisionStatus,
    
    val completedAt: ZonedDateTime? = null  // Set when status = DONE
)
