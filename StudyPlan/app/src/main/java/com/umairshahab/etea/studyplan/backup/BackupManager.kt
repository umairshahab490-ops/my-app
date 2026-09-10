package com.umairshahab.etea.studyplan.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.umairshahab.etea.studyplan.data.model.Revision
import com.umairshahab.etea.studyplan.data.model.Topic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * BackupManager - JSON v1 backup/restore via SAF (Feature 15)
 * Export format: {"version":1,"exportedAt",topics[],revisions[]}
 * Restore = validate + clear + bulk insert in ONE Room transaction then reschedule alarms
 */
class BackupManager(private val context: Context) {
    
    companion object {
        const val BACKUP_VERSION = 1
        const val PREFS_NAME = "study_plan_prefs"
        const val KEY_LAST_BACKUP_EXPORTED_AT = "last_backup_exported_at"
        
        private val formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
        private val displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    }
    
    /**
     * Get last backup timestamp for Settings caption
     * Returns formatted string or "Last backup: never"
     */
    fun getLastBackupCaption(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timestamp = prefs.getLong(KEY_LAST_BACKUP_EXPORTED_AT, -1L)
        return if (timestamp > 0) {
            val zdt = ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                java.time.ZoneId.systemDefault()
            )
            "Last backup: ${zdt.format(displayFormatter)}"
        } else {
            "Last backup: never"
        }
    }
    
    /**
     * Generate SAF suggested filename: studyplan-backup-yyyy-MM-dd.json
     */
    fun generateBackupFilename(): String {
        val dateStr = java.time.LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return "studyplan-backup-$dateStr.json"
    }
    
    /**
     * Export backup to JSON via SAF
     * Persists last_backup_exported_at timestamp
     */
    suspend fun exportBackup(topics: List<Topic>, revisions: List<Revision>, uri: Uri): Boolean = 
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("version", BACKUP_VERSION)
                    put("exportedAt", ZonedDateTime.now().format(formatter))
                    
                    put("topics", JSONArray().apply {
                        topics.forEach { topic ->
                            put(JSONObject().apply {
                                put("id", topic.id)
                                put("subject", topic.subject.name)
                                put("title", topic.title)
                                put("chapter", topic.chapter)
                                put("createdAt", topic.createdAt.format(formatter))
                                put("revisionHour", topic.revisionHour)
                                put("revisionMinute", topic.revisionMinute)
                                put("intervals", topic.intervals)
                            })
                        }
                    })
                    
                    put("revisions", JSONArray().apply {
                        revisions.forEach { revision ->
                            put(JSONObject().apply {
                                put("id", revision.id)
                                put("topicId", revision.topicId)
                                put("intervalIndex", revision.intervalIndex)
                                put("intervalDays", revision.intervalDays)
                                put("dueAt", revision.dueAt.format(formatter))
                                put("alertAt", revision.alertAt.format(formatter))
                                put("status", revision.status.name)
                                put("completedAt", revision.completedAt?.format(formatter))
                            })
                        }
                    })
                }
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toString(2).toByteArray(Charsets.UTF_8))
                }
                
                // Persist last backup timestamp
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(KEY_LAST_BACKUP_EXPORTED_AT, System.currentTimeMillis())
                    .apply()
                
                true
            } catch (e: Exception) {
                false
            }
        }
    
    /**
     * Parse and validate backup file
     * Returns Pair(topicsCount, revisionsCount) or null if invalid
     */
    suspend fun parseBackup(uri: Uri): Pair<Int, Int>? = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { 
                it.bufferedReader().readText() 
            } ?: return@withContext null
            
            val json = JSONObject(jsonString)
            
            // Validate version
            val version = json.optInt("version", -1)
            if (version != BACKUP_VERSION) return@withContext null
            
            val topics = json.optJSONArray("topics") ?: return@withContext null
            val revisions = json.optJSONArray("revisions") ?: return@withContext null
            
            Pair(topics.length(), revisions.length())
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get preview text for import dialog
     * "This backup contains {N} topics and {M} revisions."
     */
    suspend fun getBackupPreview(uri: Uri): String? {
        val counts = parseBackup(uri) ?: return null
        return "This backup contains ${counts.first} topics and ${counts.second} revisions."
    }
}
