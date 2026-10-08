package com.example.security

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager

object ExamSecurityHelper {

    private const val TAG = "ExamSecurity"

    /**
     * Applies WindowManager.LayoutParams.FLAG_SECURE to the Activity window.
     * Also blocks non-system overlay windows on Android 12+ (API 31+) using setHideOverlayWindows(true)
     * and filters obscured touches on the root view to reject tapjacking and floating apps.
     */
    fun enableWindowSecurity(activity: Activity?) {
        if (activity == null) return
        try {
            // 1. Prevent screenshots, screen recording, and task preview
            activity.window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )

            // 2. Hide overlay windows on Android 12+ (API 31+) so floating apps cannot be displayed over the exam
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    activity.window.setHideOverlayWindows(true)
                    Log.d(TAG, "setHideOverlayWindows(true) applied.")
                } catch (e: Exception) {
                    Log.w(TAG, "setHideOverlayWindows not supported on this device/ROM: ${e.message}")
                }
            }

            // 3. Filter obscured touches on the decor view so touches through overlays are rejected
            try {
                activity.window.decorView.filterTouchesWhenObscured = true
            } catch (e: Exception) {
                Log.w(TAG, "filterTouchesWhenObscured could not be set: ${e.message}")
            }

            Log.d(TAG, "FLAG_SECURE & Anti-Overlay successfully enabled on Activity window.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply window security: ${e.message}", e)
        }
    }

    /**
     * Clears WindowManager.LayoutParams.FLAG_SECURE and overlay restrictions from the Activity window.
     */
    fun disableWindowSecurity(activity: Activity?) {
        if (activity == null) return
        try {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    activity.window.setHideOverlayWindows(false)
                } catch (_: Exception) {}
            }
            try {
                activity.window.decorView.filterTouchesWhenObscured = false
            } catch (_: Exception) {}
            Log.d(TAG, "Window security cleared from Activity window.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear window security: ${e.message}", e)
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
