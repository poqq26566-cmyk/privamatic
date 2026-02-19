package com.techtrest.privacywidget.data.scanner.checks

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.techtrest.privacywidget.data.model.PrivacyCheck
import com.techtrest.privacywidget.data.model.PrivacyIssue

class GoogleServicesChecker(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Check Google Play Services installation state.
     * Stage 1: Not installed → isSecure = true
     * Stage 2: Sandboxed (no FLAG_SYSTEM) → isSecure = true (acceptable privacy trade-off)
     * Stage 3: Full system privileges → isSecure = false
     */
    fun checkGooglePlayServices(): PrivacyIssue {
        return try {
            val appInfo = try {
                packageManager.getApplicationInfo(GMS_PACKAGE, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }

            val hasSystemFlag = appInfo != null &&
                (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            val isInDataPartition = appInfo?.sourceDir?.startsWith("/data/") == true
            val isFullSystem = hasSystemFlag && !isInDataPartition

            when {
                appInfo == null -> PrivacyIssue(
                    check = PrivacyCheck.GOOGLE_PLAY_SERVICES,
                    isSecure = true,
                    currentStatus = "Not installed",
                    technicalDetails = "$GMS_PACKAGE is not installed"
                )
                !isFullSystem -> PrivacyIssue(
                    check = PrivacyCheck.GOOGLE_PLAY_SERVICES,
                    isSecure = true,
                    currentStatus = "Installed (sandboxed)",
                    technicalDetails = "Running without system privileges - reduced privacy risk"
                )
                else -> PrivacyIssue(
                    check = PrivacyCheck.GOOGLE_PLAY_SERVICES,
                    isSecure = false,
                    isSystemApp = true,
                    currentStatus = "Installed with full system privileges",
                    technicalDetails = "Google Play Services has deep system access and telemetry"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Google Play Services", e)
            PrivacyIssue(
                check = PrivacyCheck.GOOGLE_PLAY_SERVICES,
                isSecure = true,
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    /**
     * Check if Google's Find My Device is active by looking for its package.
     * com.google.android.apps.adm installed and enabled = Find My Device is active.
     */
    fun checkFindMyDevice(): PrivacyIssue {
        return try {
            val appInfo = try {
                context.packageManager.getApplicationInfo(FIND_MY_DEVICE_PACKAGE, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }

            val isActive = appInfo != null && appInfo.enabled

            PrivacyIssue(
                check = PrivacyCheck.FIND_MY_DEVICE,
                isSecure = !isActive,
                currentStatus = if (isActive) "Enabled" else "Not installed / Not applicable",
                technicalDetails = if (isActive)
                    "$FIND_MY_DEVICE_PACKAGE is installed and enabled"
                else
                    "$FIND_MY_DEVICE_PACKAGE is not installed or is disabled"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Find My Device", e)
            PrivacyIssue(
                check = PrivacyCheck.FIND_MY_DEVICE,
                isSecure = true,
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    companion object {
        private const val TAG = "GoogleServicesChecker"
        private const val GMS_PACKAGE = "com.google.android.gms"
        private const val FIND_MY_DEVICE_PACKAGE = "com.google.android.apps.adm"
    }
}
