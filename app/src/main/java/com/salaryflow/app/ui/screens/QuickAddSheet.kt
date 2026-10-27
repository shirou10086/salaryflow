package com.salaryflow.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.salaryflow.app.R
import com.salaryflow.app.core.Money
import com.salaryflow.app.data.Categories
import com.salaryflow.app.data.Category
import com.salaryflow.app.data.TxType
import com.salaryflow.app.ui.components.CategoryBadge
import com.salaryflow.app.ui.components.PillSelect
import com.salaryflow.app.ui.theme.BrandColors

private val addTypes = listOf(
    TxType.EXPENSE,
    TxType.INCOME,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    initialType: TxType,
    onDismiss: () -> Unit,
    onSubmit: (TxType, Long, String, String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var type by remember(initialType) { mutableStateOf(initialType) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var categoryKey by remember(type) {
        mutableStateOf(categoriesFor(type).first().key)
    }

    val amountCents = Money.yuanToCents(amountText) ?: 0L
    val valid = amountCents > 0
    val accent = when (type) {
        TxType.EXPENSE -> BrandColors.expense
        TxType.INCOME -> BrandColors.savings
        TxType.SAVING_IN -> MaterialTheme.colorScheme.primary
        TxType.SAVING_OUT -> MaterialTheme.colorScheme.tertiary
    }
    val sheetTitleRes = when (type) {
        TxType.EXPENSE -> R.string.quick_add_expense_title
        TxType.INCOME -> R.string.quick_add_income_title
        TxType.SAVING_IN -> R.string.quick_add_saving_title
        TxType.SAVING_OUT -> R.string.quick_add_withdraw_title
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(sheetTitleRes),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        stringResource(R.string.quick_add_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = {
                        onSubmit(type, amountCents, categoryKey, note)
                        onDismiss()
                    },
                    enabled = valid,
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        stringResource(R.string.quick_add_submit),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                addTypes.forEach { candidate ->
                    PillSelect(
                        text = stringResource(typeShortLabelRes(candidate)),
                        selected = type == candidate,
                        onClick = { type = candidate },
                        selectedContainerColor = when (candidate) {
                            TxType.EXPENSE -> BrandColors.expense
                            TxType.INCOME -> BrandColors.savings
                            TxType.SAVING_IN -> MaterialTheme.colorScheme.primary
                            TxType.SAVING_OUT -> MaterialTheme.colorScheme.tertiary
                        },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    amountText = input.filter { it.isDigit() || it == '.' }.take(12)
                },
                label = { Text(stringResource(R.string.quick_add_amount)) },
                prefix = { Text("¥ ") },
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.displayMedium,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.quick_add_category),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(9.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categoriesFor(type).forEach { category ->
                    CategoryChip(
                        category = category,
                        selected = categoryKey == category.key,
                        accent = accent,
                        onClick = { categoryKey = category.key },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(40) },
                label = { Text(stringResource(R.string.quick_add_note)) },
                placeholder = { Text(stringResource(R.string.quick_add_optional)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: Category,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val categoryName = stringResource(category.nameRes)
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accent else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(start = 7.dp, end = 11.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryBadge(
                categoryName = categoryName,
                size = 28,
                contentColor = if (selected) Color.White else accent,
            )
            Spacer(Modifier.size(7.dp))
            Text(categoryName, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun typeShortLabelRes(type: TxType): Int = when (type) {
    TxType.EXPENSE -> R.string.expense
    TxType.INCOME -> R.string.income
    TxType.SAVING_IN -> R.string.saving
    TxType.SAVING_OUT -> R.string.withdrawal
}

private fun categoriesFor(type: TxType): List<Category> = when (type) {
    TxType.EXPENSE -> Categories.expenses
    TxType.INCOME -> Categories.incomes
    TxType.SAVING_IN -> Categories.savings.filterNot { it.key == "saving_out" }
    TxType.SAVING_OUT -> Categories.savings.filter { it.key == "saving_out" }
}
