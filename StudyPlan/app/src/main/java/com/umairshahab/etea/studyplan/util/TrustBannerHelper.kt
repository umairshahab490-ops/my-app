package com.umairshahab.etea.studyplan.util

import android.content.Context
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import com.umairshahab.etea.studyplan.data.repository.StudyPlanRepository
import kotlinx.coroutines.flow.first

/**
 * TrustBannerHelper - determines if trust banner should be shown (Feature 11)
 * Shows iff >=1 SCHEDULED revision AND (notifications denied OR exact alarms not allowed OR battery optimization on)
 * SDK guards: canScheduleExactAlarms behind SDK_INT >= S (31), battery check behind SDK_INT >= M (23)
 */
class TrustBannerHelper(private val context: Context, private val repository: StudyPlanRepository) {
    
    companion object {
        const val PREFS_NAME = "study_plan_prefs"
        const val KEY_BANNER_DISMISSED = "alerts_banner_dismissed"
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Check if banner was dismissed by user (persists per-install)
     */
    fun isBannerDismissed(): Boolean {
        return prefs.getBoolean(KEY_BANNER_DISMISSED, false)
    }
    
    /**
     * Dismiss banner (persists to SharedPreferences)
     */
    fun dismissBanner() {
        prefs.edit().putBoolean(KEY_BANNER_DISMISSED, true).apply()
    }
    
    /**
     * Check if trust banner should be shown
     * Returns true iff:
     * - >=1 SCHEDULED revision exists
     * - AND (notifications denied OR exact alarms not allowed OR battery optimization on)
     */
    suspend fun shouldShowBanner(): Boolean {
        // Check if any scheduled revisions exist
        val scheduledCount = repository.getScheduledRevisionCount().first()
        if (scheduledCount < 1) return false  // NEVER show at 0 scheduled
        
        // Check notifications permission
        val notificationsDenied = !NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (notificationsDenied) return true
        
        // Check exact alarms (SDK guard for >= S)
        val alertScheduler = AlertScheduler(context)
        val exactAlarmsNotAllowed = !alertScheduler.canScheduleExactAlarms()
        if (exactAlarmsNotAllowed) return true
        
        // Check battery optimization (SDK guard for >= M)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = context.packageName
            val batteryOptimizationOn = !powerManager.isIgnoringBatteryOptimizations(packageName)
            if (batteryOptimizationOn) return true
        }
        
        return false
    }
}
