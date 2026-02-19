package com.techtrest.privacywidget.data.scanner.checks

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.techtrest.privacywidget.data.model.PrivacyCheck
import com.techtrest.privacywidget.data.model.PrivacyIssue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NetworkSecurityChecker(private val context: Context) {

    fun checkVpnConnection(): PrivacyIssue {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val hasVpn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            } else {
                false
            }

            PrivacyIssue(
                check = PrivacyCheck.VPN_CONNECTION,
                isSecure = hasVpn,
                currentStatus = if (hasVpn) "Active" else "Not active",
                technicalDetails = if (hasVpn)
                    "Checked using NetworkCapabilities.TRANSPORT_VPN"
                else
                    "Checked using NetworkCapabilities.TRANSPORT_VPN\nTip: consider enabling Always-On VPN in Settings → Network → VPN"
            )
        } catch (e: Exception) {
            PrivacyIssue(
                check = PrivacyCheck.VPN_CONNECTION,
                isSecure = false,
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    fun checkPrivateDns(): PrivacyIssue {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val privateDnsMode = Settings.Global.getString(
                    context.contentResolver,
                    "private_dns_mode"
                )

                when (privateDnsMode) {
                    "hostname" -> {
                        val hostname = Settings.Global.getString(
                            context.contentResolver,
                            "private_dns_specifier"
                        )
                        if (!hostname.isNullOrEmpty()) {
                            PrivacyIssue(
                                check = PrivacyCheck.PRIVATE_DNS,
                                isSecure = true,
                                currentStatus = "Custom hostname configured",
                                technicalDetails = "Hostname: $hostname"
                            )
                        } else {
                            PrivacyIssue(
                                check = PrivacyCheck.PRIVATE_DNS,
                                isSecure = false,
                                currentStatus = "Hostname mode set but no hostname configured",
                                technicalDetails = "Hostname mode is active but no hostname is provided"
                            )
                        }
                    }
                    "opportunistic" -> PrivacyIssue(
                        check = PrivacyCheck.PRIVATE_DNS,
                        isSecure = true,
                        currentStatus = "Automatic mode (opportunistic)",
                        technicalDetails = "Automatic mode"
                    )
                    "off" -> PrivacyIssue(
                        check = PrivacyCheck.PRIVATE_DNS,
                        isSecure = false,
                        currentStatus = "Not configured",
                        technicalDetails = "Disabled"
                    )
                    else -> PrivacyIssue(
                        check = PrivacyCheck.PRIVATE_DNS,
                        isSecure = false,
                        currentStatus = "Not configured",
                        technicalDetails = if (privateDnsMode == null) "Not configured" else "Unknown mode: $privateDnsMode"
                    )
                }
            } else {
                // Private DNS not available before Android 9
                PrivacyIssue(
                    check = PrivacyCheck.PRIVATE_DNS,
                    isSecure = true, // Don't penalize older devices
                    currentStatus = "Not available on Android < 9",
                    technicalDetails = "Private DNS requires Android 9 (API 28) or higher"
                )
            }
        } catch (e: Exception) {
            PrivacyIssue(
                check = PrivacyCheck.PRIVATE_DNS,
                isSecure = false,
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    /**
     * Check if advertising ID is active
     * This must be called from a background thread
     */
    suspend fun checkAdvertisingId(): PrivacyIssue = withContext(Dispatchers.IO) {
        try {
            // Try to get advertising ID info
            val adInfo = try {
                AdvertisingIdClient.getAdvertisingIdInfo(context)
            } catch (e: Exception) {
                // Play Services not available or other error
                Log.w(TAG, "Unable to get advertising ID info: ${e.message}")
                null
            }

            if (adInfo == null) {
                // Play Services not available - can't track via advertising ID
                return@withContext PrivacyIssue(
                    check = PrivacyCheck.ADVERTISING_ID,
                    isSecure = true,
                    currentStatus = "Not available (Play Services not installed)",
                    technicalDetails = "Advertising ID requires Google Play Services"
                )
            }

            val isLimitAdTrackingEnabled = adInfo.isLimitAdTrackingEnabled
            val adId = adInfo.id

            PrivacyIssue(
                check = PrivacyCheck.ADVERTISING_ID,
                isSecure = isLimitAdTrackingEnabled,
                currentStatus = if (isLimitAdTrackingEnabled) "Disabled/Limited" else "Active",
                technicalDetails = if (isLimitAdTrackingEnabled)
                    "Ad tracking is limited"
                else
                    "Active advertising ID: ${adId?.take(8)}..."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking advertising ID", e)
            PrivacyIssue(
                check = PrivacyCheck.ADVERTISING_ID,
                isSecure = true, // Don't penalize on error
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    companion object {
        private const val TAG = "NetworkSecurityChecker"
    }
}
