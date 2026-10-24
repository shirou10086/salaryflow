package com.salaryflow.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salaryflow.app.R
import com.salaryflow.app.core.Money
import com.salaryflow.app.core.TimeText
import com.salaryflow.app.ui.theme.BrandColors
import java.time.LocalTime

data class OnboardingResult(
    val monthlySalaryCents: Long,
    val paidDaysPerMonth: Double,
    val payday: Int,
    val workStartMinute: Int,
    val workEndMinute: Int,
    val actualSavingsCents: Long,
    val enableLiveNotification: Boolean,
    val enableBalanceReminder: Boolean,
)

private enum class OnboardingTimeField {
    WORK_START,
    WORK_END,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: (OnboardingResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    var salaryText by remember { mutableStateOf("") }
    var paydayText by remember { mutableStateOf("15") }
    var workStart by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var workEnd by remember { mutableStateOf(LocalTime.of(18, 0)) }
    var savingsText by remember { mutableStateOf("") }
    var editingTime by remember { mutableStateOf<OnboardingTimeField?>(null) }

    val salaryCents = Money.yuanToCents(salaryText)
    val savingsCents = savingsText
        .takeIf { it.isNotBlank() }
        ?.let(Money::yuanToCents)
    val payday = paydayText.toIntOrNull()
    val salaryValid = salaryCents != null && salaryCents > 0
    val savingsValid = savingsText.isBlank() || savingsCents != null
    val paydayValid = payday != null && payday in 1..31
    val timeValid = workEnd > workStart
    val canFinish = salaryValid && savingsValid && paydayValid && timeValid
    val paydaySuffix = stringResource(R.string.onboarding_payday_suffix)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = BrandColors.savings.copy(alpha = 0.06f),
                radius = size.minDimension * 0.7f,
                center = Offset(size.width * 1.04f, size.height * 0.02f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BrandMark()
                Column {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                    Text(
                        "SalaryFlow",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(30.dp))
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = BrandColors.savings.copy(alpha = 0.1f),
            ) {
                Text(
                    stringResource(R.string.onboarding_fast_setup),
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandColors.savings,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(22.dp))
            OnboardingCard {
                OnboardingFieldTitle(
                    icon = Icons.Rounded.Payments,
                    title = stringResource(R.string.onboarding_net_salary),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = salaryText,
                    onValueChange = { input ->
                        salaryText = input.filter { it.isDigit() || it == '.' }.take(12)
                    },
                    prefix = { Text("¥ ") },
                    placeholder = { Text(stringResource(R.string.onboarding_salary_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.displaySmall,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = paydayText,
                    onValueChange = { input ->
                        paydayText = input.filter(Char::isDigit).take(2)
                    },
                    label = { Text(stringResource(R.string.onboarding_payday)) },
                    suffix = { Text(paydaySuffix) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(14.dp))
            OnboardingCard {
                OnboardingFieldTitle(
                    icon = Icons.Rounded.Schedule,
                    title = stringResource(R.string.onboarding_work_hours),
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TimeSettingButton(
                        label = stringResource(R.string.onboarding_work_start),
                        time = workStart,
                        modifier = Modifier.weight(1f),
                        onClick = { editingTime = OnboardingTimeField.WORK_START },
                    )
                    TimeSettingButton(
                        label = stringResource(R.string.onboarding_work_end),
                        time = workEnd,
                        modifier = Modifier.weight(1f),
                        onClick = { editingTime = OnboardingTimeField.WORK_END },
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (timeValid) {
                        stringResource(R.string.onboarding_schedule_hint)
                    } else {
                        stringResource(R.string.onboarding_schedule_error)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (timeValid) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }

            Spacer(Modifier.height(14.dp))
            OnboardingCard {
                OnboardingFieldTitle(
                    icon = Icons.Rounded.Savings,
                    title = stringResource(R.string.current_savings),
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    stringResource(R.string.onboarding_savings_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = savingsText,
                    onValueChange = { input ->
                        savingsText = input.filter { it.isDigit() || it == '.' }.take(14)
                    },
                    prefix = { Text("¥ ") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.headlineSmall,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    val cents = salaryCents ?: return@Button
                    val day = payday ?: return@Button
                    onComplete(
                        OnboardingResult(
                            monthlySalaryCents = cents,
                            paidDaysPerMonth = 21.75,
                            payday = day,
                            workStartMinute = workStart.hour * 60 + workStart.minute,
                            workEndMinute = workEnd.hour * 60 + workEnd.minute,
                            actualSavingsCents = savingsCents ?: 0L,
                            enableLiveNotification = true,
                            enableBalanceReminder = true,
                        ),
                    )
                },
                enabled = canFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandColors.savings,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    stringResource(R.string.onboarding_finish),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.size(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.onboarding_finish_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(18.dp))
        }
    }

    editingTime?.let { field ->
        OnboardingTimeDialog(
            title = stringResource(
                if (field == OnboardingTimeField.WORK_START) {
                    R.string.onboarding_set_work_start
                } else {
                    R.string.onboarding_set_work_end
                },
            ),
            initial = if (field == OnboardingTimeField.WORK_START) workStart else workEnd,
            onDismiss = { editingTime = null },
            onConfirm = { time ->
                if (field == OnboardingTimeField.WORK_START) {
                    workStart = time
                } else {
                    workEnd = time
                }
                editingTime = null
            },
        )
    }
}

@Composable
private fun OnboardingCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(18.dp)) { content() }
    }
}

@Composable
private fun OnboardingFieldTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(11.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(34.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun TimeSettingButton(
    label: String,
    time: LocalTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(68.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                TimeText.clock(time),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingTimeDialog(
    title: String,
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun BrandMark() {
    Canvas(Modifier.size(48.dp)) {
        drawCircle(
            color = BrandColors.savings.copy(alpha = 0.12f),
            radius = size.minDimension / 2f,
        )
        val jar = Path().apply {
            moveTo(size.width * 0.28f, size.height * 0.35f)
            lineTo(size.width * 0.72f, size.height * 0.35f)
            lineTo(size.width * 0.67f, size.height * 0.78f)
            lineTo(size.width * 0.33f, size.height * 0.78f)
            close()
        }
        drawPath(jar, color = BrandColors.savings.copy(alpha = 0.16f))
        drawPath(
            jar,
            color = BrandColors.savings,
            style = Stroke(width = 3f, cap = StrokeCap.Round),
        )
        drawRoundRect(
            color = BrandColors.savings,
            topLeft = Offset(size.width * 0.26f, size.height * 0.27f),
            size = Size(size.width * 0.48f, size.height * 0.09f),
            cornerRadius = CornerRadius(size.height * 0.045f),
        )
        drawCircle(
            color = BrandColors.heroAccent,
            radius = size.minDimension * 0.055f,
            center = Offset(size.width * 0.5f, size.height * 0.57f),
        )
    }
}
