package com.umairshahab.etea.studyplan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.umairshahab.etea.studyplan.ui.screens.home.HomeScreen
import com.umairshahab.etea.studyplan.ui.theme.StudyPlanTheme

/**
 * MainActivity - entry point for Study Plan app
 * Offline-first, blank-slate spaced-repetition tracker for ETEA students
 */
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            StudyPlanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(
                        onNavigateToRevise = { /* Navigate to Revise tab */ },
                        onNavigateToTopics = { /* Navigate to Topics tab */ },
                        onNavigateToSubjects = { /* Navigate to Subjects tab */ },
                        onNavigateToCalendar = { /* Navigate to Calendar tab */ },
                        onNavigateToSettings = { /* Navigate to Settings */ }
                    )
                }
            }
        }
    }
}
