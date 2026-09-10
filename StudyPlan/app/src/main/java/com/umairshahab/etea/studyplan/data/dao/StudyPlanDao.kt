package com.umairshahab.etea.studyplan.data.dao

import androidx.room.*
import com.umairshahab.etea.studyplan.data.model.Revision
import com.umairshahab.etea.studyplan.data.model.Topic
import com.umairshahab.etea.studyplan.domain.model.RevisionStatus
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime

/**
 * Data Access Object for Topic and Revision entities
 */
@Dao
interface StudyPlanDao {
    
    // ========== Topic Operations ==========
    
    @Query("SELECT * FROM topics ORDER BY createdAt DESC")
    fun getAllTopics(): Flow<List<Topic>>
    
    @Query("SELECT * FROM topics WHERE id = :topicId")
    suspend fun getTopicById(topicId: Long): Topic?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: Topic): Long
    
    @Update
    suspend fun updateTopic(topic: Topic)
    
    @Delete
    suspend fun deleteTopic(topic: Topic)
    
    @Query("DELETE FROM topics")
    suspend fun deleteAllTopics()
    
    // ========== Revision Operations ==========
    
    @Query("SELECT * FROM revisions WHERE topicId = :topicId ORDER BY intervalIndex ASC")
    fun getRevisionsForTopic(topicId: Long): Flow<List<Revision>>
    
    @Query("SELECT * FROM revisions WHERE status = 'SCHEDULED' AND dueAt <= :now ORDER BY dueAt ASC")
    fun getDueRevisions(now: ZonedDateTime): Flow<List<Revision>>
    
    @Query("SELECT * FROM revisions WHERE status = 'SCHEDULED' AND dueAt > :now ORDER BY dueAt ASC")
    fun getUpcomingRevisions(now: ZonedDateTime): Flow<List<Revision>>
    
    @Query("SELECT * FROM revisions WHERE status = 'MISSED' ORDER BY dueAt ASC")
    fun getMissedRevisions(): Flow<List<Revision>>
    
    @Query("SELECT * FROM revisions WHERE status = 'DONE' ORDER BY completedAt DESC")
    fun getDoneRevisions(): Flow<List<Revision>>
    
    @Query("SELECT * FROM revisions WHERE id = :revisionId")
    suspend fun getRevisionById(revisionId: Long): Revision?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevision(revision: Revision): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevisions(revisions: List<Revision>)
    
    @Update
    suspend fun updateRevision(revision: Revision)
    
    @Query("DELETE FROM revisions WHERE topicId = :topicId")
    suspend fun deleteRevisionsForTopic(topicId: Long)
    
    @Query("DELETE FROM revisions")
    suspend fun deleteAllRevisions()
    
    @Query("UPDATE revisions SET status = 'MISSED' WHERE status = 'SCHEDULED' AND dueAt < :now")
    suspend fun markOverdueAsMissed(now: ZonedDateTime)
    
    @Query("UPDATE revisions SET status = 'DONE', completedAt = :completedAt WHERE id = :revisionId")
    suspend fun markRevisionAsDone(revisionId: Long, completedAt: ZonedDateTime)
    
    @Query("SELECT * FROM revisions WHERE status = 'SCHEDULED' ORDER BY dueAt ASC LIMIT 1")
    fun getNextScheduledRevision(): Flow<Revision?>
    
    @Query("SELECT COUNT(*) FROM topics")
    fun getTopicCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM revisions WHERE status = 'SCHEDULED'")
    fun getScheduledRevisionCount(): Flow<Int>
}
