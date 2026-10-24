package com.salaryflow.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salaryflow.app.core.Money
import com.salaryflow.app.ui.theme.BrandColors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class CelebrationCoin(
    val xFraction: Float,
    val delay: Float,
    val size: Float,
    val sway: Float,
)

private data class ConfettiPiece(
    val xFraction: Float,
    val delay: Float,
    val size: Float,
    val spin: Float,
    val colorIndex: Int,
)

private fun smoothstep(value: Float): Float {
    val safe = value.coerceIn(0f, 1f)
    return safe * safe * (3f - 2f * safe)
}

/**
 * 发薪日落袋动画：虚拟工资从上方落下，穿过「工资单」进入攒钱罐，
 * 金额从 0 滚到本次结算金额。
 */
@Composable
fun PaydayCelebrationOverlay(
    amountCents: Long,
    periodLabel: String?,
    onDismiss: () -> Unit,
    onOpenLedger: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()
    val coins = remember {
        val random = Random(20261023)
        List(28) { index ->
            CelebrationCoin(
                xFraction = 0.04f + random.nextFloat() * 0.92f,
                delay = random.nextFloat() * 0.52f + (index % 5) * 0.012f,
                size = 20f + random.nextFloat() * 17f,
                sway = 12f + random.nextFloat() * 42f,
            )
        }
    }
    val confetti = remember {
        val random = Random(713)
        List(38) {
            ConfettiPiece(
                xFraction = random.nextFloat(),
                delay = random.nextFloat() * 0.38f,
                size = 8f + random.nextFloat() * 12f,
                spin = random.nextFloat() * 520f - 260f,
                colorIndex = random.nextInt(4),
            )
        }
    }

    LaunchedEffect(amountCents) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(3200, easing = FastOutSlowInEasing))
    }

    val amountProgress = smoothstep(((progress.value - 0.22f) / 0.78f).coerceIn(0f, 1f))
    val shownAmount = (amountCents * amountProgress).toLong()
    val amountText = "¥${Money.centsToYuan(shownAmount)}"
    val amountSize = when {
        amountText.length >= 20 -> 30.sp
        amountText.length >= 16 -> 38.sp
        amountText.length >= 13 -> 46.sp
        else -> 56.sp
    }
    val confettiColors = listOf(
        BrandColors.heroAccent,
        Color(0xFFF2B33D),
        Color(0xFFFF8A65),
        Color(0xFFFFF1C7),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF05241E), Color(0xFF0B5F50), Color(0xFF041A17)),
                ),
            )
            .padding(24.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val jarLeft = size.width * 0.12f
            val jarWidth = size.width - jarLeft * 2f
            val jarTop = size.height * 0.455f
            val jarHeight = size.height * 0.275f
            val bodyTop = jarTop + jarHeight * 0.075f
            val jarBottom = jarTop + jarHeight
            val bodyCorner = jarWidth * 0.105f
            val neckWidth = jarWidth * 0.34f
            val neckLeft = (size.width - neckWidth) / 2f
            val rimHeight = jarHeight * 0.055f
            val fillProgress = smoothstep(
                ((progress.value - 0.12f) / 0.88f).coerceIn(0f, 1f),
            )
            val liquidRatio = 0.7f * fillProgress
            val liquidTop = jarBottom - jarHeight * liquidRatio

            val bodyPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = jarLeft,
                        top = bodyTop,
                        right = jarLeft + jarWidth,
                        bottom = jarBottom,
                        cornerRadius = CornerRadius(bodyCorner, bodyCorner),
                    ),
                )
            }

            // 罐底阴影，让罐子真正“落”在背景上。
            drawOval(
                color = Color.Black.copy(alpha = 0.24f),
                topLeft = Offset(jarLeft + jarWidth * 0.08f, jarBottom - 8f),
                size = Size(jarWidth * 0.84f, 38f),
            )

            // 玻璃罐体。
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.24f),
                        Color.White.copy(alpha = 0.07f),
                        Color(0xFF7CD6C7).copy(alpha = 0.08f),
                    ),
                    startY = bodyTop,
                    endY = jarBottom,
                ),
                topLeft = Offset(jarLeft, bodyTop),
                size = Size(jarWidth, jarHeight - (bodyTop - jarTop)),
                cornerRadius = CornerRadius(bodyCorner, bodyCorner),
            )

            // 罐中金币液体：最多填充七成，保留玻璃高光和“还没攒满”的期待感。
            if (liquidRatio > 0f) {
                clipPath(bodyPath) {
                    val waveHeight = jarWidth * 0.035f
                    val liquidPath = Path().apply {
                        moveTo(jarLeft - bodyCorner, liquidTop + waveHeight)
                        quadraticTo(
                            jarLeft + jarWidth * 0.22f,
                            liquidTop - waveHeight,
                            jarLeft + jarWidth * 0.48f,
                            liquidTop + waveHeight * 0.25f,
                        )
                        quadraticTo(
                            jarLeft + jarWidth * 0.73f,
                            liquidTop + waveHeight * 1.35f,
                            jarLeft + jarWidth + bodyCorner,
                            liquidTop - waveHeight * 0.3f,
                        )
                        lineTo(jarLeft + jarWidth + bodyCorner, jarBottom + bodyCorner)
                        lineTo(jarLeft - bodyCorner, jarBottom + bodyCorner)
                        close()
                    }
                    drawPath(
                        path = liquidPath,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFFFE88A), Color(0xFFF1B93F), Color(0xFFD88A16)),
                            startY = liquidTop,
                            endY = jarBottom,
                        ),
                    )

                    repeat(8) { index ->
                        val bubbleX = jarLeft + jarWidth * (0.15f + (index % 4) * 0.22f)
                        val bubbleY = liquidTop +
                            (jarBottom - liquidTop) * (0.24f + (index % 3) * 0.2f)
                        drawCircle(
                            color = Color.White.copy(alpha = 0.2f),
                            radius = 4f + (index % 3) * 3f,
                            center = Offset(bubbleX, bubbleY),
                        )
                    }
                }
            }

            // 罐口、金属圈与高光。
            drawRoundRect(
                color = Color(0xFFB8D8D2).copy(alpha = 0.5f),
                topLeft = Offset(neckLeft, jarTop + 4f),
                size = Size(neckWidth, bodyTop - jarTop + 12f),
                cornerRadius = CornerRadius(neckWidth * 0.18f, neckWidth * 0.18f),
            )
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFFE8F0EE), Color(0xFFFDFEFE), Color(0xFFB5C7C4)),
                ),
                topLeft = Offset(neckLeft - jarWidth * 0.018f, jarTop - rimHeight),
                size = Size(neckWidth + jarWidth * 0.036f, rimHeight),
                cornerRadius = CornerRadius(rimHeight / 2f, rimHeight / 2f),
            )
            drawPath(
                path = bodyPath,
                color = Color.White.copy(alpha = 0.72f),
                style = Stroke(width = 4.5f),
            )
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(jarLeft + jarWidth * 0.16f, bodyTop + jarHeight * 0.22f),
                end = Offset(jarLeft + jarWidth * 0.11f, bodyTop + jarHeight * 0.67f),
                strokeWidth = 9f,
                cap = StrokeCap.Round,
            )

            // 罐身标签，让攒钱罐不只是一个矩形。
            val labelLeft = jarLeft + jarWidth * 0.24f
            val labelTop = bodyTop + jarHeight * 0.4f
            val labelWidth = jarWidth * 0.52f
            val labelHeight = jarHeight * 0.18f
            drawRoundRect(
                color = Color(0xFFFFF4D4),
                topLeft = Offset(labelLeft, labelTop),
                size = Size(labelWidth, labelHeight),
                cornerRadius = CornerRadius(13f, 13f),
            )
            drawRoundRect(
                color = Color(0xFF9B6816).copy(alpha = 0.28f),
                topLeft = Offset(labelLeft, labelTop),
                size = Size(labelWidth, labelHeight),
                cornerRadius = CornerRadius(13f, 13f),
                style = Stroke(width = 2f),
            )
            drawText(
                textMeasurer = textMeasurer,
                text = "薪流存款",
                topLeft = Offset(labelLeft + labelWidth * 0.2f, labelTop + labelHeight * 0.22f),
                style = TextStyle(
                    color = Color(0xFF7B4A00),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                ),
            )

            // 金币从屏幕上方落入罐口。
            coins.forEach { coin ->
                val start = 0.04f + coin.delay * 0.48f
                val local = ((progress.value - start) / (1f - start)).coerceIn(0f, 1f)
                if (local <= 0f) return@forEach
                val fall = smoothstep(local)
                val fade = when {
                    local < 0.84f -> 1f
                    else -> (1f - (local - 0.84f) / 0.16f).coerceIn(0f, 1f)
                }
                val x = size.width * coin.xFraction +
                    sin(local * PI.toFloat() * 2.1f) * coin.sway * (1f - local * 0.55f)
                val y = -coin.size + (liquidTop - (-coin.size)) * fall
                val radius = coin.size * (0.72f + 0.28f * sin(local * PI.toFloat()))

                drawCircle(
                    color = Color(0xFFF4C654).copy(alpha = fade),
                    radius = radius,
                    center = Offset(x, y),
                )
                drawCircle(
                    color = Color(0xFF8A5700).copy(alpha = 0.9f * fade),
                    radius = radius * 0.72f,
                    center = Offset(x, y),
                    style = Stroke(width = 3f),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.42f * fade),
                    radius = radius * 0.18f,
                    center = Offset(x - radius * 0.3f, y - radius * 0.32f),
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = "¥",
                    topLeft = Offset(x - radius * 0.46f, y - radius * 0.72f),
                    style = TextStyle(
                        color = Color(0xFF704400).copy(alpha = fade),
                        fontSize = (radius * 1.15f).sp,
                        fontWeight = FontWeight.Black,
                    ),
                )
            }

            // 彩带只落在罐口上方，避免遮住数字与按钮。
            confetti.forEach { piece ->
                val local = ((progress.value - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
                if (local <= 0f) return@forEach
                val x = size.width * piece.xFraction
                val y = -30f + size.height * 0.58f * local
                if (y > jarTop - 24f) return@forEach
                rotate(degrees = piece.spin * local, pivot = Offset(x, y)) {
                    drawRoundRect(
                        color = confettiColors[piece.colorIndex].copy(alpha = 0.88f),
                        topLeft = Offset(x, y),
                        size = Size(piece.size, piece.size * 1.7f),
                        cornerRadius = CornerRadius(piece.size * 0.3f, piece.size * 0.3f),
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))
            Text(
                "工资已经到账",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                periodLabel?.let { "周期 $it 的虚拟工资" } ?: "虚拟工资正在落袋",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.78f),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                amountText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = amountSize,
                ),
                color = Color(0xFFFFE08A),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (amountProgress >= 1f) "已经落进真实存款" else "正在变成存款里的真钱",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "每一秒的工作，都变成了看得见的存款。",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.82f),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF2B33D),
                    contentColor = Color(0xFF3D2A00),
                ),
            ) {
                Text("收下这笔钱", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onOpenLedger) {
                Text("去账本看看", color = Color.White.copy(alpha = 0.9f))
            }
        }
    }
}
