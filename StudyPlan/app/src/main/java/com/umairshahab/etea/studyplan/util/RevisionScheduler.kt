package com.umairshahab.etea.studyplan.util

import android.content.Context
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * RevisionScheduler - builds revision schedules using ZonedDateTime arithmetic
 * R4: STATIC schedule - completing never shifts future dates
 * R5: Base timestamp = creation day at chosen time; if passed today, tomorrow
 * R6: Edit keeps DONE/MISSED history, deletes+rebuilds only SCHEDULED from ORIGINAL createdAt
 */
class RevisionScheduler {
    
    companion object {
        /**
         * Parse intervals string into list of positive integers
         * e.g., "1,3,7,14" -> [1, 3, 7, 14]
         */
        fun parseIntervals(intervalsStr: String): List<Int> {
            return intervalsStr.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it > 0 }
        }
        
        /**
         * Calculate base timestamp per R5
         * If revision time has passed today, use tomorrow
         */
        fun calculateBaseTimestamp(createdAt: ZonedDateTime, hour: Int, minute: Int): ZonedDateTime {
            val now = ZonedDateTime.now()
            var baseTime = createdAt.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            
            // If base time is in the past, move to tomorrow
            if (baseTime.isBefore(now) || baseTime.isEqual(now)) {
                baseTime = baseTime.plusDays(1)
            }
            
            return baseTime
        }
        
        /**
         * Build all revisions for a topic
         * Returns list of (intervalIndex, intervalDays, dueAt, alertAt) tuples
         */
        fun buildRevisions(
            topicId: Long,
            createdAt: ZonedDateTime,
            revisionHour: Int,
            revisionMinute: Int,
            intervalsStr: String
        ): List<RevisionData> {
            val intervals = parseIntervals(intervalsStr)
            val baseTime = calculateBaseTimestamp(createdAt, revisionHour, revisionMinute)
            
            return intervals.mapIndexed { index, days ->
                val dueAt = baseTime.plus(days.toLong(), ChronoUnit.DAYS)
                val alertAt = dueAt.minusMinutes(2)  // 120000ms = 2 minutes
                
                RevisionData(
                    topicId = topicId,
                    intervalIndex = index,
                    intervalDays = days,
                    dueAt = dueAt,
                    alertAt = alertAt
                )
            }
        }
        
        /**
         * Rebuild scheduled revisions after edit (R6)
         * Keeps original createdAt, recalculates from there
         * Only returns SCHEDULED revisions (DONE/MISSED history kept separately)
         */
        fun rebuildScheduledRevisions(
            topicId: Long,
            originalCreatedAt: ZonedDateTime,
            revisionHour: Int,
            revisionMinute: Int,
            intervalsStr: String,
            completedIntervalIndices: Set<Int>  // Indices that were already DONE
        ): List<RevisionData> {
            val intervals = parseIntervals(intervalsStr)
            val baseTime = calculateBaseTimestamp(originalCreatedAt, revisionHour, revisionMinute)
            
            return intervals.mapIndexedNotNull { index, days ->
                // Skip intervals already completed
                if (index in completedIntervalIndices) return@mapIndexedNotNull null
                
                val dueAt = baseTime.plus(days.toLong(), ChronoUnit.DAYS)
                
                // Skip if due date is in the past (already overdue)
                if (dueAt.isBefore(ZonedDateTime.now())) return@mapIndexedNotNull null
                
                val alertAt = dueAt.minusMinutes(2)
                
                RevisionData(
                    topicId = topicId,
                    intervalIndex = index,
                    intervalDays = days,
                    dueAt = dueAt,
                    alertAt = alertAt
                )
            }
        }
    }
    
    /**
     * Data class for revision scheduling
     */
    data class RevisionData(
        val topicId: Long,
        val intervalIndex: Int,
        val intervalDays: Int,
        val dueAt: ZonedDateTime,
        val alertAt: ZonedDateTime
    )
}
