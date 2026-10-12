package ai.sach.libraryup.ui.screens.homescreen.composable

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs.SupportedCountries

/**
 * Share onboarding link via any available sharing method
 */
fun shareOnboardingLinkGeneric(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Please fill out your onboarding form: $url")
    }
    context.startActivity(Intent.createChooser(intent, "Send onboarding link via"))
}

/**
 * Share onboarding link via SMS if phone number is provided, otherwise use general sharing
 */
fun shareOnboardingLinkSms(context: Context, url: String, phoneNumber: String) {
    if (phoneNumber.isNotBlank()) {
        try {
            // Try to send SMS directly
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", "Please fill out your onboarding form: $url")
            }
            
            // Check if there's an app that can handle this intent
            if (smsIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(smsIntent)
            } else {
                // Fall back to general sharing
                shareOnboardingLinkGeneric(context, url)
            }
        } catch (e: Exception) {
            // If SMS fails, fall back to general sharing
            Toast.makeText(context, "Could not send SMS directly: ${e.message}", Toast.LENGTH_SHORT).show()
            shareOnboardingLinkGeneric(context, url)
        }
    } else {
        // No phone number, use general sharing
        shareOnboardingLinkGeneric(context, url)
    }
}

// Helper: country code to phone prefix
private val countryPhonePrefixes = mapOf(
    "IN" to "91",
    "US" to "1",
    "GB" to "44",
    "EU" to "", // EU is not a phone region, skip
    "CA" to "1",
    "AU" to "61",
    "JP" to "81",
    "CN" to "86",
    "RU" to "7",
    "BR" to "55",
    "ZA" to "27",
    "SG" to "65",
    "AE" to "971"
)

/**
 * Share onboarding link via WhatsApp if installed, otherwise use general sharing
 */
fun shareOnboardingLinkWhatsApp(
    context: Context,
    url: String,
    phoneNumbers: String, // comma-separated
    useWhatsApp: Boolean,
    countryCode: String? = null // ISO country code, e.g. "IN"
) {
    if (useWhatsApp && phoneNumbers.isNotBlank()) {
        // Always use the first phone number
        val firstPhone = phoneNumbers.split(",").map { it.trim() }.firstOrNull { it.isNotBlank() } ?: ""
        // Clean number
        var cleanPhone = firstPhone.replace("[^\\d]".toRegex(), "")
        // Get country code (default to IN)
        val isoCountry = countryCode?.uppercase() ?: "IN"
        val prefix = countryPhonePrefixes[isoCountry] ?: ""
        // If number is local (10 digits, starts with 0, etc.), prepend country code
        if (prefix.isNotEmpty()) {
            if (isoCountry == "IN" && cleanPhone.length == 10 && !cleanPhone.startsWith("91")) {
                cleanPhone = prefix + cleanPhone
            } else if (cleanPhone.startsWith("0")) {
                cleanPhone = prefix + cleanPhone.trimStart('0')
            } else if (cleanPhone.length in 7..10 && !cleanPhone.startsWith(prefix)) {
                cleanPhone = prefix + cleanPhone
            }
        }
        // WhatsApp requires country code, no leading zero, no plus, at least 10 digits
        val isValid = cleanPhone.length in 10..15 && !cleanPhone.startsWith("0")
        if (!isValid) {
            Toast.makeText(
                context,
                "Please enter the phone number in international format: country code, no +, no leading zero (e.g. 919876543210)",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        try {
            val message = java.net.URLEncoder.encode("Please fill out your onboarding form: $url", "UTF-8")
            val waUri = Uri.parse("https://api.whatsapp.com/send/?phone=$cleanPhone&text=$message&type=phone_number&app_absent=0")
            val intent = Intent(Intent.ACTION_VIEW, waUri)
            intent.setPackage("com.whatsapp")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Try WhatsApp app first
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback: open in browser (universal WhatsApp link)
                val browserIntent = Intent(Intent.ACTION_VIEW, waUri)
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (browserIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(browserIntent)
                } else {
                    // If even browser fails, show error
                    Toast.makeText(context, "Could not open WhatsApp or browser.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
            shareOnboardingLinkGeneric(context, url)
        }
    } else {
        // Use general sharing
        shareOnboardingLinkGeneric(context, url)
    }
}