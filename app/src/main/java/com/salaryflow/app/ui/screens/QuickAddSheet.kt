package com.salaryflow.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.salaryflow.app.core.Money
import com.salaryflow.app.data.Categories
import com.salaryflow.app.data.Category
import com.salaryflow.app.data.TxType
import com.salaryflow.app.ui.components.PillSelect

private val addTypes = listOf(
    TxType.EXPENSE to "支出",
    TxType.INCOME to "收入",
    TxType.SAVING_IN to "存入攒钱罐",
    TxType.SAVING_OUT to "从罐中取出",
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
        ) {
            Text("记一笔", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                addTypes.forEach { (candidate, label) ->
                    PillSelect(
                        text = label,
                        selected = type == candidate,
                        onClick = { type = candidate },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    amountText = input.filter { it.isDigit() || it == '.' }.take(12)
                },
                label = { Text("金额（元）") },
                prefix = { Text("¥") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.displayMedium,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Text("分类", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categoriesFor(type).forEach { category ->
                    CategoryPill(
                        category = category,
                        selected = categoryKey == category.key,
                        onClick = { categoryKey = category.key },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(40) },
                label = { Text("备注（可选）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    onSubmit(type, amountCents, categoryKey, note)
                    onDismiss()
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(
                    if (valid) {
                        "保存 ¥${Money.centsToYuan(amountCents)}"
                    } else {
                        "请输入金额"
                    },
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun CategoryPill(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
) {
    PillSelect(
        text = "${category.emoji} ${category.name}",
        selected = selected,
        onClick = onClick,
    )
}

private fun categoriesFor(type: TxType): List<Category> = when (type) {
    TxType.EXPENSE -> Categories.expenses
    TxType.INCOME -> Categories.incomes
    TxType.SAVING_IN -> Categories.savings.filterNot { it.key == "saving_out" }
    TxType.SAVING_OUT -> Categories.savings.filter { it.key == "saving_out" }
}
