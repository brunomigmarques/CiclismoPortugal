package com.ciclismo.portugal.data.local

import android.content.Context
import android.util.Log
import com.ciclismo.portugal.domain.model.CyclistCategory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists wizard draft state to SharedPreferences.
 * Allows users to resume team creation even after closing the app.
 */
@Singleton
class WizardDraftStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "WizardDraftStorage"
        private const val PREFS_NAME = "wizard_draft_prefs"
        private const val KEY_SELECTIONS = "draft_selections"
        private const val KEY_TEAM_NAME = "draft_team_name"
        private const val KEY_CURRENT_STEP = "draft_current_step"
        private const val KEY_TIMESTAMP = "draft_timestamp"
        private const val DRAFT_EXPIRY_HOURS = 168 // 7 days
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val gson = Gson()

    /**
     * Data class representing a draft selection (cyclist IDs per category).
     */
    data class WizardDraft(
        val teamName: String,
        val selections: Map<String, List<String>>, // Category name -> List of cyclist IDs
        val currentStep: Int,
        val timestamp: Long
    )

    /**
     * Save the current wizard draft.
     */
    fun saveDraft(
        teamName: String,
        selections: Map<CyclistCategory, List<String>>, // Category -> Cyclist IDs
        currentStep: Int
    ) {
        try {
            val selectionsMap = selections.mapKeys { it.key.name }
            val draft = WizardDraft(
                teamName = teamName,
                selections = selectionsMap,
                currentStep = currentStep,
                timestamp = System.currentTimeMillis()
            )

            val json = gson.toJson(draft)
            prefs.edit()
                .putString(KEY_SELECTIONS, json)
                .apply()

            Log.d(TAG, "Saved draft: $teamName, step $currentStep, ${selections.values.flatten().size} cyclists")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving draft: ${e.message}", e)
        }
    }

    /**
     * Load the saved wizard draft, if any.
     * Returns null if no draft exists or if it's expired.
     */
    fun loadDraft(): WizardDraft? {
        return try {
            val json = prefs.getString(KEY_SELECTIONS, null) ?: return null

            val draft = gson.fromJson(json, WizardDraft::class.java)

            // Check if draft is expired
            val hoursSinceSave = (System.currentTimeMillis() - draft.timestamp) / (1000 * 60 * 60)
            if (hoursSinceSave > DRAFT_EXPIRY_HOURS) {
                Log.d(TAG, "Draft expired ($hoursSinceSave hours old)")
                clearDraft()
                return null
            }

            Log.d(TAG, "Loaded draft: ${draft.teamName}, step ${draft.currentStep}, ${draft.selections.values.flatten().size} cyclists")
            draft
        } catch (e: Exception) {
            Log.e(TAG, "Error loading draft: ${e.message}", e)
            clearDraft()
            null
        }
    }

    /**
     * Check if a draft exists.
     */
    fun hasDraft(): Boolean {
        return prefs.contains(KEY_SELECTIONS) && loadDraft() != null
    }

    /**
     * Clear the saved draft.
     */
    fun clearDraft() {
        prefs.edit()
            .remove(KEY_SELECTIONS)
            .apply()
        Log.d(TAG, "Draft cleared")
    }

    /**
     * Get the draft creation time as human-readable string.
     */
    fun getDraftAgeDescription(): String? {
        val draft = loadDraft() ?: return null
        val hoursSinceSave = (System.currentTimeMillis() - draft.timestamp) / (1000 * 60 * 60)
        return when {
            hoursSinceSave < 1 -> "há menos de 1 hora"
            hoursSinceSave < 24 -> "há ${hoursSinceSave.toInt()} horas"
            else -> "há ${(hoursSinceSave / 24).toInt()} dias"
        }
    }
}
