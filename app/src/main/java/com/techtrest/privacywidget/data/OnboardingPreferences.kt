package com.techtrest.privacywidget.data

import android.content.Context

private const val PREFS_NAME = "onboarding_prefs"
private const val KEY_COMPLETE = "onboarding_complete"

class OnboardingPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isComplete(): Boolean = prefs.getBoolean(KEY_COMPLETE, false)

    fun setComplete() {
        prefs.edit().putBoolean(KEY_COMPLETE, true).apply()
    }

    fun reset() {
        prefs.edit().putBoolean(KEY_COMPLETE, false).apply()
    }
}
