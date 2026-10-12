package ai.sach.libraryup.ui.library

import ai.sach.libraryup.R
import ai.sach.libraryup.data.local.entity.DurationUnit
import ai.sach.libraryup.data.local.entity.LibraryProfile
import ai.sach.libraryup.data.local.entity.Plan
import ai.sach.libraryup.data.local.entity.Shift
import ai.sach.libraryup.domain.FeeCycleMode
import ai.sach.libraryup.domain.UpiLink
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Shift presets most self-study libraries use. Owners can edit them later in Settings. */
private data class ShiftPreset(val nameRes: Int, val start: Int, val end: Int)

private val SHIFT_PRESETS = listOf(
    ShiftPreset(R.string.shift_morning, 6 * 60, 12 * 60),
    ShiftPreset(R.string.shift_afternoon, 12 * 60, 18 * 60),
    ShiftPreset(R.string.shift_evening, 18 * 60, 24 * 60),
    ShiftPreset(R.string.shift_full_day, 0, 24 * 60)
)

/**
 * First-run setup: live in about five minutes. Four short steps: library, shifts,
 * seats, fees. Everything can be changed afterwards in Library settings.
 */
@Composable
fun SetupScreen(vm: LibraryViewModel) {
    var step by rememberSaveable { mutableStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var ownerName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var shiftsOn by rememberSaveable { mutableStateOf(listOf(true, true, true, true).joinToString(",")) }
    var seatCount by rememberSaveable { mutableStateOf("40") }
    var blockName by rememberSaveable { mutableStateOf("") }
    var monthlyFee by rememberSaveable { mutableStateOf("800") }
    var fullDayFee by rememberSaveable { mutableStateOf("1200") }
    var addQuarterly by rememberSaveable { mutableStateOf(true) }
    var calendarMonth by rememberSaveable { mutableStateOf(true) }
    var upi by rememberSaveable { mutableStateOf("") }
    var hindi by rememberSaveable { mutableStateOf(false) }

    val shiftFlags = shiftsOn.split(",").map { it == "true" }
    val shiftNames = SHIFT_PRESETS.map { stringResource(it.nameRes) }
    val steps = 4
    val canNext = when (step) {
        0 -> name.isNotBlank()
        1 -> shiftFlags.any { it }
        2 -> (seatCount.toIntOrNull() ?: 0) in 1..1000
        else -> monthlyFee.toAmount() > 0 && (upi.isBlank() || UpiLink.isValidVpa(upi))
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(20.dp)) {
        Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.setup_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(progress = { (step + 1) / steps.toFloat() }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))

        when (step) {
            0 -> {
                SectionTitle(stringResource(R.string.setup_step_library))
                TextInput(name, { name = it }, stringResource(R.string.field_library_name))
                TextInput(ownerName, { ownerName = it }, stringResource(R.string.field_owner_name))
                TextInput(phone, { phone = it }, stringResource(R.string.field_phone), keyboardType = KeyboardType.Phone)
                TextInput(address, { address = it }, stringResource(R.string.field_address), singleLine = false)
                SectionTitle(stringResource(R.string.setup_message_language))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !hindi, onClick = { hindi = false }, label = { Text("English") })
                    FilterChip(selected = hindi, onClick = { hindi = true }, label = { Text("हिंदी") })
                }
            }
            1 -> {
                SectionTitle(stringResource(R.string.setup_step_shifts))
                Text(stringResource(R.string.setup_shifts_hint), style = MaterialTheme.typography.bodySmall)
                SHIFT_PRESETS.forEachIndexed { i, p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = shiftFlags[i], onCheckedChange = { on ->
                            shiftsOn = shiftFlags.mapIndexed { j, v -> if (j == i) on else v }.joinToString(",")
                        })
                        Text("${shiftNames[i]} · ${Shift.hhmm(p.start)}–${Shift.hhmm(p.end)}")
                    }
                }
            }
            2 -> {
                SectionTitle(stringResource(R.string.setup_step_seats))
                TextInput(seatCount, { seatCount = it.filter(Char::isDigit).take(4) }, stringResource(R.string.field_seat_count), keyboardType = KeyboardType.Number)
                TextInput(blockName, { blockName = it }, stringResource(R.string.field_block_name))
                Text(stringResource(R.string.setup_seats_hint), style = MaterialTheme.typography.bodySmall)
            }
            else -> {
                SectionTitle(stringResource(R.string.setup_step_fees))
                AmountInput(monthlyFee, { monthlyFee = it }, stringResource(R.string.field_monthly_fee_shift))
                if (shiftFlags.last()) AmountInput(fullDayFee, { fullDayFee = it }, stringResource(R.string.field_monthly_fee_full_day))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = addQuarterly, onCheckedChange = { addQuarterly = it })
                    Text(stringResource(R.string.setup_add_quarterly))
                }
                SectionTitle(stringResource(R.string.field_fee_cycle))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = calendarMonth, onClick = { calendarMonth = true }, label = { Text(stringResource(R.string.fee_cycle_calendar)) })
                    FilterChip(selected = !calendarMonth, onClick = { calendarMonth = false }, label = { Text(stringResource(R.string.fee_cycle_30)) })
                }
                TextInput(upi, { upi = it.trim() }, stringResource(R.string.field_upi_id),
                    isError = upi.isNotBlank() && !UpiLink.isValidVpa(upi), supportingText = stringResource(R.string.hint_upi))
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (step > 0) OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_back)) }
            Button(
                enabled = canNext,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (step < steps - 1) { step++; return@Button }
                    val shifts = SHIFT_PRESETS.indices.filter { shiftFlags[it] }.map { i ->
                        Shift(name = shiftNames[i], startMinute = SHIFT_PRESETS[i].start, endMinute = SHIFT_PRESETS[i].end)
                    }
                    val fee = monthlyFee.toAmount()
                    val fullFee = fullDayFee.toAmount().takeIf { it > 0 } ?: fee
                    val plans = buildList {
                        add(Plan(name = "Monthly", durationUnit = DurationUnit.MONTHS, durationValue = 1, price = fee))
                        if (addQuarterly) add(Plan(name = "Quarterly", durationUnit = DurationUnit.MONTHS, durationValue = 3, price = fee * 3))
                        if (shiftFlags.last()) {
                            // Plans are not tied to a shift in v0.1; the owner picks the right one at admission.
                            add(Plan(name = "Monthly · Full day", durationUnit = DurationUnit.MONTHS, durationValue = 1, price = fullFee))
                        }
                    }
                    val profile = LibraryProfile(
                        name = name.trim(), ownerName = ownerName.trim(), phone = phone.trim(), address = address.trim(),
                        upiVpa = upi.trim(), messageLanguage = if (hindi) "hi" else "en",
                        feeCycleMode = if (calendarMonth) FeeCycleMode.CALENDAR_MONTH else FeeCycleMode.FIXED_DAYS
                    )
                    vm.run(R.string.msg_setup_done) {
                        completeSetup(profile, shifts, listOf(blockName.trim() to (seatCount.toIntOrNull() ?: 0)), plans)
                    }
                }
            ) { Text(stringResource(if (step < steps - 1) R.string.action_next else R.string.action_finish)) }
        }
    }
}
