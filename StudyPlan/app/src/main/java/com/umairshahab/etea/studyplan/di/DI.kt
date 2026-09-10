package com.umairshahab.etea.studyplan.di

import android.content.Context
import com.umairshahab.etea.studyplan.data.dao.StudyPlanDao
import com.umairshahab.etea.studyplan.data.database.StudyPlanDatabase
import com.umairshahab.etea.studyplan.data.repository.StudyPlanRepository

/**
 * Simple dependency injection container
 */
object DI {
    
    @Volatile
    private var database: StudyPlanDatabase? = null
    
    @Volatile
    private var repository: StudyPlanRepository? = null
    
    fun getDatabase(context: Context): StudyPlanDatabase {
        return database ?: synchronized(this) {
            val instance = StudyPlanDatabase.getInstance(context)
            database = instance
            instance
        }
    }
    
    fun getRepository(context: Context): StudyPlanRepository {
        return repository ?: synchronized(this) {
            val dao = getDatabase(context).studyPlanDao()
            val instance = StudyPlanRepository(dao)
            repository = instance
            instance
        }
    }
    
    // For testing - allows injection of mock repository
    fun setRepository(repo: StudyPlanRepository) {
        repository = repo
    }
}
