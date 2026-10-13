package ai.sach.libraryup.ui.library

import ai.sach.libraryup.R
import ai.sach.libraryup.data.local.entity.Locker
import ai.sach.libraryup.data.local.entity.StudentStatus
import ai.sach.libraryup.data.repository.LibraryState
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Lockers: number, monthly fee and who has it. Leaving or deleting a student frees their locker. */
@Composable
fun LockersScreen(state: LibraryState, vm: LibraryViewModel, onBack: () -> Unit) {
    val lockers by remember { vm.repository.lockers }.collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<Locker?>(null) }
    val names = state.students.associate { it.student.id to it.student.name }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { LibraryTopBar(stringResource(R.string.title_lockers), onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = Locker(number = "", fee = lockers.lastOrNull()?.fee ?: 0) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add_locker))
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                stringResource(R.string.lockers_free_count, lockers.count { it.studentId == null }, lockers.size),
                modifier = Modifier.padding(16.dp)
            )
            if (lockers.isEmpty()) EmptyState(stringResource(R.string.empty_lockers))
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                items(lockers, key = { it.id }) { l ->
                    Row(
                        Modifier.fillMaxWidth().clickable { editing = l }.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.label_locker, l.number), fontWeight = FontWeight.SemiBold)
                            Text(l.studentId?.let(names::get) ?: stringResource(R.string.locker_free), style = MaterialTheme.typography.bodySmall)
                        }
                        Text(rupees(l.fee))
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    editing?.let { l ->
        val taken = lockers.filter { it.id != l.id }.mapNotNull { it.studentId }.toSet()
        val students = state.students.map { it.student }.filter { it.status != StudentStatus.LEFT && it.id !in taken }
        val options: List<Pair<Long?, String>> = listOf(null to stringResource(R.string.locker_free)) + students.map { it.id to it.name }
        var number by rememberSaveable(l.id) { mutableStateOf(l.number) }
        var fee by rememberSaveable(l.id) { mutableStateOf(if (l.fee > 0) l.fee.toString() else "") }
        var studentId by rememberSaveable(l.id) { mutableStateOf(l.studentId) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(stringResource(if (l.id == 0L) R.string.action_add_locker else R.string.title_edit_locker)) },
            text = {
                Column {
                    TextInput(number, { number = it.take(10) }, stringResource(R.string.field_locker_number))
                    AmountInput(fee, { fee = it }, stringResource(R.string.field_locker_fee))
                    Picker(stringResource(R.string.field_assigned_to), options, options.first { it.first == studentId }, { it.second }, { studentId = it.first })
                }
            },
            confirmButton = {
                Button(enabled = number.isNotBlank(), onClick = {
                    vm.run(onDone = { editing = null }) { saveLocker(l.copy(number = number, fee = fee.toAmount(), studentId = studentId)) }
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                Row {
                    if (l.id != 0L) TextButton(onClick = { vm.run(R.string.msg_deleted) { deleteLocker(l) }; editing = null }) {
                        Text(stringResource(R.string.action_delete))
                    }
                    TextButton(onClick = { editing = null }) { Text(stringResource(R.string.action_cancel)) }
                }
            }
        )
    }
}
