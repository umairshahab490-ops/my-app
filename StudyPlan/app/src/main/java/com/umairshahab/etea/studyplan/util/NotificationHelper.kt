package com.umairshahab.etea.studyplan.util

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.umairshahab.etea.studyplan.R
import com.umairshahab.etea.studyplan.data.model.Revision
import com.umairshahab.etea.studyplan.domain.model.RevisionStatus

/**
 * Notification helper for revision reminders
 * R11: Alerts best-effort; app fully usable with notifications denied
 */
class NotificationHelper(private val context: Context) {
    
    companion object {
        const val CHANNEL_REVISION_REMINDERS = "revision_reminders"
        const val CHANNEL_MISSED_REVISIONS = "missed_revisions"
        const val NOTIFICATION_GROUP_KEY = "study_plan_revisions"
        const val GROUP_NOTIFICATION_ID = 1000
    }
    
    private val notificationManager = 
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    /**
     * Show notification for a revision reminder
     */
    fun showRevisionReminder(revision: Revision, topicTitle: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_REVISION_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Revision Due: $topicTitle")
            .setContentText("Time to revise!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(revision.id.toInt(), notification)
    }
    
    /**
     * Show notification for missed revision (R8: stays missed until Done)
     */
    fun showMissedRevision(revision: Revision, topicTitle: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_MISSED_REVISIONS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Missed: $topicTitle")
            .setContentText("This revision was due. Mark Done when you catch up.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(revision.id.toInt() + 10000, notification)
    }
    
    /**
     * Cancel notification for a revision
     */
    fun cancelNotification(revisionId: Long) {
        notificationManager.cancel(revisionId.toInt())
        notificationManager.cancel(revisionId.toInt() + 10000)
    }
}
