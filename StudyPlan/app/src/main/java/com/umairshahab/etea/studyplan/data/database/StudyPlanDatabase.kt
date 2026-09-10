package com.umairshahab.etea.studyplan.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.umairshahab.etea.studyplan.data.dao.StudyPlanDao
import com.umairshahab.etea.studyplan.data.model.Revision
import com.umairshahab.etea.studyplan.data.model.Topic

/**
 * Room Database for Study Plan app
 * R1: Fresh install = 0 topics (no seed data)
 * DB version 2 with fallbackToDestructiveMigration (known debt)
 */
@Database(
    entities = [Topic::class, Revision::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StudyPlanDatabase : RoomDatabase() {
    
    abstract fun studyPlanDao(): StudyPlanDao
    
    companion object {
        @Volatile
        private var INSTANCE: StudyPlanDatabase? = null
        
        const val DATABASE_NAME = "etea_blank_v1"
        
        fun getInstance(context: Context): StudyPlanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyPlanDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()  // KNOWN DEBT: migration lockdown pending
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
