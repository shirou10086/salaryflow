package com.salaryflow.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salaryflow.app.R
import com.salaryflow.app.core.Money
import com.salaryflow.app.core.TimeText
import com.salaryflow.app.core.toLocalDate
import com.salaryflow.app.data.Categories
import com.salaryflow.app.data.TxType
import com.salaryflow.app.data.TransactionEntity
import com.salaryflow.app.ui.SalaryFlowViewModel
import com.salaryflow.app.ui.components.EmptyState
import com.salaryflow.app.ui.components.PillSelect
import com.salaryflow.app.ui.components.SectionCard
import com.salaryflow.app.ui.components.TransactionRow
import com.salaryflow.app.ui.theme.BrandColors
import java.time.LocalDate
import java.time.YearMonth

private enum class LedgerFilter(val labelRes: Int) {
    ALL(R.string.ledger_all),
    EXPENSE(R.string.expense),
    INCOME(R.string.income),
    SAVING(R.string.ledger_saving),
}

@Composable
fun LedgerScreen(
    viewModel: SalaryFlowViewModel,
    onQuickAdd: (TxType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val month by viewModel.currentMonth.collectAsStateWithLifecycle()
    val monthTransactions by viewModel.monthTransactions.collectAsStateWithLifecycle()
    val expense by viewModel.monthExpenseCents.collectAsStateWithLifecycle()
    val income by viewModel.monthIncomeCents.collectAsStateWithLifecycle()
    val monthBalance = income - expense
    var filter by remember { mutableStateOf(LedgerFilter.ALL) }
    var pendingDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    val filtered = remember(monthTransactions, filter) {
        monthTransactions.filter { transaction ->
            when (filter) {
                LedgerFilter.ALL -> true
                LedgerFilter.EXPENSE -> transaction.type == TxType.EXPENSE
                LedgerFilter.INCOME -> transaction.type == TxType.INCOME
                LedgerFilter.SAVING -> transaction.type == TxType.SAVING_IN || transaction.type == TxType.SAVING_OUT
            }
        }
    }
    val grouped = remember(filtered) {
        filtered.groupBy { it.occurredAt.toLocalDate() }.toSortedMap(compareByDescending { it })
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            MonthSwitcher(
                month = month,
                onPrevious = { viewModel.shiftMonth(-1) },
                onNext = { viewModel.shiftMonth(1) },
                onToday = { viewModel.selectMonth(YearMonth.now()) },
            )
        }
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SummaryBlock(
                        stringResource(R.string.monthly_expense),
                        expense,
                        settings.hideAmounts,
                        BrandColors.expense,
                    )
                    SummaryBlock(
                        stringResource(R.string.monthly_income),
                        income,
                        settings.hideAmounts,
                        BrandColors.savings,
                    )
                    SummaryBlock(
                        stringResource(R.string.ledger_month_balance),
                        monthBalance,
                        settings.hideAmounts,
                        if (monthBalance >= 0) BrandColors.savings else BrandColors.expense,
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LedgerFilter.entries.forEach { item ->
                    PillSelect(
                        text = stringResource(item.labelRes),
                        selected = filter == item,
                        onClick = { filter = item },
                    )
                }
            }
        }

        if (grouped.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    title = stringResource(R.string.ledger_empty_title),
                    subtitle = stringResource(R.string.ledger_empty_subtitle),
                )
            }
        } else {
            grouped.forEach { (date, dayTransactions) ->
                item(key = "header-$date") {
                    DayHeader(date = date, transactions = dayTransactions, hidden = settings.hideAmounts)
                }
                item(key = "card-$date") {
                    SectionCard {
                        dayTransactions.forEachIndexed { index, transaction ->
                            TransactionRow(
                                transaction = transaction,
                                hidden = settings.hideAmounts,
                                trailing = {
                                    IconButton(onClick = { pendingDelete = transaction }) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = stringResource(R.string.action_delete),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                            )
                            if (index != dayTransactions.lastIndex) {
                                HorizontalDividerLine()
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { transaction ->
        val categoryName = stringResource(
            Categories.find(transaction.category).nameRes,
        )
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.ledger_delete_transaction)) },
            text = {
                Text(
                    stringResource(
                        R.string.ledger_delete_body,
                        "¥${Money.centsToYuan(transaction.amountCents)}",
                        categoryName,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(transaction)
                        pendingDelete = null
                    },
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun MonthSwitcher(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.Rounded.ChevronLeft,
                contentDescription = stringResource(R.string.ledger_previous_month),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                TimeText.monthYear(month),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (month != YearMonth.now()) {
                Text(
                    stringResource(R.string.ledger_back_to_current),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable(onClick = onToday)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        IconButton(onClick = onNext) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = stringResource(R.string.ledger_next_month),
            )
        }
    }
}

@Composable
private fun SummaryBlock(
    label: String,
    cents: Long,
    hidden: Boolean,
    color: androidx.compose.ui.graphics.Color,
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (hidden) "¥•••" else "¥${Money.compactYuan(cents)}",
            style = MaterialTheme.typography.titleLarge,
            color = color,
        )
    }
}

@Composable
private fun DayHeader(
    date: LocalDate,
    transactions: List<TransactionEntity>,
    hidden: Boolean,
) {
    val expense = transactions.filter { it.type == TxType.EXPENSE }.sumOf { it.amountCents }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            TimeText.day(date) + " · " + TimeText.weekday(date.dayOfWeek),
            style = MaterialTheme.typography.titleMedium,
        )
        if (expense > 0) {
            Text(
                if (hidden) {
                    stringResource(
                        R.string.ledger_day_expense_hidden,
                        stringResource(R.string.amount_hidden_short),
                    )
                } else {
                    stringResource(
                        R.string.ledger_day_expense,
                        Money.centsToYuan(expense),
                    )
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HorizontalDividerLine() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(start = 54.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
