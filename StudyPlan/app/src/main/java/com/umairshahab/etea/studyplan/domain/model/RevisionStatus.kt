package com.umairshahab.etea.studyplan.domain.model

/**
 * R4: Static SRS - status enum for revisions
 */
enum class RevisionStatus {
    SCHEDULED,  // Pending revision
    DONE,       // Completed by user
    MISSED      // Overdue, stays missed until marked done (R8)
}
