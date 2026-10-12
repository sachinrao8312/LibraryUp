package ai.sach.rentup.ui.screens.settingsScreen.appprefernecs

import ai.sach.rentup.RentUpApplication
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Currency

/**
 * Utility functions for currency formatting and handling based on user preferences.
 */

/**
 * Get the currency symbol for a given currency code.
 *
 * @param currencyCode The ISO 4217 currency code (e.g., "USD", "INR")
 * @return The currency symbol (e.g., "$", "₹")
 */
fun getCurrencySymbol(currencyCode: String): String {
    return try {
        Currency.getInstance(currencyCode).symbol
    } catch (e: Exception) {
        currencyCode
    }
}

fun formatCurrencyString(amount: String?, currencySymbol: String): String {
    if (amount.isNullOrBlank()) return ""

    // Replace existing currency symbol if present
    return if (amount.startsWith("$") || amount.startsWith("₹") || amount.startsWith("£") ||
        amount.startsWith("€") || amount.startsWith("¥")) {
        currencySymbol + amount.substring(1)
    } else {
        currencySymbol + amount
    }
}

@Composable
fun formatWithCurrency(amount: String?): String {
    if (amount.isNullOrBlank()) return ""

    val context = LocalContext.current
    val appPreferencesRepository = remember {
        (context.applicationContext as RentUpApplication).appPreferencesRepository
    }
    val currencyCode by appPreferencesRepository.currency.collectAsState(initial = "USD")
    val currencySymbol = getCurrencySymbol(currencyCode)

    // Replace existing currency symbol if present
    return if (amount.startsWith("$") || amount.startsWith("₹") || amount.startsWith("£") ||
        amount.startsWith("€") || amount.startsWith("¥")) {
        currencySymbol + amount.substring(1)
    } else {
        currencySymbol + amount
    }
}