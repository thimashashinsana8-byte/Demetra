package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

enum class PipMood {
    HAPPY, THINKING, CELEBRATING
}

/**
 * Pip: Demetra's consistent botanical green bird mascot with a small golden wheat crest.
 * Caches all vector Paths via drawWithCache and animates strictly via GPU graphicsLayer translationY.
 */
@Composable
fun PipMascot(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    mood: PipMood = PipMood.HAPPY,
    reducedMotion: Boolean = false,
    animate: Boolean = true
) {
    val bobOffsetState: State<Float>? = if (animate && !reducedMotion) {
        val infiniteTransition = rememberInfiniteTransition(label = "pip_idle")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pip_bob"
        )
    } else null

    Spacer(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                translationY = (bobOffsetState?.value ?: 0f) * (size.toPx() / 100f)
            }
            .semantics { contentDescription = "Pip, Demetra's friendly green bird guide" }
            .drawWithCache {
                val w = this.size.width
                val h = this.size.height

                val tailPath = Path().apply {
                    moveTo(w * 0.24f, h * 0.60f)
                    quadraticTo(w * 0.07f, h * 0.50f, w * 0.10f, h * 0.64f)
                    quadraticTo(w * 0.15f, h * 0.72f, w * 0.28f, h * 0.68f)
                    close()
                }
                val leaf1 = Path().apply {
                    moveTo(w * 0.49f, h * 0.11f)
                    quadraticTo(w * 0.38f, h * 0.06f, w * 0.43f, h * 0.03f)
                    quadraticTo(w * 0.50f, h * 0.05f, w * 0.49f, h * 0.11f)
                }
                val leaf2 = Path().apply {
                    moveTo(w * 0.48f, h * 0.09f)
                    quadraticTo(w * 0.58f, h * 0.03f, w * 0.55f, h * 0.01f)
                    quadraticTo(w * 0.49f, h * 0.04f, w * 0.48f, h * 0.09f)
                }
                val wingPath = Path().apply {
                    moveTo(w * 0.26f, h * 0.44f)
                    quadraticTo(w * 0.48f, h * 0.40f, w * 0.50f, h * 0.62f)
                    quadraticTo(w * 0.32f, h * 0.68f, w * 0.24f, h * 0.52f)
                    close()
                }
                val beakPath = Path().apply {
                    moveTo(w * 0.74f, h * 0.31f)
                    lineTo(w * 0.86f, h * 0.35f)
                    lineTo(w * 0.74f, h * 0.40f)
                    close()
                }
                val shadowTopLeft = Offset(w * 0.25f, h * 0.84f)
                val shadowSize = Size(w * 0.50f, h * 0.09f)
                val bodyTopLeft = Offset(w * 0.20f, h * 0.30f)
                val bodySize = Size(w * 0.56f, h * 0.50f)
                val bellyTopLeft = Offset(w * 0.42f, h * 0.48f)
                val bellySize = Size(w * 0.30f, h * 0.28f)
                val headCenter = Offset(w * 0.56f, h * 0.34f)
                val crestPivot = Offset(w * 0.52f, h * 0.16f)
                val crestStart = Offset(w * 0.52f, h * 0.18f)
                val crestEnd = Offset(w * 0.46f, h * 0.05f)
                val eyeCenter = Offset(w * 0.63f, h * 0.31f)
                val eyeArcTopLeft = Offset(eyeCenter.x - w * 0.04f, eyeCenter.y - w * 0.03f)
                val eyeArcSize = Size(w * 0.08f, w * 0.06f)
                val eyeArcStroke = Stroke(width = w * 0.028f, cap = StrokeCap.Round)
                val eyeHighlight = Offset(eyeCenter.x - w * 0.01f, eyeCenter.y - w * 0.01f)
                val cheekCenter = Offset(w * 0.58f, h * 0.39f)
                val wingVeinStart = Offset(w * 0.30f, h * 0.50f)
                val wingVeinEnd = Offset(w * 0.44f, h * 0.57f)
                val foot1Start = Offset(w * 0.42f, h * 0.78f)
                val foot1End = Offset(w * 0.42f, h * 0.86f)
                val foot2Start = Offset(w * 0.56f, h * 0.78f)
                val foot2End = Offset(w * 0.56f, h * 0.86f)

                onDrawBehind {
                    drawOval(
                        color = ForestGreen.copy(alpha = 0.12f),
                        topLeft = shadowTopLeft,
                        size = shadowSize
                    )
                    drawPath(tailPath, color = ForestGreen)
                    drawOval(color = SageGreen, topLeft = bodyTopLeft, size = bodySize)
                    drawOval(color = WarmCream.copy(alpha = 0.88f), topLeft = bellyTopLeft, size = bellySize)
                    drawCircle(color = SageGreen, radius = w * 0.21f, center = headCenter)

                    rotate(degrees = -12f, pivot = crestPivot) {
                        drawLine(
                            color = HarvestGold,
                            start = crestStart,
                            end = crestEnd,
                            strokeWidth = w * 0.032f,
                            cap = StrokeCap.Round
                        )
                        drawPath(leaf1, color = HarvestGold)
                        drawPath(leaf2, color = HarvestGold)
                    }

                    drawPath(wingPath, color = ForestGreen)
                    drawLine(
                        color = HarvestGold.copy(alpha = 0.85f),
                        start = wingVeinStart,
                        end = wingVeinEnd,
                        strokeWidth = w * 0.025f,
                        cap = StrokeCap.Round
                    )

                    if (mood == PipMood.CELEBRATING) {
                        drawArc(
                            color = DarkText,
                            startAngle = 190f,
                            sweepAngle = 160f,
                            useCenter = false,
                            topLeft = eyeArcTopLeft,
                            size = eyeArcSize,
                            style = eyeArcStroke
                        )
                    } else {
                        drawCircle(color = DarkText, radius = w * 0.035f, center = eyeCenter)
                        drawCircle(color = SoftWhite, radius = w * 0.013f, center = eyeHighlight)
                    }

                    drawCircle(color = HarvestGold.copy(alpha = 0.35f), radius = w * 0.038f, center = cheekCenter)
                    drawPath(beakPath, color = HarvestGold)

                    drawLine(
                        color = HarvestGold,
                        start = foot1Start,
                        end = foot1End,
                        strokeWidth = w * 0.026f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = HarvestGold,
                        start = foot2Start,
                        end = foot2End,
                        strokeWidth = w * 0.026f,
                        cap = StrokeCap.Round
                    )
                }
            }
    )
}

@Composable
fun PipSpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    actionChipLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SoftWhite,
        border = BorderStroke(1.dp, CreamCardBorder),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(SageTint),
                contentAlignment = Alignment.Center
            ) {
                PipMascot(size = 38.dp, reducedMotion = reducedMotion, animate = false)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Pip • Demetra Guide",
                    style = MaterialTheme.typography.labelMedium,
                    color = ForestGreen
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkText
                )
                if (actionChipLabel != null && onActionClick != null) {
                    Surface(
                        onClick = onActionClick,
                        shape = RoundedCornerShape(50),
                        color = GoldTint
                    ) {
                        Text(
                            text = actionChipLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = DeepGreen,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Subtle, non-blocking milestone reaction banner for Pip.
 * Appears quietly with Pip's expression, optional quick jump chip, and close affordance.
 */
@Composable
fun PipMilestoneBanner(
    headline: String,
    message: String,
    isCelebration: Boolean,
    reducedMotion: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp),
        shape = RoundedCornerShape(20.dp),
        color = SoftWhite.copy(alpha = 0.97f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCelebration) HarvestGold.copy(alpha = 0.7f) else CreamCardBorder
        ),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isCelebration) GoldTint else SageTint),
                contentAlignment = Alignment.Center
            ) {
                PipMascot(
                    size = 34.dp,
                    mood = if (isCelebration) PipMood.CELEBRATING else PipMood.HAPPY,
                    reducedMotion = reducedMotion,
                    animate = !reducedMotion
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Pip • $headline",
                    style = MaterialTheme.typography.labelSmall,
                    color = ForestGreen
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkText
                )
            }
            if (actionLabel != null && onActionClick != null) {
                Surface(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(50),
                    color = SageTint,
                    border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = ForestGreen,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Dismiss milestone note",
                    tint = MutedText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
