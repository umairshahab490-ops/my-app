package com.umairshahab.etea.studyplan.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import com.umairshahab.etea.studyplan.receiver.AlertReceiver
import java.time.ZonedDateTime

/**
 * AlertScheduler - schedules exact alarms for revision reminders
 * - Only within next 48h window
 * - Only when canScheduleExactAlarms() is true
 * - R11: Best-effort alerts
 */
class AlertScheduler(private val context: Context) {
    
    companion object {
        private const val ALERT_WINDOW_HOURS = 48L
        private const val PENDING_INTENT_REQUEST_BASE = 2000
    }
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    /**
     * Check if exact alarms can be scheduled (SDK guard for >= S)
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true  // Pre-S, always allowed
        }
    }
    
    /**
     * Schedule an alarm for a revision at its alertAt time
     * Only schedules if within 48h window and exact alarms allowed
     */
    fun scheduleAlert(revisionId: Long, alertAt: ZonedDateTime) {
        if (!canScheduleExactAlarms()) return
        
        val now = ZonedDateTime.now()
        val hoursUntilAlert = java.time.Duration.between(now, alertAt).toHours()
        
        // Only schedule if within 48h window and not in the past
        if (hoursUntilAlert < 0 || hoursUntilAlert > ALERT_WINDOW_HOURS) return
        
        val intent = Intent(context, AlertReceiver::class.java).apply {
            putExtra("revision_id", revisionId)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (PENDING_INTENT_REQUEST_BASE + revisionId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        AlarmManagerCompat.setExactAndAllowWhileIdle(
            alarmManager,
            AlarmManager.RTC_WAKEUP,
            alertAt.toInstant().toEpochMilli(),
            pendingIntent
        )
    }
    
    /**
     * Cancel a scheduled alarm
     */
    fun cancelAlert(revisionId: Long) {
        val intent = Intent(context, AlertReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (PENDING_INTENT_REQUEST_BASE + revisionId).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.cancel()
    }
}
