package ai.sach.libraryup.ui.library

import ai.sach.libraryup.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.PictureAsPdf
import ai.sach.libraryup.data.local.entity.Expense
import ai.sach.libraryup.data.local.entity.PaymentMode
import ai.sach.libraryup.data.repository.LibraryState
import ai.sach.libraryup.domain.MoneyEntry
import ai.sach.libraryup.domain.Reports
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

val monthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

@Composable
private fun MonthSwitcher(month: YearMonth, onChange: (YearMonth) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        IconButton(onClick = { onChange(month.minusMonths(1)) }) { Icon(Icons.Default.ChevronLeft, contentDescription = null) }
        Text(month.format(monthFormat), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange(month.plusMonths(1)) }, enabled = month < YearMonth.now()) {
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

/** Every payment in a month, newest first, with the month's total. */
@Composable
fun PaymentsScreen(state: LibraryState, onOpenStudent: (Long) -> Unit) {
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthText)
    val names = state.students.associate { it.student.id to it.student.name }
    val rows = state.payments.filter { !it.deleted && YearMonth.from(it.paidOn) == month }
    val total = rows.sumOf { it.amount }
    val byMode = rows.groupBy { it.mode }.mapValues { (_, v) -> v.sumOf { it.amount } }

    Column(Modifier.fillMaxSize()) {
        MonthSwitcher(month) { monthText = it.toString() }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.label_total), rupees(total), Modifier.weight(1f))
            StatCard(stringResource(R.string.mode_cash), rupees(byMode[PaymentMode.CASH] ?: 0L), Modifier.weight(1f))
            StatCard(stringResource(R.string.mode_upi), rupees(byMode[PaymentMode.UPI] ?: 0L), Modifier.weight(1f))
        }
        if (rows.isEmpty()) EmptyState(stringResource(R.string.empty_payments_month))
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(rows, key = { it.id }) { p ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpenStudent(p.studentId) }.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(names[p.studentId] ?: "-", fontWeight = FontWeight.SemiBold)
                        Text("${p.receiptNo} · ${p.paidOn.pretty()} · ${stringResource(p.mode.label())}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(rupees(p.amount), fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider()
            }
        }
    }
}

/** Expenses and the monthly profit-and-loss summary. */
@Composable
fun ExpensesScreen(state: LibraryState, vm: LibraryViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val expenses by remember { vm.repository.expenses }.collectAsState(initial = emptyList())
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthText)
    var editing by remember { mutableStateOf<Expense?>(null) }
    val live = expenses.filter { !it.deleted }
    val profit = Reports.profitByMonth(
        state.payments.filter { !it.deleted }.map { MoneyEntry(it.paidOn, it.amount) },
        live.map { MoneyEntry(it.date, it.amount) }
    ).firstOrNull { it.month == month }
    val rows = live.filter { YearMonth.from(it.date) == month }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LibraryTopBar(stringResource(R.string.title_expenses), onBack = onBack) {
                IconButton(onClick = { shareMonthlyReport(context, state, expenses, month) }) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource(R.string.action_monthly_report))
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = Expense(category = "", amount = 0, date = LocalDate.now()) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add_expense))
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MonthSwitcher(month) { monthText = it.toString() }
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(stringResource(R.string.label_income), rupees(profit?.income ?: 0L), Modifier.weight(1f))
                StatCard(stringResource(R.string.label_expenses), rupees(profit?.expenses ?: 0L), Modifier.weight(1f))
                StatCard(
                    stringResource(R.string.label_profit), rupees(profit?.profit ?: 0L), Modifier.weight(1f),
                    accent = if ((profit?.profit ?: 0L) < 0) StatusColors.full else StatusColors.paid
                )
            }
            if (rows.isEmpty()) EmptyState(stringResource(R.string.empty_expenses))
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                items(rows, key = { it.id }) { e ->
                    Row(
                        Modifier.fillMaxWidth().clickable { editing = e }.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(e.category, fontWeight = FontWeight.SemiBold)
                            Text(e.date.pretty() + if (e.note.isNotBlank()) " · ${e.note}" else "", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(rupees(e.amount))
                        IconButton(onClick = { vm.run(R.string.msg_deleted) { setExpenseDeleted(e, true) } }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    editing?.let { e ->
        ExpenseDialog(e, onDismiss = { editing = null }) { saved ->
            editing = null
            vm.run(R.string.msg_saved) { saveExpense(saved) }
        }
    }
}

@Composable
private fun ExpenseDialog(expense: Expense, onDismiss: () -> Unit, onSave: (Expense) -> Unit) {
    val presets = listOf(
        R.string.expense_rent, R.string.expense_electricity, R.string.expense_internet,
        R.string.expense_salary, R.string.expense_cleaning, R.string.expense_other
    ).map { stringResource(it) }
    var category by rememberSaveable { mutableStateOf(expense.category.ifBlank { presets.first() }) }
    var amount by rememberSaveable { mutableStateOf(if (expense.amount > 0) expense.amount.toString() else "") }
    var dateEpoch by rememberSaveable { mutableStateOf(expense.date.toEpochDay()) }
    var note by rememberSaveable { mutableStateOf(expense.note) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_add_expense)) },
        text = {
            Column {
                ChipRow { presets.forEach { p -> FilterChip(selected = category == p, onClick = { category = p }, label = { Text(p) }) } }
                TextInput(category, { category = it }, stringResource(R.string.field_category))
                AmountInput(amount, { amount = it }, stringResource(R.string.field_amount))
                DateField(stringResource(R.string.field_date), LocalDate.ofEpochDay(dateEpoch), { dateEpoch = it.toEpochDay() })
                TextInput(note, { note = it }, stringResource(R.string.field_note))
            }
        },
        confirmButton = {
            Button(enabled = category.isNotBlank() && amount.toAmount() > 0, onClick = {
                onSave(expense.copy(category = category.trim(), amount = amount.toAmount(), date = LocalDate.ofEpochDay(dateEpoch), note = note.trim()))
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
