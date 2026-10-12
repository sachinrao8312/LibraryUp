package ai.sach.libraryup.ui.library

import ai.sach.libraryup.R
import ai.sach.libraryup.data.local.entity.StudentStatus
import ai.sach.libraryup.data.repository.LibraryState
import ai.sach.libraryup.data.repository.StudentOverview
import ai.sach.libraryup.domain.DueStatus
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class StudentFilter(@StringRes val label: Int) {
    ALL(R.string.filter_all),
    DUE(R.string.filter_due),
    OVERDUE(R.string.filter_overdue),
    EXPIRING(R.string.filter_expiring),
    NO_SEAT(R.string.filter_no_seat),
    FROZEN(R.string.filter_frozen),
    LEFT(R.string.filter_left);

    fun matches(s: StudentOverview): Boolean = when (this) {
        ALL -> s.student.status != StudentStatus.LEFT
        DUE -> s.balance > 0 && s.student.status == StudentStatus.ACTIVE
        OVERDUE -> s.status == DueStatus.OVERDUE || s.status == DueStatus.EXPIRED
        EXPIRING -> s.status == DueStatus.EXPIRING_SOON || s.status == DueStatus.DUE_TODAY
        NO_SEAT -> s.membership == null && s.student.status != StudentStatus.LEFT
        FROZEN -> s.student.status == StudentStatus.FROZEN
        LEFT -> s.student.status == StudentStatus.LEFT
    }
}

/** Sorts the most urgent first: overdue, then due, then by name. */
private val urgency = compareBy<StudentOverview>({ it.status?.ordinal ?: Int.MAX_VALUE }, { it.student.name.lowercase() })

@Composable
fun StudentsScreen(
    state: LibraryState,
    initialFilter: StudentFilter,
    onOpenStudent: (Long) -> Unit,
    onAddStudent: () -> Unit
) {
    var filter by rememberSaveable(initialFilter) { mutableStateOf(initialFilter) }
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim().lowercase()
    val visible = state.students
        .filter { filter.matches(it) }
        .filter {
            q.isEmpty() || it.student.name.lowercase().contains(q) || it.student.phone.contains(q) ||
                it.seat?.label?.lowercase() == q
        }
        .sortedWith(urgency)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddStudent,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add_student)) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.hint_search_students)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            ChipRow {
                StudentFilter.values().forEach { f ->
                    val count = state.students.count { f.matches(it) }
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text("${stringResource(f.label)} ($count)") }
                    )
                }
            }
            if (visible.isEmpty()) {
                EmptyState(stringResource(if (state.students.isEmpty()) R.string.empty_students else R.string.empty_filter))
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(visible, key = { it.student.id }) { s ->
                        StudentRow(s) { onOpenStudent(s.student.id) }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun StudentRow(s: StudentOverview, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(s.student.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val seatLine = if (s.membership == null) stringResource(R.string.label_no_seat)
            else stringResource(R.string.label_seat_shift, s.seat?.label ?: "-", s.shift?.name ?: "-")
            Text(seatLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            s.membership?.let {
                Text(
                    stringResource(R.string.label_due_on, it.dueDate.pretty()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            when {
                s.student.status == StudentStatus.LEFT -> StatusPill(stringResource(R.string.status_left), StatusColors.inactive)
                s.status != null -> StatusPill(s.status)
            }
            if (s.balance > 0) {
                Spacer(Modifier.height(4.dp))
                Text(rupees(s.balance), color = StatusColors.full, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
