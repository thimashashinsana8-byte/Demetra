package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

private val WIZARD_STEP_TITLES = listOf(
    "Category & Diet",
    "Details & Photo",
    "Quantity & Safety",
    "Pickup & Preview"
)
private val DIETARY_OPTIONS = listOf("Vegetarian", "Halal-friendly", "Vegan", "Dairy-free", "Nut-free")
private val PICKUP_PRESET_WINDOWS = listOf(
    "Today, 7:00 PM – 8:30 PM",
    "Today, 8:30 PM – 10:00 PM",
    "Tomorrow, 8:00 AM – 10:00 AM"
)
private val CUSTOM_DAY_OPTIONS = listOf("Today", "Tomorrow")
private val CUSTOM_DURATION_PRESETS = listOf(
    30 to "30 min",
    60 to "1 hr",
    90 to "1.5 hrs",
    120 to "2 hrs"
)

@Composable
fun DonorDashboardScreen(
    donations: List<FoodDonation>,
    impact: ImpactSummary,
    garden: GardenState,
    userDistrict: String,
    selectedMapDistricts: Set<String>,
    language: AppLanguage,
    lowDataMode: Boolean,
    reducedMotion: Boolean,
    onToggleMapDistrict: (String) -> Unit,
    onApplyMapDistricts: (Set<String>) -> Unit,
    onClearMapDistricts: () -> Unit,
    onCreateDonationClick: () -> Unit,
    onDonationClick: (String) -> Unit,
    onOpenGarden: () -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = stringsFor(language)
    var showMapView by remember { mutableStateOf(false) }
    var selectedPinId by remember { mutableStateOf(donations.firstOrNull()?.id) }

    val filteredMapDonations = remember(donations, selectedMapDistricts) {
        donations.filter { item ->
            item.status != DonationStatus.DELIVERED_VERIFIED &&
                (selectedMapDistricts.isEmpty() || normalizeSriLankaDistrict(item.district) in selectedMapDistricts)
        }
    }
    val pinnedDonation = remember(filteredMapDonations, selectedPinId) {
        filteredMapDonations.find { it.id == selectedPinId } ?: filteredMapDonations.firstOrNull()
    }
    val onPinSelected = remember { { d: FoodDonation -> selectedPinId = d.id } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "donor_hero", contentType = "donor_hero") {
            // Quick Donation Hero CTA Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ForestGreen
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = RoundedCornerShape(50), color = SoftWhite.copy(alpha = 0.16f)) {
                            Text(
                                text = "Quick Donation",
                                style = MaterialTheme.typography.labelMedium,
                                color = HarvestGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "Sri Lanka • LKR",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmCream.copy(alpha = 0.8f)
                        )
                    }
                    Text(
                        text = "Share surplus food with verified local kitchens",
                        style = MaterialTheme.typography.headlineSmall,
                        color = SoftWhite
                    )
                    Text(
                        text = "Smart Relay matches your listing with nearby recipients and volunteers in under 2 minutes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmCream.copy(alpha = 0.88f)
                    )
                    Button(
                        onClick = onCreateDonationClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HarvestGold,
                            contentColor = DarkText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("quick_donate_button")
                    ) {
                        Icon(Icons.Outlined.AddCircleOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(copy.quickDonateCta, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        item(key = "donor_metrics", contentType = "donor_metrics") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatTile(
                    label = "Verified Handovers",
                    value = "${impact.verifiedHandoversCount}",
                    badge = "Measured",
                    icon = Icons.Outlined.Verified,
                    modifier = Modifier.weight(1f)
                )
                MetricStatTile(
                    label = "Meals Supported",
                    value = "~${impact.estimatedMealsSupported}",
                    badge = "Estimated",
                    icon = Icons.Outlined.Restaurant,
                    accentColor = HarvestGold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item(key = "donor_garden_preview", contentType = "donor_garden_preview") {
            Surface(
                onClick = onOpenGarden,
                shape = RoundedCornerShape(20.dp),
                color = SageTint,
                border = BorderStroke(1.dp, CreamCardBorder),
                modifier = Modifier.fillMaxWidth().testTag("donor_garden_preview")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SoftWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.LocalFlorist, contentDescription = null, tint = ForestGreen)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${garden.activeTheme.title} • ${garden.bloomedCount}/6 Blooms",
                            style = MaterialTheme.typography.titleSmall,
                            color = DarkText
                        )
                        Text(
                            text = if (garden.bloomedCount == 5) {
                                "1 more verified handover reaches Full Bloom!"
                            } else {
                                copy.harvestGardenTagline
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestGreen
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open Garden", tint = ForestGreen)
                }
            }
        }

        item(key = "donor_list_header", contentType = "section_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Donations & Smart Relay",
                    style = MaterialTheme.typography.titleMedium,
                    color = DarkText
                )
                Surface(
                    onClick = { showMapView = !showMapView },
                    shape = RoundedCornerShape(14.dp),
                    color = if (showMapView) ForestGreen else SageTint,
                    border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.35f)),
                    modifier = Modifier.testTag("donor_toggle_map_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (showMapView) Icons.Outlined.ViewList else Icons.Outlined.Map,
                            contentDescription = null,
                            tint = if (showMapView) SoftWhite else ForestGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (showMapView) "List" else "District Map",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (showMapView) SoftWhite else ForestGreen
                        )
                    }
                }
            }
        }

        if (showMapView) {
            item(key = "donor_district_filter", contentType = "district_filter") {
                DistrictMapFilterBar(
                    donations = donations,
                    userDistrict = userDistrict,
                    selectedDistricts = selectedMapDistricts,
                    onToggleDistrict = onToggleMapDistrict,
                    onApplyDistricts = onApplyMapDistricts,
                    onClearDistricts = onClearMapDistricts
                )
            }
            item(key = "donor_google_map", contentType = "district_map") {
                DistrictDonationsGoogleMap(
                    donations = filteredMapDonations,
                    selectedDistricts = selectedMapDistricts,
                    selectedDonationId = selectedPinId,
                    onSelectDonation = onPinSelected,
                    reducedMotion = reducedMotion
                )
            }
            if (pinnedDonation != null) {
                item(key = "donor_pinned_${pinnedDonation.id}", contentType = "donation_card") {
                    DonationSummaryCard(
                        donation = pinnedDonation,
                        lowDataMode = lowDataMode,
                        onClick = { onDonationClick(pinnedDonation.id) }
                    )
                }
            }
        } else if (donations.isEmpty()) {
            item(key = "donor_empty", contentType = "empty_state") {
                EmptyBotanicalState(
                    title = "No surplus listings yet",
                    message = "Tap '${copy.quickDonateCta}' to share wholesome surplus food.",
                    reducedMotion = reducedMotion,
                    actionLabel = copy.quickDonateCta,
                    onAction = onCreateDonationClick
                )
            }
        } else {
            items(donations, key = { it.id }, contentType = { "donation_card" }) { donation ->
                DonationSummaryCard(
                    donation = donation,
                    lowDataMode = lowDataMode,
                    onClick = { onDonationClick(donation.id) }
                )
            }
        }
    }
}

@Composable
fun DonationSummaryCard(
    donation: FoodDonation,
    lowDataMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = SoftWhite,
        border = BorderStroke(1.dp, CreamCardBorder),
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("donation_card_${donation.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!lowDataMode) {
                    DemetraAsyncImage(
                        model = donation.pickedPhotoUri ?: donation.photoResId,
                        contentDescription = donation.title,
                        targetSizePx = 220,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SageTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Fastfood, contentDescription = null, tint = ForestGreen)
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(status = donation.status)
                        Text(
                            text = donation.createdAtLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedText
                        )
                    }
                    Text(
                        text = donation.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${donation.servingsEstimate} est. portions (${donation.quantityKg} kg) • ${donation.district}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }
            }

            // Smart Relay concise explanation bar
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = WarmCream
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = HarvestGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${donation.smartRelayMatch.matchPercentage}% Match • ${donation.smartRelayMatch.recipientOrgName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ForestGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${donation.smartRelayMatch.distanceKm} km",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDonationWizardScreen(
    defaultDistrict: String,
    reducedMotion: Boolean,
    onBack: () -> Unit,
    onSubmitDonation: (
        title: String,
        category: FoodCategory,
        quantityKg: Double,
        servingsEstimate: Int,
        locationName: String,
        district: String,
        pickupWindow: String,
        expiryNote: String,
        dietaryTags: List<String>,
        pickedPhotoUri: String?
    ) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf(FoodCategory.COOKED_MEALS) }
    var selectedDietary by remember { mutableStateOf(setOf("Vegetarian", "Halal-friendly")) }
    var title by remember { mutableStateOf("Sri Lankan Rice & Dhal Curry Boxes") }
    var locationName by remember { mutableStateOf("65 Ward Place, Cinnamon Gardens") }
    var district by remember { mutableStateOf(defaultDistrict.ifBlank { "Colombo" }) }
    var showDistrictPicker by remember { mutableStateOf(false) }
    var quantityKgText by remember { mutableStateOf("8.0") }
    var servingsText by remember { mutableStateOf("20") }
    var expiryNote by remember { mutableStateOf("Prepared fresh today • Best within 3 hours") }
    var pickupWindow by remember { mutableStateOf("Today, 7:00 PM – 8:30 PM") }
    var useCustomWindow by remember { mutableStateOf(false) }
    var customDay by remember { mutableStateOf("Today") }
    var customStartMinutes by remember { mutableIntStateOf(19 * 60) } // 7:00 PM
    var customEndMinutes by remember { mutableIntStateOf(20 * 60 + 30) } // 8:30 PM
    var activeTimePickerTarget by remember { mutableStateOf<String?>(null) } // "start" or "end"
    var safetyConfirmed by remember { mutableStateOf(true) }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }

    val customDurationMinutes = customEndMinutes - customStartMinutes
    val isPickupWindowValid = !useCustomWindow || customDurationMinutes >= 15
    val effectivePickupWindow = remember(
        useCustomWindow,
        pickupWindow,
        customDay,
        customStartMinutes,
        customEndMinutes
    ) {
        if (useCustomWindow) {
            "$customDay, ${formatTime12Hour(customStartMinutes)} – ${formatTime12Hour(customEndMinutes)}"
        } else {
            pickupWindow
        }
    }

    activeTimePickerTarget?.let { target ->
        val isStart = target == "start"
        val initialMinutes = if (isStart) customStartMinutes else customEndMinutes
        val timePickerState = rememberTimePickerState(
            initialHour = initialMinutes / 60,
            initialMinute = initialMinutes % 60,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { activeTimePickerTarget = null },
            containerColor = SoftWhite,
            title = {
                Text(
                    text = if (isStart) "Handover Start Time" else "Handover End Time",
                    style = MaterialTheme.typography.titleMedium,
                    color = DarkText
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = WarmCream,
                            selectorColor = ForestGreen,
                            clockDialSelectedContentColor = SoftWhite,
                            clockDialUnselectedContentColor = DarkText,
                            periodSelectorSelectedContainerColor = SageTint,
                            periodSelectorSelectedContentColor = ForestGreen,
                            periodSelectorUnselectedContentColor = MutedText,
                            timeSelectorSelectedContainerColor = SageTint,
                            timeSelectorSelectedContentColor = ForestGreen,
                            timeSelectorUnselectedContainerColor = WarmCream,
                            timeSelectorUnselectedContentColor = DarkText
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pickedMinutes = timePickerState.hour * 60 + timePickerState.minute
                        if (isStart) {
                            val previousDuration = (customEndMinutes - customStartMinutes).coerceIn(30, 180)
                            customStartMinutes = pickedMinutes
                            if (pickedMinutes + 15 > customEndMinutes) {
                                customEndMinutes = (pickedMinutes + previousDuration).coerceAtMost(23 * 60 + 55)
                            }
                        } else {
                            customEndMinutes = pickedMinutes
                        }
                        useCustomWindow = true
                        activeTimePickerTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    modifier = Modifier.testTag("confirm_custom_time_button")
                ) {
                    Text("Set Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeTimePickerTarget = null }) {
                    Text("Cancel", color = MutedText)
                }
            }
        )
    }

    if (showDistrictPicker) {
        SriLankaDistrictPickerSheet(
            selectedDistrict = district,
            onDistrictSelected = { district = it },
            onDismiss = { showDistrictPicker = false }
        )
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pickedImageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCream)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        SubScreenTopBar(
            title = "Create Food Donation",
            onBack = {
                if (step > 0) step -= 1 else onBack()
            }
        )

        // Step Progress Indicator
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Step ${step + 1} of 4 • ${WIZARD_STEP_TITLES[step]}",
                    style = MaterialTheme.typography.labelLarge,
                    color = ForestGreen
                )
                Text(
                    text = "${((step + 1) * 25)}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedText
                )
            }
            LinearProgressIndicator(
                progress = { (step + 1) / 4f },
                color = ForestGreen,
                trackColor = SageTint,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (step) {
                0 -> {
                    Text(
                        text = "Select Food Category",
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkText
                    )
                    FoodCategory.entries.forEach { cat ->
                        val active = cat == selectedCategory
                        Surface(
                            onClick = { selectedCategory = cat },
                            shape = RoundedCornerShape(18.dp),
                            color = if (active) SageTint else SoftWhite,
                            border = BorderStroke(if (active) 2.dp else 1.dp, if (active) ForestGreen else CreamCardBorder),
                            modifier = Modifier.fillMaxWidth().testTag("cat_${cat.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DemetraAsyncImage(
                                    model = cat.defaultImageRes,
                                    contentDescription = cat.label,
                                    targetSizePx = 180,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cat.label, style = MaterialTheme.typography.titleSmall, color = DarkText)
                                    Text(cat.safetyNote, style = MaterialTheme.typography.bodySmall, color = MutedText)
                                }
                                RadioButton(
                                    selected = active,
                                    onClick = { selectedCategory = cat },
                                    colors = RadioButtonDefaults.colors(selectedColor = ForestGreen)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Dietary & Allergen Suitability",
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkText
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DIETARY_OPTIONS.forEach { tag ->
                            val picked = tag in selectedDietary
                            FilterChip(
                                selected = picked,
                                onClick = {
                                    selectedDietary = if (picked) selectedDietary - tag else selectedDietary + tag
                                },
                                label = { Text(tag) }
                            )
                        }
                    }
                }

                1 -> {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = SoftWhite,
                        border = BorderStroke(1.dp, CreamCardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Food Listing Title") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().testTag("donation_title_input")
                            )
                            OutlinedTextField(
                                value = locationName,
                                onValueChange = { locationName = it },
                                label = { Text("Pickup Street / Landmark") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            DistrictSelectorField(
                                label = "Pickup District (25 Districts in Sri Lanka)",
                                selectedDistrict = district,
                                onClick = { showDistrictPicker = true },
                                modifier = Modifier.testTag("donation_district_selector")
                            )
                        }
                    }

                    // Food Image Upload using zero-permission Android Photo Picker
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = SoftWhite,
                        border = BorderStroke(1.dp, CreamCardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Food Photograph",
                                style = MaterialTheme.typography.titleSmall,
                                color = DarkText
                            )
                            DemetraAsyncImage(
                                model = pickedImageUri ?: selectedCategory.defaultImageRes,
                                contentDescription = if (pickedImageUri != null) "Selected food photo" else "Default category food photo",
                                targetSizePx = 600,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, ForestGreen),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = ForestGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose Photo from Device", color = ForestGreen)
                            }
                        }
                    }
                }

                2 -> {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = SoftWhite,
                        border = BorderStroke(1.dp, CreamCardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = quantityKgText,
                                    onValueChange = { quantityKgText = it },
                                    label = { Text("Weight (kg)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = servingsText,
                                    onValueChange = { servingsText = it },
                                    label = { Text("Est. Portions") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OutlinedTextField(
                                value = expiryNote,
                                onValueChange = { expiryNote = it },
                                label = { Text("Freshness & Safe Consumption Window") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Food Safety Checklist (prevents unsafe food from entering Harvest Garden)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (safetyConfirmed) SageTint else TerracottaTint,
                        border = BorderStroke(1.dp, if (safetyConfirmed) ForestGreen else ErrorTerracotta)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Checkbox(
                                checked = safetyConfirmed,
                                onCheckedChange = { safetyConfirmed = it },
                                colors = CheckboxDefaults.colors(checkedColor = ForestGreen)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Food Hygiene & Safety Declaration",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DarkText
                                )
                                Text(
                                    text = "I confirm this food is freshly stored, uncontaminated, sealed in food-grade containers, and safe for consumption.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedText
                                )
                            }
                        }
                    }
                }

                3 -> {
                    Text(
                        text = "Handover Pickup Window",
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkText
                    )
                    Text(
                        text = "Choose a quick slot or adjust a custom handover window before Smart Relay matches a volunteer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                    PICKUP_PRESET_WINDOWS.forEachIndexed { index, win ->
                        val active = !useCustomWindow && pickupWindow == win
                        Surface(
                            onClick = {
                                useCustomWindow = false
                                pickupWindow = win
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (active) SageTint else SoftWhite,
                            border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) ForestGreen else CreamCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pickup_preset_$index")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(win, style = MaterialTheme.typography.bodyMedium, color = DarkText)
                                RadioButton(
                                    selected = active,
                                    onClick = {
                                        useCustomWindow = false
                                        pickupWindow = win
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = ForestGreen)
                                )
                            }
                        }
                    }

                    // Custom Handover Time Window Selector
                    Surface(
                        onClick = { useCustomWindow = true },
                        shape = RoundedCornerShape(20.dp),
                        color = if (useCustomWindow) SageTint.copy(alpha = 0.72f) else SoftWhite,
                        border = BorderStroke(
                            if (useCustomWindow) 1.5.dp else 1.dp,
                            if (useCustomWindow) ForestGreen else CreamCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_pickup_window_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (useCustomWindow) ForestGreen else SageTint,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.Schedule,
                                                contentDescription = "Custom handover time",
                                                tint = if (useCustomWindow) SoftWhite else ForestGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "Custom Handover Window",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = DarkText
                                        )
                                        Text(
                                            text = if (useCustomWindow) {
                                                "$customDay, ${formatTime12Hour(customStartMinutes)} – ${formatTime12Hour(customEndMinutes)}"
                                            } else {
                                                "Tap to set your exact handover start & end time"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (useCustomWindow) ForestGreen else MutedText
                                        )
                                    }
                                }
                                RadioButton(
                                    selected = useCustomWindow,
                                    onClick = { useCustomWindow = true },
                                    colors = RadioButtonDefaults.colors(selectedColor = ForestGreen)
                                )
                            }

                            AnimatedVisibility(
                                visible = useCustomWindow,
                                enter = if (reducedMotion) fadeIn() else expandVertically() + fadeIn(),
                                exit = if (reducedMotion) fadeOut() else shrinkVertically() + fadeOut()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    HorizontalDivider(color = CreamCardBorder)

                                    // Day Switcher
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Day:",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = DarkText
                                        )
                                        CUSTOM_DAY_OPTIONS.forEach { dayOption ->
                                            FilterChip(
                                                selected = customDay == dayOption,
                                                onClick = { customDay = dayOption },
                                                label = { Text(dayOption) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = ForestGreen,
                                                    selectedLabelColor = SoftWhite
                                                ),
                                                modifier = Modifier.testTag("custom_day_${dayOption.lowercase()}")
                                            )
                                        }
                                    }

                                    // Start & End Time Picker Triggers
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        TimeSlotPickerButton(
                                            label = "From",
                                            formattedTime = formatTime12Hour(customStartMinutes),
                                            onClick = { activeTimePickerTarget = "start" },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("custom_start_time_button")
                                        )
                                        TimeSlotPickerButton(
                                            label = "Until",
                                            formattedTime = formatTime12Hour(customEndMinutes),
                                            isError = !isPickupWindowValid,
                                            onClick = { activeTimePickerTarget = "end" },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("custom_end_time_button")
                                        )
                                    }

                                    // Quick Duration Presets
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Duration:",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MutedText
                                        )
                                        CUSTOM_DURATION_PRESETS.forEach { (mins, label) ->
                                            val selectedDuration = customDurationMinutes == mins
                                            FilterChip(
                                                selected = selectedDuration,
                                                onClick = {
                                                    val maxEnd = 23 * 60 + 55
                                                    val proposedEnd = customStartMinutes + mins
                                                    if (proposedEnd <= maxEnd) {
                                                        customEndMinutes = proposedEnd
                                                    } else {
                                                        customStartMinutes = (maxEnd - mins).coerceAtLeast(0)
                                                        customEndMinutes = maxEnd
                                                    }
                                                },
                                                label = { Text(label) },
                                                modifier = Modifier.testTag("duration_chip_$mins")
                                            )
                                        }
                                    }

                                    if (isPickupWindowValid) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = SoftWhite,
                                            border = BorderStroke(1.dp, CreamCardBorder)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.CheckCircle,
                                                    contentDescription = null,
                                                    tint = SuccessGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "${formatDurationLabel(customDurationMinutes)} handover window ready for volunteers",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = DeepGreen
                                                )
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = TerracottaTint,
                                            border = BorderStroke(1.dp, ErrorTerracotta)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.WarningAmber,
                                                    contentDescription = "Invalid time window",
                                                    tint = ErrorTerracotta,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Handover end time must be at least 15 minutes after start time.",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = ErrorTerracotta
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Preview Card
                    Text(
                        text = "Donation Preview",
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkText
                    )
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = SoftWhite,
                        border = BorderStroke(1.dp, CreamCardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(title, style = MaterialTheme.typography.titleMedium, color = DarkText)
                            Text(
                                text = "${selectedCategory.label} • ${quantityKgText} kg (~${servingsText} portions)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ForestGreen
                            )
                            Text(
                                text = "$locationName, $district",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedText
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = ForestGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Pickup: $effectivePickupWindow",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = DarkText
                                )
                                if (useCustomWindow) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = GoldTint
                                    ) {
                                        Text(
                                            text = "Custom",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DeepGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = selectedDietary.joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedText
                            )
                        }
                    }

                    PipSpeechBubble(
                        text = "Everything looks wholesome and well-labeled! Submitting will run Smart Relay for $effectivePickupWindow to find your closest verified match.",
                        reducedMotion = reducedMotion
                    )
                }
            }
        }

        // Bottom Next / Submit Bar
        Surface(
            color = SoftWhite,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step -= 1 },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text("Back")
                    }
                }
                Button(
                    onClick = {
                        if (step < 3) {
                            step += 1
                        } else if (safetyConfirmed && isPickupWindowValid) {
                            onSubmitDonation(
                                title.ifBlank { "Surplus Meal Packs" },
                                selectedCategory,
                                quantityKgText.toDoubleOrNull() ?: 6.0,
                                servingsText.toIntOrNull() ?: 15,
                                locationName,
                                district,
                                effectivePickupWindow,
                                expiryNote,
                                selectedDietary.toList(),
                                pickedImageUri?.toString()
                            )
                        }
                    },
                    enabled = when (step) {
                        2 -> safetyConfirmed
                        3 -> safetyConfirmed && isPickupWindowValid
                        else -> true
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag(if (step < 3) "wizard_next_button" else "submit_donation_button")
                ) {
                    Text(if (step < 3) "Continue" else "Publish & Run Smart Relay")
                }
            }
        }
    }
}

@Composable
private fun TimeSlotPickerButton(
    label: String,
    formattedTime: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = SoftWhite,
        border = BorderStroke(1.dp, if (isError) ErrorTerracotta else ForestGreen.copy(alpha = 0.55f)),
        modifier = modifier.heightIn(min = 56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isError) ErrorTerracotta else MutedText
                )
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkText
                )
            }
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Adjust $label time",
                tint = if (isError) ErrorTerracotta else ForestGreen,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun formatTime12Hour(totalMinutes: Int): String {
    val clamped = totalMinutes.coerceIn(0, 23 * 60 + 59)
    val hour24 = clamped / 60
    val minute = clamped % 60
    val period = if (hour24 < 12) "AM" else "PM"
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return "$hour12:${minute.toString().padStart(2, '0')} $period"
}

private fun formatDurationLabel(durationMinutes: Int): String {
    val hours = durationMinutes / 60
    val mins = durationMinutes % 60
    return when {
        hours == 0 -> "$mins min"
        mins == 0 -> if (hours == 1) "1 hr" else "$hours hrs"
        else -> "$hours hr $mins min"
    }
}

@Composable
fun DonationDetailTrackingScreen(
    donation: FoodDonation,
    reducedMotion: Boolean,
    onBack: () -> Unit,
    onAssignVolunteer: () -> Unit,
    onVerifyHandover: () -> Unit
) {
    var showHandoverModal by remember { mutableStateOf(false) }

    if (showHandoverModal) {
        QrPinHandoverDialog(
            title = "Confirm Donor Handover",
            subtitle = "Confirm collection of \"${donation.title}\" with volunteer ${donation.assignedVolunteerName ?: donation.smartRelayMatch.suggestedVolunteerName}.",
            expectedPin = donation.handoverPin,
            qrPayload = donation.qrPayload,
            onDismiss = { showHandoverModal = false },
            onVerified = {
                showHandoverModal = false
                onVerifyHandover()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCream)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        SubScreenTopBar(title = "Donation Tracking", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Food Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SoftWhite,
                border = BorderStroke(1.dp, CreamCardBorder)
            ) {
                Column {
                    DemetraAsyncImage(
                        model = donation.pickedPhotoUri ?: donation.photoResId,
                        contentDescription = donation.title,
                        targetSizePx = 640,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    )
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(status = donation.status)
                            donation.estimatedValueLkr?.takeIf { it > 0 }?.let { valueLkr ->
                                Text(
                                    text = "LKR $valueLkr",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ForestGreen
                                )
                            }
                        }
                        Text(
                            text = donation.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = DarkText
                        )
                        Text(
                            text = "${donation.servingsEstimate} est. portions • ${donation.quantityKg} kg • ${donation.pickupWindow}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedText
                        )
                        Text(
                            text = donation.expiryNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestGreen
                        )
                    }
                }
            }

            // Tracking Progress Timeline
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SoftWhite,
                border = BorderStroke(1.dp, CreamCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Redistribution Progress", style = MaterialTheme.typography.titleMedium, color = DarkText)
                    DonationStatus.entries.forEach { stage ->
                        val reached = donation.status.stepIndex >= stage.stepIndex
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (reached) ForestGreen else WarmCream),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (reached) Icons.Outlined.Check else Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = if (reached) SoftWhite else MutedText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = stage.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (reached) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (reached) DarkText else MutedText
                            )
                        }
                    }
                }
            }

            // Explainable Smart Relay Match Card
            val match = donation.smartRelayMatch
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SoftWhite,
                border = BorderStroke(1.5.dp, HarvestGold.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = HarvestGold)
                            Text("Smart Relay Match", style = MaterialTheme.typography.titleMedium, color = DarkText)
                        }
                        Surface(shape = RoundedCornerShape(50), color = GoldTint) {
                            Text(
                                text = "${match.matchPercentage}% Fit",
                                style = MaterialTheme.typography.labelLarge,
                                color = DeepGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Matched Recipient: ${match.recipientOrgName} (${match.recipientDistrict})",
                        style = MaterialTheme.typography.titleSmall,
                        color = ForestGreen
                    )
                    Text(
                        text = match.summaryReason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkText
                    )
                    HorizontalDivider(color = CreamCardBorder)
                    match.factors.forEach { factor ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(factor.title, style = MaterialTheme.typography.labelMedium, color = DarkText)
                                Text(factor.detail, style = MaterialTheme.typography.bodySmall, color = MutedText)
                            }
                            Text(
                                text = "+${factor.scoreContribution} pts",
                                style = MaterialTheme.typography.labelMedium,
                                color = ForestGreen
                            )
                        }
                    }

                    HorizontalDivider(color = CreamCardBorder)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Volunteer: ${donation.assignedVolunteerName ?: match.suggestedVolunteerName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = DarkText
                            )
                            Text(
                                text = "${match.volunteerVehicle} • ${match.distanceKm} km • ${match.urgency.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedText
                            )
                        }
                    }
                }
            }

            // Action Buttons
            if (donation.status == DonationStatus.MATCHING) {
                Button(
                    onClick = onAssignVolunteer,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("assign_volunteer_button")
                ) {
                    Text("Confirm Match & Dispatch ${match.suggestedVolunteerName}")
                }
            }

            if (donation.status != DonationStatus.DELIVERED_VERIFIED) {
                Button(
                    onClick = { showHandoverModal = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HarvestGold,
                        contentColor = DarkText
                    ),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("donor_verify_handover_button")
                ) {
                    Icon(Icons.Outlined.QrCode2, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verify Handover (QR / PIN ${donation.handoverPin})")
                }
            } else {
                PipSpeechBubble(
                    text = "Handover verified! A native flower has bloomed in your Harvest Garden.",
                    reducedMotion = reducedMotion
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DonorImpactHistoryScreen(
    donations: List<FoodDonation>,
    impact: ImpactSummary,
    lowDataMode: Boolean,
    onDonationClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "impact_summary", contentType = "impact_summary") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SoftWhite,
                border = BorderStroke(1.dp, CreamCardBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Impact Transparency", style = MaterialTheme.typography.titleMedium, color = DarkText)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${impact.verifiedHandoversCount}", style = MaterialTheme.typography.headlineSmall, color = ForestGreen)
                            Text("Measured Handovers", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                        Column {
                            Text("${"%.1f".format(impact.measuredWeightKg)} kg", style = MaterialTheme.typography.headlineSmall, color = ForestGreen)
                            Text("Measured Food", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                    }
                    HorizontalDivider(color = CreamCardBorder)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("~${impact.estimatedMealsSupported}", style = MaterialTheme.typography.headlineSmall, color = HarvestGold)
                            Text("Est. Meals (~0.4 kg/meal)", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                        Column {
                            Text("LKR ${impact.estimatedValueLkr}", style = MaterialTheme.typography.headlineSmall, color = HarvestGold)
                            Text("Est. Food Value", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                    }
                    Text(
                        text = "Demetra strictly separates measured handovers from meal and LKR estimates without fabricating unverified environmental statistics.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText
                    )
                }
            }
        }

        item(key = "history_header", contentType = "section_header") {
            Text("Donation History", style = MaterialTheme.typography.titleMedium, color = DarkText)
        }

        items(donations, key = { it.id }, contentType = { "donation_card" }) { item ->
            DonationSummaryCard(
                donation = item,
                lowDataMode = lowDataMode,
                onClick = { onDonationClick(item.id) }
            )
        }
    }
}
