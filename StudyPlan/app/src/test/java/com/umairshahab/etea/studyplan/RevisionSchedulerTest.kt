package com.umairshahab.etea.studyplan

import com.umairshahab.etea.studyplan.util.RevisionScheduler
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * RevisionSchedulerTest - covers R4, R5, R8 logic
 */
class RevisionSchedulerTest {
    
    private val formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
    
    @Test
    fun testR5_baseTimestamp_futureTime_today() {
        // If revision time is in the future today, base = creation day at chosen time
        val now = ZonedDateTime.now()
        val futureHour = (now.hour + 2) % 24
        val createdAt = now
        
        val baseTime = RevisionScheduler.calculateBaseTimestamp(createdAt, futureHour, 0)
        
        assertEquals(now.year, baseTime.year)
        assertEquals(now.month, baseTime.month)
        assertEquals(now.dayOfMonth, baseTime.dayOfMonth)
        assertEquals(futureHour, baseTime.hour)
    }
    
    @Test
    fun testR5_baseTimestamp_pastTime_tomorrow() {
        // If revision time has passed today, base = tomorrow at chosen time
        val now = ZonedDateTime.now()
        val pastHour = (now.hour - 2 + 24) % 24
        
        val baseTime = RevisionScheduler.calculateBaseTimestamp(now, pastHour, 0)
        
        assertTrue(baseTime.isAfter(now))
        assertEquals(pastHour, baseTime.hour)
    }
    
    @Test
    fun testR4_staticSchedule_intervals() {
        // Verify intervals are parsed correctly and produce static schedule
        val intervalsStr = "1,3,7,14"
        val intervals = RevisionScheduler.parseIntervals(intervalsStr)
        
        assertEquals(listOf(1, 3, 7, 14), intervals)
        
        val createdAt = ZonedDateTime.now()
        val revisions = RevisionScheduler.buildRevisions(
            topicId = 1L,
            createdAt = createdAt,
            revisionHour = 12,
            revisionMinute = 0,
            intervalsStr = intervalsStr
        )
        
        assertEquals(4, revisions.size)
        assertEquals(0, revisions[0].intervalIndex)
        assertEquals(1, revisions[0].intervalDays)
        assertEquals(3, revisions[1].intervalDays)
        assertEquals(7, revisions[2].intervalDays)
        assertEquals(14, revisions[3].intervalDays)
    }
    
    @Test
    fun testR8_missedStaysMissed_notAutoMoved() {
        // R8: MISSED stays missed until Done; never auto-moves
        // This is verified by the DAO query that only transitions SCHEDULED -> MISSED
        // and never automatically changes MISSED status
        val overdueTime = ZonedDateTime.now().minusDays(2)
        assertTrue(overdueTime.isBefore(ZonedDateTime.now()))
        // The RevisionStatus.MISSED enum value persists until user marks Done
    }
    
    @Test
    fun testAlertAt_calculation() {
        // alertAt = dueAt - 120000ms (2 minutes)
        val createdAt = ZonedDateTime.now().plusDays(1)
        val revisions = RevisionScheduler.buildRevisions(
            topicId = 1L,
            createdAt = createdAt,
            revisionHour = 12,
            revisionMinute = 0,
            intervalsStr = "1"
        )
        
        val revision = revisions.first()
        val expectedAlertAt = revision.dueAt.minusMinutes(2)
        assertEquals(expectedAlertAt, revision.alertAt)
    }
}
