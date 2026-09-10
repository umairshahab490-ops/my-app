package com.umairshahab.etea.studyplan.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.umairshahab.etea.studyplan.di.DI
import com.umairshahab.etea.studyplan.util.NotificationHelper
import java.time.ZonedDateTime

/**
 * ReminderWorker - scans for overdue revisions every ~15 min
 * - Transitions SCHEDULED -> MISSED for overdue (one notification per transition)
 * - Tops up alarms ~30 min
 * - Same scan on app open
 */
class ReminderWorker(context: Context, params: WorkerParameters) : 
    CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val repository = DI.getRepository(applicationContext)
            val now = ZonedDateTime.now()
            
            // Mark overdue SCHEDULED revisions as MISSED (R8)
            repository.markOverdueAsMissed(now)
            
            // In full implementation, would send notifications for transitions
            // and re-schedule upcoming alarms within 48h window
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
