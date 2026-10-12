package ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Component for displaying the current country selection and opening the country picker dialog.
 *
 * @param selectedCountry The currently selected country
 * @param onSelectCountry Callback for when a new country is selected
 * @param isDarkTheme Whether the app is in dark mode
 */
@Composable
fun CountrySelector(
    selectedCountry: CountryOption,
    onSelectCountry: (CountryOption) -> Unit,
    isDarkTheme: Boolean
) {
    var showCountryPicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Text(
                text = "Country & Currency",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Current selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDarkTheme) Color(0xFF2A2A2A) else Color(0xFFF5F5F5))
                    .clickable { showCountryPicker = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flag
                Text(
                    text = selectedCountry.flag,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )

                // Country name and currency info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = selectedCountry.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "${selectedCountry.currencySymbol} ${selectedCountry.currencyCode}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                // Dropdown indicator
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Country",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Explanation text
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Changes currency symbol throughout the app",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }

    // Country picker dialog
    if (showCountryPicker) {
        CountryPickerDialog(
            currentCountry = selectedCountry,
            onCountrySelected = {
                onSelectCountry(it)
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false },
            isDarkTheme = isDarkTheme
        )
    }
}

/**
 * Dialog component for selecting a country from a list.
 * Includes search functionality to filter the list of countries.
 *
 * @param currentCountry Currently selected country
 * @param onCountrySelected Callback for when a new country is selected
 * @param onDismiss Callback for when the dialog is dismissed
 * @param isDarkTheme Whether the app is in dark mode
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryPickerDialog(
    currentCountry: CountryOption,
    onCountrySelected: (CountryOption) -> Unit,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    val backgroundColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Dialog title
                Text(
                    text = "Select Country",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search countries") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (isDarkTheme) Color.Gray else Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Country list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .height(400.dp)
                ) {
                    val filteredCountries = if (searchQuery.isEmpty()) {
                        SupportedCountries.countries
                    } else {
                        SupportedCountries.countries.filter {
                            it.name.contains(searchQuery, ignoreCase = true) ||
                                    it.code.contains(searchQuery, ignoreCase = true) ||
                                    it.currencyCode.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    items(filteredCountries) { country ->
                        val isSelected = country.code == currentCountry.code

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCountrySelected(country) }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Flag emoji
                            Text(
                                text = country.flag,
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 16.dp)
                            )

                            // Country details
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = country.name,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )

                                Text(
                                    text = "${country.currencySymbol} ${country.currencyCode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor.copy(alpha = 0.7f)
                                )
                            }

                            // Selection indicator
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Divider between items
                        if (country != filteredCountries.last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = if (isDarkTheme) Color.DarkGray else Color.LightGray.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Show message if no results
                    if (filteredCountries.isEmpty()) {
                        item {
                            Text(
                                text = "No countries found for \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}