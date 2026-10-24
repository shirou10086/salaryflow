package com.salaryflow.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.salaryflow.app.core.Money
import com.salaryflow.app.data.Categories
import com.salaryflow.app.data.TxType
import com.salaryflow.app.data.TransactionEntity
import com.salaryflow.app.ui.theme.BrandColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(18.dp)) { content() }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun StatusChip(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
fun AnimatedMoneyText(
    cents: Double,
    hidden: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayLarge,
    color: Color = Color.Unspecified,
    prefix: String = "¥",
    animated: Boolean = true,
) {
    val animatedValue by animateFloatAsState(
        targetValue = cents.toFloat(),
        animationSpec = tween(durationMillis = 950, easing = LinearEasing),
        label = "money",
    )
    val shown = if (animated) animatedValue.toDouble() else cents
    Text(
        text = if (hidden) "$prefix••••" else prefix + Money.centsToYuan(shown),
        style = style,
        color = color,
        modifier = modifier,
        maxLines = 1,
    )
}

@Composable
fun StatBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
    sub: String? = null,
    alignEnd: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = valueColor)
        if (sub != null) {
            Text(
                sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun CategoryBadge(
    categoryName: String,
    modifier: Modifier = Modifier,
    size: Int = 42,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(size.dp / 3))
            .background(contentColor.copy(alpha = 0.11f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            categoryName.take(1),
            style = MaterialTheme.typography.titleMedium,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(54.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun PillSelect(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(
                if (selected) Modifier
                else Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(12.dp),
                ),
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) selectedContainerColor else Color.Transparent,
        contentColor = if (selected) selectedContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    hidden: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val category = Categories.find(transaction.category)
    val (amountColor, sign) = when (transaction.type) {
        TxType.EXPENSE -> BrandColors.expense to "−"
        TxType.INCOME -> BrandColors.savings to "+"
        TxType.SAVING_IN -> BrandColors.savings to "→"
        TxType.SAVING_OUT -> MaterialTheme.colorScheme.onSurfaceVariant to "←"
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(category.name, contentColor = amountColor)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(category.name, style = MaterialTheme.typography.titleMedium)
            val detail = buildString {
                if (transaction.note.isNotBlank()) append(transaction.note)
                if (isNotEmpty()) append(" · ")
                append(transaction.occurredAt.asTimeText())
                if (transaction.auto) append(" · 自动")
            }
            Text(
                detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Text(
            if (hidden) "$sign¥••••" else "$sign¥${Money.centsToYuan(transaction.amountCents)}",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
            color = amountColor,
            maxLines = 1,
        )
        if (trailing != null) {
            Spacer(Modifier.size(6.dp))
            trailing()
        }
    }
}

fun Long.asTimeText(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
