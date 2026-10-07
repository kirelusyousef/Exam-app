package com.example.security

import android.app.Activity
import android.util.Log
import android.view.WindowManager

object ExamSecurityHelper {

    private const val TAG = "ExamSecurity"

    /**
     * Applies WindowManager.LayoutParams.FLAG_SECURE to the Activity window.
     * This prevents Android OS and third-party apps from taking screenshots,
     * recording screen contents, or capturing previews in the recent apps task switcher.
     */
    fun enableWindowSecurity(activity: Activity?) {
        if (activity == null) return
        try {
            activity.window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
            Log.d(TAG, "FLAG_SECURE successfully enabled on Activity window.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply FLAG_SECURE: ${e.message}", e)
        }
    }

    /**
     * Clears WindowManager.LayoutParams.FLAG_SECURE from the Activity window.
     */
    fun disableWindowSecurity(activity: Activity?) {
        if (activity == null) return
        try {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            Log.d(TAG, "FLAG_SECURE cleared from Activity window.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear FLAG_SECURE: ${e.message}", e)
        }
    }

    /**
     * Checks if FLAG_SECURE is currently applied to the Activity window.
     */
    fun isWindowSecure(activity: Activity?): Boolean {
        if (activity == null) return false
        val flags = activity.window.attributes.flags
        return (flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
    }
}
