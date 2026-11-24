package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.DonationSuccessState
import com.example.ui.theme.CreamCardBorder
import com.example.ui.theme.DarkText
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GoldTint
import com.example.ui.theme.HarvestGold
import com.example.ui.theme.MutedText
import com.example.ui.theme.SageGreen
import com.example.ui.theme.SageTint
import com.example.ui.theme.SoftWhite
import com.example.viewmodel.DemetraViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val BURST_ANGLES_RAD = FloatArray(6) { i -> ((i * 60.0) * PI / 180.0).toFloat() }
private val BURST_COS = FloatArray(6) { i -> cos(BURST_ANGLES_RAD[i]) }
private val BURST_SIN = FloatArray(6) { i -> sin(BURST_ANGLES_RAD[i]) }

@Immutable
data class PipToastPayload(
    val key: String,
    val headline: String,
    val message: String,
    val isCelebration: Boolean,
    val actionLabel: String? = null,
    val actionRoute: String? = null,
    val fromDonationSuccess: Boolean = false
)

/**
 * Stateful Pip assistant observer that listens to [DemetraViewModel.donationSuccessState]
 * and [DemetraViewModel.latestMilestone], triggering a brief, non-intrusive celebratory
 * micro-animation and toast without interrupting the user's current flow.
 */
@Composable
fun PipDonationAssistant(
    viewModel: DemetraViewModel,
    reducedMotion: Boolean,
    onNavigateGarden: () -> Unit,
    modifier: Modifier = Modifier,
    autoDismissMillis: Long = 4200L
) {
    val donationSuccess by viewModel.donationSuccessState.collectAsStateWithLifecycle()
    val latestMilestone by viewModel.latestMilestone.collectAsStateWithLifecycle()
    val latestFlowerEvent by viewModel.latestFlowerEvent.collectAsStateWithLifecycle()
    val isPipOpen by viewModel.isPipOpen.collectAsStateWithLifecycle()

    val payload = remember(donationSuccess, latestMilestone) {
        when (val state = donationSuccess) {
            is DonationSuccessState.Published -> PipToastPayload(
                key = "donation_pub_${state.eventId}",
                headline = "Donation Listed",
                message = "Smart Relay matched \"${state.title}\" (${state.matchPercentage}% fit) with ${state.recipientOrgName}.",
                isCelebration = false,
                fromDonationSuccess = true
            )
            is DonationSuccessState.Completed -> {
                if (state.reachedFullBloom) {
                    // Full 6-slot harvest already shows HarvestCompletionCelebrationDialog
                    null
                } else {
                    val bloomSuffix = state.flowerSpecies?.let { " A $it bloomed in your garden." } ?: ""
                    PipToastPayload(
                        key = "donation_done_${state.eventId}",
                        headline = "Donation Completed",
                        message = "Warm thanks! \"${state.title}\" (${state.servingsEstimate} servings) reached safe hands.$bloomSuffix",
                        isCelebration = true,
                        actionLabel = if (state.flowerSpecies != null) "View Garden" else null,
                        actionRoute = if (state.flowerSpecies != null) "harvest_garden" else null,
                        fromDonationSuccess = true
                    )
                }
            }
            DonationSuccessState.Idle -> latestMilestone?.let { milestone ->
                PipToastPayload(
                    key = "milestone_${milestone.id}",
                    headline = milestone.headline,
                    message = milestone.message,
                    isCelebration = milestone.isCelebration,
                    actionLabel = milestone.actionLabel,
                    actionRoute = milestone.actionRoute,
                    fromDonationSuccess = false
                )
            }
        }
    }

    val isVisible = payload != null && latestFlowerEvent == null && !isPipOpen

    LaunchedEffect(payload?.key, isVisible) {
        if (isVisible && payload != null) {
            delay(autoDismissMillis)
            if (payload.fromDonationSuccess) {
                viewModel.clearDonationSuccessState()
            } else {
                viewModel.clearMilestone()
            }
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = if (reducedMotion) fadeIn(snap()) else fadeIn(tween(220)) + slideInVertically(tween(260)) { -it / 2 },
        exit = if (reducedMotion) fadeOut(snap()) else fadeOut(tween(180)) + slideOutVertically(tween(200)) { -it / 2 },
        modifier = modifier.testTag("pip_celebration_toast")
    ) {
        payload?.let { current ->
            PipCelebrationToast(
                payload = current,
                reducedMotion = reducedMotion,
                durationMillis = autoDismissMillis,
                onDismiss = {
                    if (current.fromDonationSuccess) {
                        viewModel.clearDonationSuccessState()
                    } else {
                        viewModel.clearMilestone()
                    }
                },
                onActionClick = current.actionRoute?.let { route ->
                    {
                        if (current.fromDonationSuccess) {
                            viewModel.clearDonationSuccessState()
                        } else {
                            viewModel.clearMilestone()
                        }
                        if (route == "harvest_garden") {
                            onNavigateGarden()
                        }
                    }
                }
            )
        }
    }
}

/**
 * Brief, non-intrusive celebratory toast with a one-shot radial petal sparkle burst
 * and GPU-driven scale bounce around Pip.
 */
@Composable
fun PipCelebrationToast(
    payload: PipToastPayload,
    reducedMotion: Boolean,
    durationMillis: Long,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onActionClick: (() -> Unit)? = null
) {
    val burstProgress = remember(payload.key) { Animatable(0f) }
    val avatarScale = remember(payload.key) { Animatable(if (reducedMotion) 1f else 0.78f) }
    val countdownProgress = remember(payload.key) { Animatable(1f) }

    LaunchedEffect(payload.key, reducedMotion) {
        if (reducedMotion) {
            burstProgress.snapTo(1f)
            avatarScale.snapTo(1f)
            countdownProgress.snapTo(0f)
        } else {
            launch {
                avatarScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            if (payload.isCelebration) {
                launch {
                    burstProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
                    )
                }
            }
            launch {
                countdownProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = durationMillis.toInt(), easing = LinearEasing)
                )
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp),
        shape = RoundedCornerShape(22.dp),
        color = SoftWhite.copy(alpha = 0.97f),
        border = BorderStroke(
            width = 1.2.dp,
            color = if (payload.isCelebration) HarvestGold.copy(alpha = 0.75f) else CreamCardBorder
        ),
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 10.dp, end = 6.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            val s = avatarScale.value
                            scaleX = s
                            scaleY = s
                        }
                        .drawWithCache {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val maxRadius = size.minDimension * 0.56f
                            val ringStroke = Stroke(width = 2.dp.toPx())
                            val dotRadius = 2.2.dp.toPx()
                            onDrawBehind {
                                val t = burstProgress.value
                                if (payload.isCelebration && !reducedMotion && t in 0.01f..0.98f) {
                                    val alpha = (1f - t).coerceIn(0f, 0.7f)
                                    val ringRadius = size.minDimension * 0.34f + (maxRadius - size.minDimension * 0.34f) * t
                                    drawCircle(
                                        color = HarvestGold.copy(alpha = alpha * 0.55f),
                                        radius = ringRadius,
                                        center = center,
                                        style = ringStroke
                                    )
                                    val petalDist = size.minDimension * 0.32f + t * (size.minDimension * 0.24f)
                                    for (i in 0 until 6) {
                                        val px = center.x + BURST_COS[i] * petalDist
                                        val py = center.y + BURST_SIN[i] * petalDist
                                        drawCircle(
                                            color = if (i % 2 == 0) HarvestGold.copy(alpha = alpha) else SageGreen.copy(alpha = alpha),
                                            radius = dotRadius * (1.1f - 0.5f * t),
                                            center = Offset(px, py)
                                        )
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (payload.isCelebration) GoldTint else SageTint),
                        contentAlignment = Alignment.Center
                    ) {
                        PipMascot(
                            size = 34.dp,
                            mood = if (payload.isCelebration) PipMood.CELEBRATING else PipMood.HAPPY,
                            reducedMotion = reducedMotion,
                            animate = false
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Pip • ${payload.headline}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForestGreen
                    )
                    Text(
                        text = payload.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkText
                    )
                }

                if (payload.actionLabel != null && onActionClick != null) {
                    Surface(
                        onClick = onActionClick,
                        shape = RoundedCornerShape(50),
                        color = SageTint,
                        border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("pip_toast_action_chip")
                    ) {
                        Text(
                            text = payload.actionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = ForestGreen,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("pip_toast_dismiss")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss Pip notification",
                        tint = MutedText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (!reducedMotion) {
                val accentColor = remember(payload.isCelebration) {
                    if (payload.isCelebration) HarvestGold.copy(alpha = 0.7f) else SageGreen.copy(alpha = 0.55f)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            scaleX = countdownProgress.value.coerceIn(0f, 1f)
                        }
                        .background(accentColor)
                )
            }
        }
    }
}
