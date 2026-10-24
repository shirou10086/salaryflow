package com.salaryflow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.salaryflow.app.core.Money
import com.salaryflow.app.ui.theme.BrandColors

/**
 * 用用户看到的真实余额覆盖账本推算值。
 *
 * 校准不会改写历史流水，而是写入一笔明确的余额调整，用户之后可以在账本中查看或删除。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceCalibrationSheet(
    currentCents: Long,
    hidden: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember(currentCents) { mutableStateOf("") }
    val actualCents = Money.yuanToCents(amountText)
    val deltaCents = actualCents?.minus(currentCents)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(BrandColors.savings.copy(alpha = 0.08f))
                    .padding(18.dp),
            ) {
                Column {
                    Text(
                        "账本推算余额",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        if (hidden) "¥••••" else "¥${Money.centsToYuan(currentCents)}",
                        style = MaterialTheme.typography.displayMedium,
                        color = BrandColors.savings,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "工资收入增加，支出减少；可能因漏记或账户转账产生误差。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("输入你现在的实际存款", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "可以填银行卡、支付宝、微信等账户的合计，只保存在本机。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    amountText = input.filter { it.isDigit() || it == '.' }.take(14)
                },
                label = { Text("实际余额") },
                prefix = { Text("¥ ") },
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.displayMedium,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            if (deltaCents != null) {
                Spacer(Modifier.height(12.dp))
                val direction = when {
                    deltaCents > 0 -> "将向上校准"
                    deltaCents < 0 -> "将向下校准"
                    else -> "无需调整"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        direction,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (deltaCents != 0L) {
                        Text(
                            (if (deltaCents > 0) "+" else "−") +
                                "¥${Money.centsToYuan(kotlin.math.abs(deltaCents))}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (deltaCents > 0) BrandColors.savings else BrandColors.expense,
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    actualCents?.let {
                        onSubmit(it)
                        onDismiss()
                    }
                },
                enabled = actualCents != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandColors.savings,
                    contentColor = Color.White,
                ),
            ) {
                Text("保存实际余额", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "保存后会在账本中生成一笔「余额校准」，不会删除原来的流水。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
