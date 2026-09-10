package com.umairshahab.etea.studyplan.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.umairshahab.etea.studyplan.di.DI
import com.umairshahab.etea.studyplan.util.NotificationHelper

/**
 * Broadcast receiver for alarm-triggered revision reminders
 */
class AlertReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val revisionId = intent.getLongExtra("revision_id", -1L)
        if (revisionId < 0) return
        
        // Show notification for the revision
        // In full implementation, would fetch revision details from DB
        val notificationHelper = NotificationHelper(context)
        // notificationHelper.showRevisionReminder(revision, topicTitle)
    }
}
