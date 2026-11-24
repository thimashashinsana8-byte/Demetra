package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.model.FlowerSlot
import com.example.model.GardenTheme
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

private val PETAL_X_5 = FloatArray(5) { p -> cos(Math.toRadians((p * 72.0) - 90.0)).toFloat() }
private val PETAL_Y_5 = FloatArray(5) { p -> sin(Math.toRadians((p * 72.0) - 90.0)).toFloat() }
private val PETAL_X_6 = FloatArray(6) { p -> cos(Math.toRadians((p * 60.0) - 90.0)).toFloat() }
private val PETAL_Y_6 = FloatArray(6) { p -> sin(Math.toRadians((p * 60.0) - 90.0)).toFloat() }

private val STEM_STROKE = Stroke(width = 5f, cap = StrokeCap.Round)

/**
 * Illustrated, interactive 6-slot Harvest Garden botanical canvas.
 * Supports animated bloom growth, theme palettes, and tap-to-inspect flowers.
 */
@Composable
fun HarvestGardenCanvas(
    theme: GardenTheme,
    slots: List<FlowerSlot>,
    selectedSlotIndex: Int?,
    onSlotSelected: (FlowerSlot) -> Unit,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val swayAngleState: State<Float>? = if (!reducedMotion) {
        val infiniteTransition = rememberInfiniteTransition(label = "garden_breeze")
        infiniteTransition.animateFloat(
            initialValue = -2.5f,
            targetValue = 2.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "sway"
        )
    } else null

    val skyTop = remember(theme) {
        when (theme) {
            GardenTheme.SPRING_BLOSSOM -> Color(0xFFE9F3EA)
            GardenTheme.GOLDEN_HARVEST -> Color(0xFFFAF0D7)
            GardenTheme.WILDFLOWER -> Color(0xFFE7EFF3)
            GardenTheme.SUNFLOWER -> Color(0xFFFDF2D2)
            GardenTheme.ORCHARD -> Color(0xFFEAF3E6)
        }
    }
    val hillBack = remember(theme) {
        when (theme) {
            GardenTheme.GOLDEN_HARVEST, GardenTheme.SUNFLOWER -> Color(0xFFA2BA78)
            else -> SageGreen.copy(alpha = 0.65f)
        }
    }
    val hillFront = remember(theme) {
        when (theme) {
            GardenTheme.GOLDEN_HARVEST -> Color(0xFF6B9355)
            else -> ForestGreen.copy(alpha = 0.88f)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.5.dp, CreamCardBorder),
        shadowElevation = 3.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "Harvest Garden with 6 botanical flower slots" }
                    .drawWithCache {
                        val w = size.width
                        val h = size.height
                        val skyBrush = Brush.verticalGradient(colors = listOf(skyTop, WarmCream))
                        val sunBrush = Brush.radialGradient(
                            colors = listOf(HarvestGold.copy(alpha = 0.35f), Color.Transparent),
                            center = Offset(w * 0.82f, h * 0.18f),
                            radius = w * 0.30f
                        )
                        val soilBrush = Brush.verticalGradient(colors = listOf(EarthSoilLight, EarthSoil))
                        val backHill = Path().apply {
                            moveTo(0f, h * 0.66f)
                            quadraticTo(w * 0.32f, h * 0.48f, w * 0.65f, h * 0.60f)
                            quadraticTo(w * 0.86f, h * 0.66f, w, h * 0.56f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        val frontHill = Path().apply {
                            moveTo(0f, h * 0.72f)
                            quadraticTo(w * 0.50f, h * 0.60f, w, h * 0.72f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        onDrawBehind {
                            drawRect(brush = skyBrush)
                            drawCircle(
                                brush = sunBrush,
                                radius = w * 0.30f,
                                center = Offset(w * 0.82f, h * 0.18f)
                            )
                            drawCircle(
                                color = HarvestGold.copy(alpha = 0.85f),
                                radius = w * 0.055f,
                                center = Offset(w * 0.82f, h * 0.18f)
                            )
                            drawPath(backHill, color = hillBack)
                            drawPath(frontHill, color = hillFront)
                            drawRoundRect(
                                brush = soilBrush,
                                topLeft = Offset(w * 0.06f, h * 0.73f),
                                size = Size(w * 0.88f, h * 0.20f),
                                cornerRadius = CornerRadius(24f, 24f)
                            )
                        }
                    }
            )

            // 6 interactive flower slot columns laid cleanly across the garden bed
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                slots.forEachIndexed { idx, slot ->
                    key(slot.slotIndex) {
                        val isSelected = selectedSlotIndex == slot.slotIndex
                        val heightShift = if (idx % 2 == 0) 14.dp else 4.dp
                        val bloomScaleState = animateFloatAsState(
                            targetValue = if (slot.isBloomed) 1f else 0.72f,
                            animationSpec = if (reducedMotion) {
                                snap()
                            } else {
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            },
                            label = "bloom_$idx"
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = heightShift)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSlotSelected(slot) }
                                .testTag("garden_slot_${slot.slotIndex}"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .width(52.dp)
                                    .height(132.dp)
                                    .graphicsLayer {
                                        transformOrigin = TransformOrigin(0.5f, 0.92f)
                                        val scale = bloomScaleState.value
                                        scaleX = scale
                                        scaleY = scale
                                        if (slot.isBloomed) {
                                            rotationZ = swayAngleState?.value ?: 0f
                                        }
                                    }
                                    .drawWithCache {
                                        val cx = size.width / 2f
                                        val baseY = size.height * 0.92f
                                        val stemHeight = size.height * (0.48f + (slot.slotIndex % 3) * 0.06f)
                                        val flowerTopY = baseY - stemHeight
                                        val stemPath = Path().apply {
                                            moveTo(cx, baseY)
                                            quadraticTo(
                                                cx + if (slot.slotIndex % 2 == 0) 8f else -8f,
                                                (baseY + flowerTopY) / 2f,
                                                cx,
                                                flowerTopY
                                            )
                                        }
                                        val leafY = baseY - stemHeight * 0.45f
                                        val leftLeaf = Path().apply {
                                            moveTo(cx, leafY)
                                            quadraticTo(cx - 22f, leafY - 14f, cx - 18f, leafY - 2f)
                                            quadraticTo(cx - 10f, leafY + 6f, cx, leafY)
                                        }
                                        val rightLeaf = Path().apply {
                                            moveTo(cx, leafY - 10f)
                                            quadraticTo(cx + 22f, leafY - 24f, cx + 18f, leafY - 10f)
                                            quadraticTo(cx + 10f, leafY - 2f, cx, leafY - 10f)
                                        }
                                        val petalColor = when ((slot.slotIndex + theme.ordinal) % 5) {
                                            0 -> Color(0xFFF3B5C1)
                                            1 -> Color(0xFFFFFFFF)
                                            2 -> HarvestGold
                                            3 -> Color(0xFF87B3D6)
                                            else -> Color(0xFFF09D6C)
                                        }
                                        val useSixPetals = slot.slotIndex % 2 == 0
                                        val petalXs = if (useSixPetals) PETAL_X_6 else PETAL_X_5
                                        val petalYs = if (useSixPetals) PETAL_Y_6 else PETAL_Y_5
                                        val petalRadius = 15f
                                        val petalOrbit = 14f

                                        onDrawBehind {
                                            drawGardenSlotFlower(
                                                cx = cx,
                                                baseY = baseY,
                                                flowerTopY = flowerTopY,
                                                isBloomed = slot.isBloomed,
                                                isSelected = isSelected,
                                                stemPath = stemPath,
                                                leftLeaf = leftLeaf,
                                                rightLeaf = rightLeaf,
                                                petalColor = petalColor,
                                                petalXs = petalXs,
                                                petalYs = petalYs,
                                                petalRadius = petalRadius,
                                                petalOrbit = petalOrbit
                                            )
                                        }
                                    }
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = when {
                                    isSelected -> HarvestGold
                                    slot.isBloomed -> SoftWhite.copy(alpha = 0.92f)
                                    else -> WarmCream.copy(alpha = 0.75f)
                                }
                            ) {
                                Text(
                                    text = "${slot.slotIndex + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (slot.isBloomed || isSelected) DarkText else MutedText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawGardenSlotFlower(
    cx: Float,
    baseY: Float,
    flowerTopY: Float,
    isBloomed: Boolean,
    isSelected: Boolean,
    stemPath: Path,
    leftLeaf: Path,
    rightLeaf: Path,
    petalColor: Color,
    petalXs: FloatArray,
    petalYs: FloatArray,
    petalRadius: Float,
    petalOrbit: Float
) {

    // Selection halo
    if (isSelected) {
        drawCircle(
            color = HarvestGold.copy(alpha = 0.28f),
            radius = size.width * 0.45f,
            center = Offset(cx, size.height * 0.44f)
        )
    }

    if (!isBloomed) {
        drawOval(
            color = Color(0xFF4F3B2B),
            topLeft = Offset(cx - 16f, baseY - 10f),
            size = Size(32f, 12f)
        )
        drawLine(
            color = SageGreen.copy(alpha = 0.75f),
            start = Offset(cx, baseY - 6f),
            end = Offset(cx, baseY - 26f),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        drawOval(
            color = SageGreen.copy(alpha = 0.8f),
            topLeft = Offset(cx - 10f, baseY - 34f),
            size = Size(12f, 8f)
        )
        return
    }

    drawPath(
        path = stemPath,
        color = Color(0xFF3E7B4E),
        style = STEM_STROKE
    )
    drawPath(leftLeaf, color = SageGreen)
    drawPath(rightLeaf, color = ForestGreen)

    for (i in petalXs.indices) {
        drawCircle(
            color = petalColor,
            radius = petalRadius,
            center = Offset(cx + petalXs[i] * petalOrbit, flowerTopY + petalYs[i] * petalOrbit)
        )
    }

    drawCircle(
        color = HarvestGold,
        radius = 10f,
        center = Offset(cx, flowerTopY)
    )
    drawCircle(
        color = DeepGreen.copy(alpha = 0.35f),
        radius = 4f,
        center = Offset(cx, flowerTopY)
    )
}
