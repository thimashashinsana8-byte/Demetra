package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.FlowerGrowthEvent
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private val SPARKLE_BASE_ANGLES_RAD = DoubleArray(10) { i -> Math.toRadians(i * 36.0) }
private val SPARKLE_X_FRAC = FloatArray(10) { i -> 0.12f + (i % 5) * 0.19f }
private val SPARKLE_RADIUS = FloatArray(10) { i -> 4.5f + (i % 3) * 2f }
private const val SPARKLE_ALPHA_BUCKETS = 16
private val SPARKLE_GOLD_PALETTE = Array(SPARKLE_ALPHA_BUCKETS) { idx ->
    HarvestGold.copy(alpha = (idx / (SPARKLE_ALPHA_BUCKETS - 1f)) * 0.55f)
}
private val SPARKLE_SAGE_PALETTE = Array(SPARKLE_ALPHA_BUCKETS) { idx ->
    SageGreen.copy(alpha = (idx / (SPARKLE_ALPHA_BUCKETS - 1f)) * 0.55f)
}

@Composable
fun HarvestGardenScreen(
    role: UserRole,
    gardenState: GardenState,
    communityGrove: CommunityGroveStats,
    impactSummary: ImpactSummary,
    language: AppLanguage,
    reducedMotion: Boolean,
    onSelectTheme: (GardenTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = stringsFor(language)
    var selectedTab by remember { mutableIntStateOf(0) }
    var inspectedSlotIndex by remember(gardenState.activeTheme) {
        mutableIntStateOf((gardenState.slots.lastOrNull { it.isBloomed } ?: gardenState.slots.first()).slotIndex)
    }
    val inspectedSlot = remember(gardenState.slots, inspectedSlotIndex) {
        gardenState.slots.find { it.slotIndex == inspectedSlotIndex } ?: gardenState.slots.first()
    }
    var inspectedArchivedGarden by remember { mutableStateOf<ArchivedGarden?>(null) }

    inspectedArchivedGarden?.let { archived ->
        GardenMemorySheet(
            archivedGarden = archived,
            reducedMotion = reducedMotion,
            onDismiss = { inspectedArchivedGarden = null }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Segmented Switch: My Harvest Garden vs Community Grove
        item(key = "segmented_switch", contentType = "segmented_switch") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SoftWhite,
                border = BorderStroke(1.dp, CreamCardBorder)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    listOf(copy.harvestGardenTitle, copy.communityGroveTitle).forEachIndexed { idx, label ->
                        val active = selectedTab == idx
                        Surface(
                            onClick = { selectedTab = idx },
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) ForestGreen else SoftWhite,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("garden_tab_$idx")
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) SoftWhite else MutedText
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectedTab == 0) {
            item(key = "garden_header", contentType = "garden_header") {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = SoftWhite,
                    border = BorderStroke(1.dp, CreamCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = gardenState.activeTheme.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = DarkText
                                )
                                Text(
                                    text = copy.harvestGardenTagline,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedText
                                )
                            }
                            Surface(shape = RoundedCornerShape(50), color = GoldTint) {
                                Text(
                                    text = "${gardenState.bloomedCount} / 6 Blooms",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DeepGreen,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { gardenState.bloomedCount / 6f },
                            color = ForestGreen,
                            trackColor = SageTint,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50))
                        )

                        val remaining = 6 - gardenState.bloomedCount
                        Text(
                            text = if (remaining == 1) {
                                "1 more verified ${if (role == UserRole.DONOR) "donation handover" else "delivery"} reaches Full Bloom & unlocks a new garden!"
                            } else {
                                "$remaining more verified ${if (role == UserRole.DONOR) "donations" else "deliveries"} to reach Full Bloom."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestGreen
                        )
                    }
                }
            }

            item(key = "garden_canvas", contentType = "garden_canvas") {
                HarvestGardenCanvas(
                    theme = gardenState.activeTheme,
                    slots = gardenState.slots,
                    selectedSlotIndex = inspectedSlot.slotIndex,
                    onSlotSelected = { inspectedSlotIndex = it.slotIndex },
                    reducedMotion = reducedMotion
                )
            }

            item(key = "slot_inspector", contentType = "slot_inspector") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (inspectedSlot.isBloomed) SageTint else SoftWhite,
                    border = BorderStroke(1.dp, CreamCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (inspectedSlot.isBloomed) ForestGreen else WarmCream),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalFlorist,
                                contentDescription = null,
                                tint = if (inspectedSlot.isBloomed) HarvestGold else MutedText
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (inspectedSlot.isBloomed) {
                                    "Slot ${inspectedSlot.slotIndex + 1} • ${inspectedSlot.flowerSpecies}"
                                } else {
                                    "Slot ${inspectedSlot.slotIndex + 1} • Waiting Sprout"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = DarkText
                            )
                            Text(
                                text = if (inspectedSlot.isBloomed) {
                                    "Grown from: ${inspectedSlot.earnedFromTitle} (${inspectedSlot.earnedDateLabel})"
                                } else {
                                    "Complete a verified handover to bloom a ${inspectedSlot.flowerSpecies} here."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedText
                            )
                        }
                    }
                }
            }

            item(key = "themes_header", contentType = "section_header") {
                Text(
                    text = "Garden Environments",
                    style = MaterialTheme.typography.titleMedium,
                    color = DarkText
                )
            }

            item(key = "themes_row", contentType = "themes_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GardenTheme.entries.forEach { theme ->
                        val unlocked = theme in gardenState.unlockedThemes
                        val active = theme == gardenState.activeTheme
                        Surface(
                            onClick = { if (unlocked) onSelectTheme(theme) },
                            shape = RoundedCornerShape(16.dp),
                            color = when {
                                active -> ForestGreen
                                unlocked -> SoftWhite
                                else -> WarmCream
                            },
                            border = BorderStroke(1.dp, if (active) ForestGreen else CreamCardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (unlocked) Icons.Outlined.Park else Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = if (active) HarvestGold else if (unlocked) ForestGreen else MutedText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = theme.title.removeSuffix(" Garden"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (active) SoftWhite else if (unlocked) DarkText else MutedText
                                )
                            }
                        }
                    }
                }
            }

            item(key = "archive_header", contentType = "section_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Previous Gardens Archive (${gardenState.archivedGardens.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkText
                    )
                    if (gardenState.archivedGardens.isNotEmpty()) {
                        Text(
                            text = "Tap to revisit memories",
                            style = MaterialTheme.typography.labelSmall,
                            color = ForestGreen
                        )
                    }
                }
            }

            items(
                items = gardenState.archivedGardens,
                key = { it.id },
                contentType = { "archived_garden" }
            ) { archived ->
                Surface(
                    onClick = { inspectedArchivedGarden = archived },
                    shape = RoundedCornerShape(20.dp),
                    color = SoftWhite,
                    border = BorderStroke(1.dp, CreamCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("archived_garden_${archived.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(GoldTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EmojiEvents,
                                contentDescription = archived.badgeTitle,
                                tint = HarvestGold
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = archived.theme.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = DarkText
                            )
                            Text(
                                text = "${archived.badgeTitle} • Preserved ${archived.completedDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ForestGreen
                            )
                            Text(
                                text = "6/6 verified deeds • Tap to view garden memories",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedText
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = "Open garden memories",
                            tint = ForestGreen
                        )
                    }
                }
            }

            item(key = "personal_impact", contentType = "impact_summary") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SoftWhite,
                    border = BorderStroke(1.dp, CreamCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Personal Cultivation Summary",
                            style = MaterialTheme.typography.titleSmall,
                            color = DarkText
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "${impactSummary.verifiedHandoversCount}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = ForestGreen
                                )
                                Text("Verified Handovers", style = MaterialTheme.typography.bodySmall, color = MutedText)
                            }
                            Column {
                                Text(
                                    text = "${"%.1f".format(impactSummary.measuredWeightKg)} kg",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = ForestGreen
                                )
                                Text("Measured Weight", style = MaterialTheme.typography.bodySmall, color = MutedText)
                            }
                            Column {
                                Text(
                                    text = "~${impactSummary.estimatedMealsSupported}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = HarvestGold
                                )
                                Text("Est. Meals", style = MaterialTheme.typography.bodySmall, color = MutedText)
                            }
                        }
                    }
                }
            }
        } else {
            item(key = "grove_hero", contentType = "grove_hero") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = ForestGreen
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(50), color = SoftWhite.copy(alpha = 0.16f)) {
                            Text(
                                text = "Collective Impact",
                                style = MaterialTheme.typography.labelMedium,
                                color = HarvestGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "${communityGrove.totalCommunityFlowers} Community Blooms",
                            style = MaterialTheme.typography.headlineMedium,
                            color = SoftWhite
                        )
                        Text(
                            text = communityGrove.milestoneLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmCream.copy(alpha = 0.9f)
                        )
                        LinearProgressIndicator(
                            progress = { (communityGrove.totalCommunityFlowers.toFloat() / communityGrove.milestoneGoal).coerceIn(0f, 1f) },
                            color = HarvestGold,
                            trackColor = DeepGreen,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50))
                        )
                        Text(
                            text = "${communityGrove.totalCommunityFlowers} / ${communityGrove.milestoneGoal} flowers • ${communityGrove.activeDonorsCount} donors & ${communityGrove.activeVolunteersCount} volunteers",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmCream.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            item(key = "grove_privacy", contentType = "info_banner") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SageTint,
                    border = BorderStroke(1.dp, CreamCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VerifiedUser,
                            contentDescription = null,
                            tint = ForestGreen
                        )
                        Text(
                            text = "Community Privacy Shield: Only aggregate district blooms are shown. Recipient identities and shelter locations are never exposed publicly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkText
                        )
                    }
                }
            }

            item(key = "grove_recent_header", contentType = "section_header") {
                Text(
                    text = "Recent Community Blooms",
                    style = MaterialTheme.typography.titleMedium,
                    color = DarkText
                )
            }

            items(
                items = communityGrove.recentCommunityBlooms,
                key = { it },
                contentType = { "community_bloom" }
            ) { entry ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SoftWhite,
                    border = BorderStroke(1.dp, CreamCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Spa,
                            contentDescription = null,
                            tint = SageGreen
                        )
                        Text(
                            text = entry,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkText
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenMemorySheet(
    archivedGarden: ArchivedGarden,
    reducedMotion: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedSlotIndex by remember(archivedGarden.id) { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SoftCreamSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(shape = RoundedCornerShape(50), color = GoldTint) {
                        Text(
                            text = "${archivedGarden.badgeTitle} • ${archivedGarden.completedDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepGreen,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "${archivedGarden.theme.title} Memories",
                        style = MaterialTheme.typography.titleLarge,
                        color = DarkText
                    )
                    Text(
                        text = if (archivedGarden.totalMealsCultivated > 0) {
                            "${archivedGarden.flowers.size} verified deeds • ${"%.1f".format(archivedGarden.totalWeightKg)} kg • ~${archivedGarden.totalMealsCultivated} meals shared"
                        } else {
                            "${archivedGarden.flowers.size} verified contributions that cultivated this garden"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = ForestGreen
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close garden memories",
                        tint = MutedText
                    )
                }
            }

            HarvestGardenCanvas(
                theme = archivedGarden.theme,
                slots = archivedGarden.flowers,
                selectedSlotIndex = selectedSlotIndex,
                onSlotSelected = { selectedSlotIndex = it.slotIndex },
                reducedMotion = reducedMotion
            )

            Text(
                text = "Verified Contributions (${archivedGarden.flowers.size}/6)",
                style = MaterialTheme.typography.titleSmall,
                color = DarkText
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(
                    items = archivedGarden.flowers,
                    key = { it.slotIndex },
                    contentType = { "memory_flower" }
                ) { slot ->
                    val isSelected = slot.slotIndex == selectedSlotIndex
                    Surface(
                        onClick = { selectedSlotIndex = slot.slotIndex },
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) SageTint else SoftWhite,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) ForestGreen else CreamCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("memory_flower_card_${slot.slotIndex}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) ForestGreen else WarmCream),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocalFlorist,
                                    contentDescription = slot.flowerSpecies,
                                    tint = if (isSelected) HarvestGold else ForestGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Bloom ${slot.slotIndex + 1} • ${slot.flowerSpecies}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ForestGreen
                                    )
                                    Text(
                                        text = slot.earnedDateLabel ?: archivedGarden.completedDate,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MutedText
                                    )
                                }
                                Text(
                                    text = slot.earnedFromTitle ?: "Verified Community Contribution",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DarkText
                                )
                                if (!slot.contributionSummary.isNullOrBlank()) {
                                    Text(
                                        text = slot.contributionSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedText
                                    )
                                }
                                if (!slot.district.isNullOrBlank()) {
                                    Text(
                                        text = "District: ${slot.district}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ForestGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlowerGrowthCelebrationDialog(
    event: FlowerGrowthEvent,
    reducedMotion: Boolean,
    onDismiss: () -> Unit,
    onExploreGarden: () -> Unit
) {
    if (event.reachedFullBloom) {
        HarvestCompletionCelebrationDialog(
            event = event,
            reducedMotion = reducedMotion,
            onDismiss = onDismiss,
            onExploreGarden = onExploreGarden
        )
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = SoftWhite,
                border = BorderStroke(1.5.dp, HarvestGold),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(SageTint),
                        contentAlignment = Alignment.Center
                    ) {
                        PipMascot(
                            size = 66.dp,
                            mood = PipMood.CELEBRATING,
                            reducedMotion = reducedMotion,
                            animate = false
                        )
                    }

                    Surface(shape = RoundedCornerShape(50), color = SageTint) {
                        Text(
                            text = "New Flower Grown • Slot ${event.slotIndex + 1}/6",
                            style = MaterialTheme.typography.labelMedium,
                            color = ForestGreen,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    Text(
                        text = "${event.flowerSpecies} Bloomed!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = DarkText,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Your verified handover for \"${event.earnedFromTitle}\" planted a living bloom in your Harvest Garden.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = {
                            onDismiss()
                            onExploreGarden()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("explore_garden_from_celebration")
                    ) {
                        Text("Explore Harvest Garden")
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Continue", color = MutedText)
                    }
                }
            }
        }
    }
}

@Composable
private fun HarvestCompletionCelebrationDialog(
    event: FlowerGrowthEvent,
    reducedMotion: Boolean,
    onDismiss: () -> Unit,
    onExploreGarden: () -> Unit
) {
    val completedFlowers = remember(event) {
        if (event.completedGardenFlowers.isNotEmpty()) {
            event.completedGardenFlowers
        } else {
            (0..5).map { idx ->
                FlowerSlot(
                    slotIndex = idx,
                    isBloomed = true,
                    flowerSpecies = if (idx == 5) event.flowerSpecies else "Native Bloom"
                )
            }
        }
    }

    var bloomedStageCount by remember(event) { mutableIntStateOf(if (reducedMotion) 6 else 1) }
    var collectionStamped by remember(event) { mutableStateOf(reducedMotion) }

    LaunchedEffect(event, reducedMotion) {
        if (reducedMotion) {
            bloomedStageCount = 6
            collectionStamped = true
        } else {
            for (count in 2..6) {
                delay(180L)
                bloomedStageCount = count
            }
            delay(320L)
            collectionStamped = true
        }
    }

    val animatedSlots = remember(completedFlowers, bloomedStageCount) {
        completedFlowers.mapIndexed { idx, slot ->
            slot.copy(isBloomed = idx < bloomedStageCount)
        }
    }

    val stampScaleState = animateFloatAsState(
        targetValue = if (collectionStamped) 1f else 0.72f,
        animationSpec = if (reducedMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "harvest_stamp_scale"
    )

    val sparklePhaseState: State<Float>? = if (!reducedMotion) {
        val transition = rememberInfiniteTransition(label = "harvest_sparkles")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "sparkle_phase"
        )
    } else null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(30.dp),
            color = SoftWhite,
            border = BorderStroke(2.dp, HarvestGold),
            shadowElevation = 10.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (sparklePhaseState != null) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val phase = sparklePhaseState.value
                        val phaseRad = phase * 1.57079632679
                        for (i in 0 until 10) {
                            val rad = SPARKLE_BASE_ANGLES_RAD[i] + phaseRad
                            val x = size.width * SPARKLE_X_FRAC[i] + (cos(rad) * 14.0).toFloat()
                            val yProgress = (phase + i * 0.13f) % 1f
                            val y = size.height * (0.65f - yProgress * 0.55f) + (sin(rad) * 8.0).toFloat()
                            val bucket = ((1f - yProgress).coerceIn(0f, 1f) * (SPARKLE_ALPHA_BUCKETS - 1))
                                .toInt()
                                .coerceIn(0, SPARKLE_ALPHA_BUCKETS - 1)
                            drawCircle(
                                color = if (i % 2 == 0) SPARKLE_GOLD_PALETTE[bucket] else SPARKLE_SAGE_PALETTE[bucket],
                                radius = SPARKLE_RADIUS[i],
                                center = Offset(x, y)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(GoldTint),
                            contentAlignment = Alignment.Center
                        ) {
                            PipMascot(
                                size = 48.dp,
                                mood = PipMood.CELEBRATING,
                                reducedMotion = reducedMotion,
                                animate = false
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(shape = RoundedCornerShape(50), color = GoldTint) {
                                Text(
                                    text = event.completedTheme?.badgeTitle ?: "Full Bloom Achieved!",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DeepGreen,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Harvest Celebration!",
                                style = MaterialTheme.typography.titleLarge,
                                color = ForestGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Animated 6-flower bloom stage transitioning into permanent collection
                    HarvestGardenCanvas(
                        theme = event.completedTheme ?: GardenTheme.SPRING_BLOSSOM,
                        slots = animatedSlots,
                        selectedSlotIndex = 5,
                        onSlotSelected = {},
                        reducedMotion = reducedMotion,
                        modifier = Modifier.graphicsLayer {
                            val s = 0.94f + 0.06f * stampScaleState.value
                            scaleX = s
                            scaleY = s
                        }
                    )

                    AnimatedVisibility(
                        visible = collectionStamped,
                        enter = if (reducedMotion) fadeIn(snap()) else fadeIn(tween(300)) + scaleIn(
                            initialScale = 0.88f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = SageTint,
                            border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CollectionsBookmark,
                                    contentDescription = "Permanent collection",
                                    tint = ForestGreen
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Preserved in Your Permanent Collection",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ForestGreen
                                    )
                                    Text(
                                        text = "${event.completedTheme?.title ?: "Your garden"} & all 6 verified deeds are now saved in Previous Gardens Archive.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DarkText
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Your final ${event.flowerSpecies} completed all 6 slots! ${event.nextUnlockedTheme?.title ?: "A new garden theme"} is now unlocked to grow next.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = {
                            onDismiss()
                            onExploreGarden()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("explore_garden_from_celebration")
                    ) {
                        Text("View Collection & Memories")
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Continue", color = MutedText)
                    }
                }
            }
        }
    }
}
