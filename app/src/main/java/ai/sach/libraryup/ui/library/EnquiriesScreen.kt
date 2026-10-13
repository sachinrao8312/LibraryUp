package ai.sach.libraryup.ui.library

import ai.sach.libraryup.R
import ai.sach.libraryup.data.local.entity.Enquiry
import ai.sach.libraryup.data.local.entity.EnquiryStatus
import ai.sach.libraryup.data.repository.LibraryState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/** Walk-in and phone enquiries with a follow-up date; "Admit" turns one into a student. */
@Composable
fun EnquiriesScreen(state: LibraryState, vm: LibraryViewModel, onOpenStudent: (Long) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val all by remember { vm.repository.enquiries }.collectAsState(initial = emptyList())
    var showClosed by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Enquiry?>(null) }
    val today = LocalDate.now()
    val rows = all.filter { (it.status != EnquiryStatus.OPEN) == showClosed }.sortedBy { it.followUpOn ?: LocalDate.MAX }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { LibraryTopBar(stringResource(R.string.title_enquiries), onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = Enquiry(name = "", phone = "", followUpOn = today.plusDays(1)) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add_enquiry))
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ChipRow {
                FilterChip(selected = !showClosed, onClick = { showClosed = false }, label = { Text(stringResource(R.string.enquiries_open)) })
                FilterChip(selected = showClosed, onClick = { showClosed = true }, label = { Text(stringResource(R.string.enquiries_closed)) })
            }
            if (rows.isEmpty()) EmptyState(stringResource(R.string.empty_enquiries))
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                items(rows, key = { it.id }) { e ->
                    Row(
                        Modifier.fillMaxWidth().clickable { editing = e }.padding(start = 16.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(e.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                listOfNotNull(e.phone, e.wantedShiftId?.let { state.shiftsById[it]?.name }, e.note.ifBlank { null }).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        e.followUpOn?.takeIf { e.status == EnquiryStatus.OPEN }?.let {
                            Text(it.pretty(), style = MaterialTheme.typography.bodySmall, color = if (it.isAfter(today)) Color.Unspecified else StatusColors.overdue)
                        }
                        IconButton(onClick = { dial(context, e.phone) }) { Icon(Icons.Default.Call, contentDescription = stringResource(R.string.action_call)) }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    editing?.let { e ->
        var name by rememberSaveable(e.id) { mutableStateOf(e.name) }
        var phone by rememberSaveable(e.id) { mutableStateOf(e.phone) }
        var shiftId by rememberSaveable(e.id) { mutableStateOf(e.wantedShiftId) }
        var followUp by rememberSaveable(e.id) { mutableStateOf((e.followUpOn ?: today).toEpochDay()) }
        var note by rememberSaveable(e.id) { mutableStateOf(e.note) }
        val shifts: List<Pair<Long?, String>> = listOf(null to stringResource(R.string.label_any_shift)) + state.activeShifts.map { it.id to it.name }
        fun edited() = e.copy(name = name, phone = phone, wantedShiftId = shiftId, followUpOn = LocalDate.ofEpochDay(followUp), note = note.trim())
        fun close() { editing = null }
        AlertDialog(
            onDismissRequest = ::close,
            title = { Text(stringResource(if (e.id == 0L) R.string.action_add_enquiry else R.string.title_enquiry)) },
            text = {
                Column {
                    TextInput(name, { name = it }, stringResource(R.string.field_name))
                    TextInput(phone, { phone = it }, stringResource(R.string.field_phone), keyboardType = KeyboardType.Phone)
                    Picker(stringResource(R.string.field_shift), shifts, shifts.firstOrNull { it.first == shiftId }, { it.second }, { shiftId = it.first })
                    DateField(stringResource(R.string.field_follow_up), LocalDate.ofEpochDay(followUp), { followUp = it.toEpochDay() })
                    TextInput(note, { note = it }, stringResource(R.string.field_note))
                    if (e.id != 0L && e.status == EnquiryStatus.OPEN) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.run { onOpenStudent(convertEnquiry(edited())) }; close() }) { Text(stringResource(R.string.action_admit)) }
                        OutlinedButton(onClick = { vm.run { saveEnquiry(edited().copy(status = EnquiryStatus.LOST)) }; close() }) { Text(stringResource(R.string.action_mark_lost)) }
                    }
                }
            },
            confirmButton = {
                Button(enabled = name.isNotBlank() && phone.isNotBlank(), onClick = { vm.run { saveEnquiry(edited()) }; close() }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = { TextButton(onClick = ::close) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}
