package ai.sach.libraryup.ui.library

import ai.sach.libraryup.R
import ai.sach.libraryup.domain.DueStatus
import ai.sach.libraryup.domain.SeatState
import ai.sach.libraryup.ui.theme.LocalStatusColors
import ai.sach.libraryup.ui.theme.ToneSet
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/* ---------------------------------------------------------------- colours */

/** A status colour pair: a container and the text/icon colour that goes on it. */
@Immutable
data class Tone(val container: Color, val content: Color)

private val ToneSet.tone get() = Tone(container, onContainer)

/** Free green, part-booked amber, full neutral, fee problem red: red only ever means "act on this". */
@Composable
fun SeatState.tone(): Tone {
    val status = LocalStatusColors.current
    val c = MaterialTheme.colorScheme
    return when (this) {
        SeatState.FREE -> status.success.tone
        SeatState.PARTIAL -> status.warning.tone
        SeatState.FULL -> Tone(c.surfaceContainerHighest, c.onSurfaceVariant)
        SeatState.OVERDUE -> Tone(c.errorContainer, c.onErrorContainer)
        SeatState.INACTIVE -> Tone(c.surfaceContainer, c.outline)
    }
}

@Composable
fun DueStatus.tone(): Tone {
    val status = LocalStatusColors.current
    val c = MaterialTheme.colorScheme
    return when (this) {
        DueStatus.OVERDUE, DueStatus.EXPIRED -> Tone(c.errorContainer, c.onErrorContainer)
        DueStatus.DUE_TODAY, DueStatus.UNPAID -> status.warning.tone
        DueStatus.EXPIRING_SOON -> Tone(c.secondaryContainer, c.onSecondaryContainer)
        DueStatus.PAID -> status.success.tone
        DueStatus.FROZEN -> Tone(c.secondaryContainer, c.onSecondaryContainer)
    }
}

/** Positive money and "done" ticks. */
val successColor: Color @Composable get() = LocalStatusColors.current.success.color

@StringRes
fun DueStatus.label(): Int = when (this) {
    DueStatus.OVERDUE -> R.string.status_overdue
    DueStatus.EXPIRED -> R.string.status_expired
    DueStatus.DUE_TODAY -> R.string.status_due_today
    DueStatus.UNPAID -> R.string.status_unpaid
    DueStatus.EXPIRING_SOON -> R.string.status_expiring
    DueStatus.PAID -> R.string.status_paid
    DueStatus.FROZEN -> R.string.status_frozen
}

/* ---------------------------------------------------------------- building blocks */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            }
        },
        actions = { actions() }
    )
}

@Composable
fun StatusPill(text: String, tone: Tone, modifier: Modifier = Modifier) {
    Surface(color = tone.container, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(
            text,
            color = tone.content,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StatusPill(status: DueStatus, modifier: Modifier = Modifier) =
    StatusPill(stringResource(status.label()), status.tone(), modifier)

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

/** A number with its label on a tinted card; [tone] colours it by status. */
@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, tone: Tone? = null, onClick: (() -> Unit)? = null) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = tone?.container ?: MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = tone?.content ?: MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 2)
        }
    }
}

/** First letter of a name in a tinted circle: makes long lists scannable. */
@Composable
fun Avatar(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.trim().take(1).uppercase(),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LegendDot(tone: Tone, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(tone.container, RoundedCornerShape(3.dp)).border(1.dp, tone.content.copy(alpha = 0.4f), RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

/* ---------------------------------------------------------------- form fields */

@Composable
fun TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

/** Whole-rupee amount; keeps only digits. */
@Composable
fun AmountInput(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) =
    TextInput(value, { v -> onValueChange(v.filter { it.isDigit() }.take(9)) }, label, modifier, KeyboardType.Number)

fun String.toAmount(): Long = toLongOrNull() ?: 0L

/** A read-only field that opens a menu of [options]. */
@Composable
fun <T> Picker(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        OutlinedButton(
            onClick = { open = true },
            enabled = enabled && options.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(selected?.let(optionLabel) ?: stringResource(R.string.hint_choose), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(optionLabel(option)) }, onClick = { onSelected(option); open = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, date: LocalDate, onDate: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(date.pretty())
        }
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.action_cancel)) } }
        ) { DatePicker(state = state) }
    }
}

@Composable
fun ChipRow(content: @Composable () -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
    ) {
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() } }
    }
}
