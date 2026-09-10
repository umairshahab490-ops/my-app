package com.umairshahab.etea.studyplan.data.database

import androidx.room.TypeConverter
import com.umairshahab.etea.studyplan.domain.model.RevisionStatus
import com.umairshahab.etea.studyplan.domain.model.Subject
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Type converters for Room database
 * Converts ZonedDateTime, Subject enum, and RevisionStatus enum to/from stored types
 */
class Converters {
    
    private val formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
    
    @TypeConverter
    fun fromZonedDateTime(zonedDateTime: ZonedDateTime): String {
        return zonedDateTime.format(formatter)
    }
    
    @TypeConverter
    fun toZonedDateTime(string: String): ZonedDateTime {
        return ZonedDateTime.parse(string, formatter)
    }
    
    @TypeConverter
    fun fromSubject(subject: Subject): String {
        return subject.name
    }
    
    @TypeConverter
    fun toSubject(string: String): Subject {
        return Subject.valueOf(string)
    }
    
    @TypeConverter
    fun fromRevisionStatus(status: RevisionStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toRevisionStatus(string: String): RevisionStatus {
        return RevisionStatus.valueOf(string)
    }
}
