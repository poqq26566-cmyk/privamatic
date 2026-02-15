package com.techtrest.privacywidget.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.techtrest.privacywidget.data.model.ScoreHistory
import kotlin.math.abs

@Composable
fun DeviceInfoCard(
    scoreHistory: ScoreHistory?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scoreDelta = scoreHistory?.scoreDelta
    val showPrivacyChange = scoreDelta != null &&
            scoreDelta != 0 &&
            scoreHistory != null &&
            (System.currentTimeMillis() - scoreHistory.lastUpdateTimestamp) <= CHANGE_EXPIRY_MS

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Device Information",
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            DeviceInfoRow("Model", Build.MODEL)
            DeviceInfoRow("Operating System", detectOperatingSystem(context))
            DeviceInfoRow("Android Version", Build.VERSION.RELEASE)

            if (showPrivacyChange && scoreDelta != null) {
                val isIncrease = scoreDelta > 0
                val changeText = "${if (isIncrease) "↑" else "↓"}${abs(scoreDelta)} pts"
                val changeColor = if (isIncrease) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
                DeviceInfoRow(
                    label = "Privacy Change",
                    value = changeText,
                    valueColor = changeColor
                )
            }
        }
    }
}

private const val CHANGE_EXPIRY_MS = 172_800_000L  // 48 hours

/**
 * Detects the operating system/ROM running on the device
 */
private fun detectOperatingSystem(context: Context): String {
    // Debug logging to help diagnose detection issues
    Log.d("DeviceInfo", "OS Detection Debug Info:")
    Log.d("DeviceInfo", "  FINGERPRINT: ${Build.FINGERPRINT}")
    Log.d("DeviceInfo", "  DISPLAY: ${Build.DISPLAY}")
    Log.d("DeviceInfo", "  MANUFACTURER: ${Build.MANUFACTURER}")
    Log.d("DeviceInfo", "  BRAND: ${Build.BRAND}")
    Log.d("DeviceInfo", "  PRODUCT: ${Build.PRODUCT}")
    Log.d("DeviceInfo", "  ID: ${Build.ID}")
    Log.d("DeviceInfo", "  TAGS: ${Build.TAGS}")

    // Check GrapheneOS FIRST via system packages
    // GrapheneOS mimics stock Android in Build.FINGERPRINT for privacy/compatibility
    if (isGrapheneOS(context)) {
        Log.d("DeviceInfo", "  Detected: GrapheneOS (via system packages)")
        return "GrapheneOS"
    }

    // Convert all Build properties to lowercase for case-insensitive comparison
    val fingerprint = Build.FINGERPRINT.lowercase()
    val display = Build.DISPLAY.lowercase()
    val manufacturer = Build.MANUFACTURER.lowercase()
    val brand = Build.BRAND.lowercase()

    return when {
        // CalyxOS
        fingerprint.contains("calyx") || brand.contains("calyx") -> {
            Log.d("DeviceInfo", "  Detected: CalyxOS")
            "CalyxOS"
        }

        // LineageOS
        display.contains("lineage") || fingerprint.contains("lineage") -> {
            Log.d("DeviceInfo", "  Detected: LineageOS")
            "LineageOS"
        }

        // /e/OS
        display.contains("/e/") || display.contains("eos") || brand.contains("e_os") -> {
            Log.d("DeviceInfo", "  Detected: /e/OS")
            "/e/OS"
        }

        // Samsung One UI
        manufacturer == "samsung" -> {
            Log.d("DeviceInfo", "  Detected: Samsung One UI")
            "Samsung One UI"
        }

        // Xiaomi MIUI
        manufacturer == "xiaomi" -> {
            Log.d("DeviceInfo", "  Detected: Xiaomi MIUI")
            "Xiaomi MIUI"
        }

        // OnePlus OxygenOS
        manufacturer == "oneplus" -> {
            Log.d("DeviceInfo", "  Detected: OnePlus OxygenOS")
            "OnePlus OxygenOS"
        }

        // Oppo/Realme ColorOS
        manufacturer == "oppo" || manufacturer == "realme" -> {
            Log.d("DeviceInfo", "  Detected: ColorOS")
            "ColorOS"
        }

        // Stock Pixel/AOSP
        manufacturer == "google" -> {
            Log.d("DeviceInfo", "  Detected: Stock Android")
            "Stock Android"
        }

        // Fallback
        else -> {
            Log.d("DeviceInfo", "  Detected: Generic Android")
            "Android"
        }
    }
}

/**
 * GrapheneOS detection via system packages
 * GrapheneOS mimics stock Android in Build properties for privacy/compatibility,
 * so we need to detect it via GrapheneOS-specific system packages
 */
private fun isGrapheneOS(context: Context): Boolean {
    val grapheneOSPackages = listOf(
        "app.grapheneos.apps",
        "app.grapheneos.camera",
        "app.grapheneos.gmscompat",
        "app.grapheneos.info"
    )

    for (pkg in grapheneOSPackages) {
        try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(pkg, 0)
            Log.d("DeviceInfo", "  GrapheneOS package found: $pkg")
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            // Package not found, continue checking
        }
    }

    return false
}

@Composable
private fun DeviceInfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor
        )
    }
}
