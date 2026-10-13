package ai.sach.libraryup.ui.library

import ai.sach.libraryup.R
import ai.sach.libraryup.data.local.entity.MembershipStatus
import ai.sach.libraryup.data.local.entity.Seat
import ai.sach.libraryup.data.repository.LibraryState
import ai.sach.libraryup.domain.SeatMap
import ai.sach.libraryup.domain.SeatState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The seat map: every seat coloured by who is in it today. Filtering by shift answers
 * "which seats are free in the evening?" at a glance - the top complaint about the market
 * leader was that this took real effort.
 */
@Composable
fun SeatMapScreen(
    state: LibraryState,
    onOpenStudent: (Long) -> Unit,
    onAssignSeat: (seatId: Long, shiftId: Long?) -> Unit
) {
    val context = LocalContext.current
    var shiftFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    var openSeat by rememberSaveable { mutableStateOf<Long?>(null) }

    val shifts = state.activeShifts.associate { it.id to it.window }
    val bookingsBySeat = state.bookings.groupBy { it.seatId }
    val overdue = state.overdueMembershipIds
    val seatsByBlock = state.seats.groupBy { it.block }

    fun stateOf(seat: Seat): SeatState =
        SeatMap.stateOf(seat.active, shifts, bookingsBySeat[seat.id].orEmpty(), overdue, state.today, shiftFilter)

    Column(Modifier.fillMaxSize()) {
        ChipRow {
            FilterChip(selected = shiftFilter == null, onClick = { shiftFilter = null }, label = { Text(stringResource(R.string.filter_all_shifts)) })
            state.activeShifts.forEach { s ->
                FilterChip(selected = shiftFilter == s.id, onClick = { shiftFilter = s.id }, label = { Text(s.name) })
            }
        }
        val counts = state.seats.groupingBy { stateOf(it) }.eachCount()
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(StatusColors.free, "${stringResource(R.string.seat_free)} ${counts[SeatState.FREE] ?: 0}")
            if (shiftFilter == null) LegendDot(StatusColors.partial, "${stringResource(R.string.seat_partial)} ${counts[SeatState.PARTIAL] ?: 0}")
            LegendDot(StatusColors.full, "${stringResource(R.string.seat_full)} ${counts[SeatState.FULL] ?: 0}")
            LegendDot(StatusColors.overdue, "${stringResource(R.string.seat_overdue)} ${counts[SeatState.OVERDUE] ?: 0}")
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { shareVacancyPoster(context, state) }, enabled = state.seats.isNotEmpty()) {
                Icon(Icons.Default.Campaign, contentDescription = stringResource(R.string.action_vacancy_poster))
            }
        }
        if (state.seats.isEmpty()) {
            EmptyState(stringResource(R.string.empty_seats))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 56.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                seatsByBlock.forEach { (block, seats) ->
                    if (seatsByBlock.size > 1 || block.isNotBlank()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                block.ifBlank { stringResource(R.string.label_main_hall) },
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    items(seats, key = { it.id }) { seat ->
                        SeatTile(seat.label, stateOf(seat).color()) { openSeat = seat.id }
                    }
                }
            }
        }
    }

    val seat = openSeat?.let { id -> state.seatsById[id] }
    if (seat != null) {
        SeatDialog(
            state = state,
            seat = seat,
            onDismiss = { openSeat = null },
            onOpenStudent = { openSeat = null; onOpenStudent(it) },
            onAssign = { shiftId -> openSeat = null; onAssignSeat(seat.id, shiftId) }
        )
    }
}

@Composable
private fun SeatTile(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .aspectRatio(1f)
            .background(color.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

/** Who sits on this seat in each shift, with a shortcut to fill a free shift. */
@Composable
private fun SeatDialog(
    state: LibraryState,
    seat: Seat,
    onDismiss: () -> Unit,
    onOpenStudent: (Long) -> Unit,
    onAssign: (Long?) -> Unit
) {
    val occupants = state.memberships
        .filter { it.seatId == seat.id && it.status == MembershipStatus.ACTIVE }
        .mapNotNull { m -> state.students.firstOrNull { it.membership?.id == m.id } }
    val freeShifts = SeatMap.freeShiftIds(
        state.activeShifts.associate { it.id to it.window },
        state.bookings.filter { it.seatId == seat.id },
        state.today
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.seat_title, seat.label)) },
        text = {
            Column {
                if (seat.block.isNotBlank()) Text(seat.block, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (occupants.isEmpty()) Text(stringResource(R.string.seat_empty))
                occupants.forEach { s ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenStudent(s.student.id) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(s.student.name, fontWeight = FontWeight.SemiBold)
                            Text("${s.shift?.name ?: "-"} · ${s.shift?.timeLabel ?: ""}", style = MaterialTheme.typography.bodySmall)
                        }
                        s.status?.let { StatusPill(it) }
                    }
                }
                if (freeShifts.isNotEmpty() && seat.active) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.seat_free_in), style = MaterialTheme.typography.labelLarge)
                    state.activeShifts.filter { it.id in freeShifts }.forEach { sh ->
                        TextButton(onClick = { onAssign(sh.id) }) {
                            Text(stringResource(R.string.seat_assign_in, sh.name))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } }
    )
}
