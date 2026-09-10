package com.umairshahab.etea.studyplan.data.repository

import com.umairshahab.etea.studyplan.data.dao.StudyPlanDao
import com.umairshahab.etea.studyplan.data.model.Revision
import com.umairshahab.etea.studyplan.data.model.Topic
import com.umairshahab.etea.studyplan.domain.model.RevisionStatus
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime

/**
 * Repository for Topic and Revision data operations
 */
class StudyPlanRepository(private val dao: StudyPlanDao) {
    
    // ========== Topic Operations ==========
    
    val allTopics: Flow<List<Topic>> = dao.getAllTopics()
    
    fun getTopicById(topicId: Long): Flow<Topic?> = 
        kotlinx.coroutines.flow.flow { emit(dao.getTopicById(topicId)) }
    
    suspend fun insertTopic(topic: Topic): Long = dao.insertTopic(topic)
    
    suspend fun updateTopic(topic: Topic) = dao.updateTopic(topic)
    
    suspend fun deleteTopic(topic: Topic) = dao.deleteTopic(topic)
    
    suspend fun deleteAllTopics() = dao.deleteAllTopics()
    
    // ========== Revision Operations ==========
    
    fun getRevisionsForTopic(topicId: Long): Flow<List<Revision>> = 
        dao.getRevisionsForTopic(topicId)
    
    fun getDueRevisions(now: ZonedDateTime): Flow<List<Revision>> = 
        dao.getDueRevisions(now)
    
    fun getUpcomingRevisions(now: ZonedDateTime): Flow<List<Revision>> = 
        dao.getUpcomingRevisions(now)
    
    fun getMissedRevisions(): Flow<List<Revision>> = 
        dao.getMissedRevisions()
    
    fun getDoneRevisions(): Flow<List<Revision>> = 
        dao.getDoneRevisions()
    
    suspend fun getRevisionById(revisionId: Long): Revision? = 
        dao.getRevisionById(revisionId)
    
    suspend fun insertRevision(revision: Revision): Long = 
        dao.insertRevision(revision)
    
    suspend fun insertRevisions(revisions: List<Revision>) = 
        dao.insertRevisions(revisions)
    
    suspend fun updateRevision(revision: Revision) = 
        dao.updateRevision(revision)
    
    suspend fun deleteRevisionsForTopic(topicId: Long) = 
        dao.deleteRevisionsForTopic(topicId)
    
    suspend fun deleteAllRevisions() = 
        dao.deleteAllRevisions()
    
    suspend fun markOverdueAsMissed(now: ZonedDateTime) = 
        dao.markOverdueAsMissed(now)
    
    suspend fun markRevisionAsDone(revisionId: Long, completedAt: ZonedDateTime) = 
        dao.markRevisionAsDone(revisionId, completedAt)
    
    fun getNextScheduledRevision(): Flow<Revision?> = 
        dao.getNextScheduledRevision()
    
    fun getTopicCount(): Flow<Int> = dao.getTopicCount()
    
    fun getScheduledRevisionCount(): Flow<Int> = dao.getScheduledRevisionCount()
}
