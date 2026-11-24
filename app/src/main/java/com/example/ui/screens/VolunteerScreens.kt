package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VolunteerDashboardScreen(
    donations: List<FoodDonation>,
    impact: ImpactSummary,
    garden: GardenState,
    registeredVolunteer: VolunteerEntity?,
    lowDataMode: Boolean,
    reducedMotion: Boolean,
    onExploreClick: () -> Unit,
    onTaskClick: (String) -> Unit,
    onOpenGarden: () -> Unit,
    onOpenVolunteerRegistration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTask = remember(donations) {
        donations.firstOrNull {
            it.status == DonationStatus.VOLUNTEER_ASSIGNED || it.status == DonationStatus.PICKED_UP
        }
    }
    val openTasks = remember(donations) {
        donations.filter { it.status == DonationStatus.MATCHING }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (activeTask != null) {
            item(key = "active_${activeTask.id}", contentType = "active_task") {
                Surface(
                    onClick = { onTaskClick(activeTask.id) },
                    shape = RoundedCornerShape(24.dp),
                    color = ForestGreen,
                    modifier = Modifier.fillMaxWidth().testTag("active_volunteer_task_card")
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = RoundedCornerShape(50), color = SoftWhite.copy(alpha = 0.16f)) {
                            Text(
                                text = "Active Collection • ${activeTask.status.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = HarvestGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = activeTask.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = SoftWhite
                        )
                        Text(
                            text = "${activeTask.donorName} (${activeTask.district}) → ${activeTask.smartRelayMatch.recipientOrgName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmCream.copy(alpha = 0.9f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${activeTask.smartRelayMatch.distanceKm} km • PIN ${activeTask.handoverPin}",
                                style = MaterialTheme.typography.labelLarge,
                                color = HarvestGold
                            )
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open task", tint = SoftWhite)
                        }
                    }
                }
            }
        }

        item(key = "volunteer_registration_card", contentType = "registration_card") {
            Surface(
                onClick = onOpenVolunteerRegistration,
                shape = RoundedCornerShape(22.dp),
                color = SoftWhite,
                border = BorderStroke(1.dp, if (registeredVolunteer != null) ForestGreen.copy(alpha = 0.3f) else HarvestGold.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth().testTag("open_volunteer_registration_button")
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (registeredVolunteer != null) SageTint else GoldTint),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (registeredVolunteer != null) Icons.Outlined.Badge else Icons.Outlined.HowToReg,
                                    contentDescription = "Volunteer Registration",
                                    tint = if (registeredVolunteer != null) ForestGreen else DeepGreen
                                )
                            }
                            Column {
                                Text(
                                    text = if (registeredVolunteer != null) {
                                        "Registered • ${registeredVolunteer.preferredDistrict} District"
                                    } else {
                                        "Complete Volunteer Registration"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DarkText
                                )
                                Text(
                                    text = if (registeredVolunteer != null) {
                                        "${registeredVolunteer.vehicleType} • ${registeredVolunteer.availabilityWindow}"
                                    } else {
                                        "Choose your interest areas & preferred Sri Lankan district"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedText
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (registeredVolunteer != null) SageTint else ForestGreen
                        ) {
                            Text(
                                text = if (registeredVolunteer != null) "Edit" else "Register",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (registeredVolunteer != null) ForestGreen else SoftWhite,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (registeredVolunteer != null && registeredVolunteer.interestAreas.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            registeredVolunteer.interestAreas.take(4).forEach { area ->
                                Surface(shape = RoundedCornerShape(50), color = WarmCream) {
                                    Text(
                                        text = area,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ForestGreen,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item(key = "volunteer_metrics", contentType = "metrics_row") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricStatTile(
                    label = "Deliveries Done",
                    value = "${impact.deliveriesCompleted}",
                    badge = "Measured",
                    icon = Icons.Outlined.DeliveryDining,
                    modifier = Modifier.weight(1f)
                )
                MetricStatTile(
                    label = "Food Rescued",
                    value = "${"%.1f".format(impact.measuredWeightKg)} kg",
                    badge = "Measured",
                    icon = Icons.Outlined.Inventory2,
                    accentColor = HarvestGold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item(key = "volunteer_garden_teaser", contentType = "garden_teaser") {
            Surface(
                onClick = onOpenGarden,
                shape = RoundedCornerShape(20.dp),
                color = SageTint,
                border = BorderStroke(1.dp, CreamCardBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(SoftWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.LocalFlorist, contentDescription = null, tint = ForestGreen)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Volunteer Garden • ${garden.bloomedCount}/6 Blooms",
                            style = MaterialTheme.typography.titleSmall,
                            color = DarkText
                        )
                        Text(
                            text = "Complete a verified delivery to grow your next blossom.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestGreen
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ForestGreen)
                }
            }
        }

        item(key = "nearby_header", contentType = "section_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nearby Pickup Opportunities", style = MaterialTheme.typography.titleMedium, color = DarkText)
                TextButton(onClick = onExploreClick) {
                    Text("Map & All", color = ForestGreen)
                }
            }
        }

        if (openTasks.isEmpty()) {
            item(key = "volunteer_empty", contentType = "empty_state") {
                EmptyBotanicalState(
                    title = "All nearby pickups claimed",
                    message = "Check back soon or review your active delivery above.",
                    reducedMotion = reducedMotion
                )
            }
        } else {
            items(openTasks, key = { it.id }, contentType = { "opportunity_card" }) { task ->
                VolunteerOpportunityCard(
                    donation = task,
                    lowDataMode = lowDataMode,
                    onClick = { onTaskClick(task.id) }
                )
            }
        }
    }
}

@Composable
fun VolunteerExploreScreen(
    donations: List<FoodDonation>,
    userDistrict: String,
    selectedMapDistricts: Set<String>,
    lowDataMode: Boolean,
    reducedMotion: Boolean,
    onToggleMapDistrict: (String) -> Unit,
    onApplyMapDistricts: (Set<String>) -> Unit,
    onClearMapDistricts: () -> Unit,
    onTaskClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMapView by remember { mutableStateOf(true) }
    var selectedPinId by remember { mutableStateOf(donations.firstOrNull()?.id) }
    var urgencyFilter by remember { mutableStateOf<UrgencyLevel?>(null) }

    val activeAll = remember(donations) {
        donations.filter { it.status != DonationStatus.DELIVERED_VERIFIED }
    }
    val available = remember(activeAll, urgencyFilter, selectedMapDistricts) {
        activeAll.filter { item ->
            (urgencyFilter == null || item.smartRelayMatch.urgency == urgencyFilter) &&
                (selectedMapDistricts.isEmpty() || normalizeSriLankaDistrict(item.district) in selectedMapDistricts)
        }
    }
    val pinnedDonation = remember(available, selectedPinId) {
        available.find { it.id == selectedPinId } ?: available.firstOrNull()
    }
    val onPinSelected = remember { { d: FoodDonation -> selectedPinId = d.id } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "explore_controls", contentType = "explore_controls") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = urgencyFilter == null,
                        onClick = { urgencyFilter = null },
                        label = { Text("All (${activeAll.size})") }
                    )
                    FilterChip(
                        selected = urgencyFilter == UrgencyLevel.URGENT,
                        onClick = {
                            urgencyFilter = if (urgencyFilter == UrgencyLevel.URGENT) null else UrgencyLevel.URGENT
                        },
                        label = { Text("Urgent") }
                    )
                }

                Surface(
                    onClick = { showMapView = !showMapView },
                    shape = RoundedCornerShape(14.dp),
                    color = ForestGreen,
                    modifier = Modifier.testTag("toggle_map_view_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (showMapView) Icons.Outlined.ViewList else Icons.Outlined.Map,
                            contentDescription = null,
                            tint = SoftWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (showMapView) "List" else "Map",
                            style = MaterialTheme.typography.labelMedium,
                            color = SoftWhite
                        )
                    }
                }
            }
        }

        item(key = "explore_district_filter", contentType = "district_filter") {
            DistrictMapFilterBar(
                donations = activeAll,
                userDistrict = userDistrict,
                selectedDistricts = selectedMapDistricts,
                onToggleDistrict = onToggleMapDistrict,
                onApplyDistricts = onApplyMapDistricts,
                onClearDistricts = onClearMapDistricts
            )
        }

        if (showMapView) {
            item(key = "explore_map", contentType = "district_map") {
                DistrictDonationsGoogleMap(
                    donations = available,
                    selectedDistricts = selectedMapDistricts,
                    selectedDonationId = selectedPinId,
                    onSelectDonation = onPinSelected,
                    reducedMotion = reducedMotion
                )
            }
            if (pinnedDonation != null) {
                item(key = "pinned_${pinnedDonation.id}", contentType = "opportunity_card") {
                    VolunteerOpportunityCard(
                        donation = pinnedDonation,
                        lowDataMode = lowDataMode,
                        onClick = { onTaskClick(pinnedDonation.id) }
                    )
                }
            } else {
                item(key = "empty_map_districts", contentType = "empty_state") {
                    EmptyBotanicalState(
                        title = "No active requests in selected districts",
                        message = "Tap 'All Active' or select additional Sri Lankan districts above to view nearby pickups.",
                        reducedMotion = reducedMotion,
                        actionLabel = "Show All Districts",
                        onAction = onClearMapDistricts
                    )
                }
            }
        } else {
            if (available.isEmpty()) {
                item(key = "empty_list_districts", contentType = "empty_state") {
                    EmptyBotanicalState(
                        title = "No matching pickups",
                        message = "Try clearing the district or urgency filters.",
                        reducedMotion = reducedMotion,
                        actionLabel = "Reset Filters",
                        onAction = {
                            urgencyFilter = null
                            onClearMapDistricts()
                        }
                    )
                }
            } else {
                items(available, key = { it.id }, contentType = { "opportunity_card" }) { task ->
                    VolunteerOpportunityCard(
                        donation = task,
                        lowDataMode = lowDataMode,
                        onClick = { onTaskClick(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun VolunteerOpportunityCard(
    donation: FoodDonation,
    lowDataMode: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = SoftWhite,
        border = BorderStroke(1.dp, CreamCardBorder),
        modifier = Modifier.fillMaxWidth().testTag("volunteer_task_${donation.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!lowDataMode) {
                DemetraAsyncImage(
                    model = donation.pickedPhotoUri ?: donation.photoResId,
                    contentDescription = donation.title,
                    targetSizePx = 220,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp))
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(shape = RoundedCornerShape(50), color = GoldTint) {
                        Text(
                            text = "${donation.smartRelayMatch.distanceKm} km • ${donation.smartRelayMatch.urgency.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    StatusPill(status = donation.status)
                }
                Text(
                    text = donation.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = DarkText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${donation.donorName} (${donation.district}) → ${donation.smartRelayMatch.recipientDistrict}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${donation.quantityKg} kg • ${donation.smartRelayMatch.volunteerVehicle}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ForestGreen
                )
            }
        }
    }
}

@Composable
fun VolunteerTaskDetailScreen(
    donation: FoodDonation,
    reducedMotion: Boolean,
    onBack: () -> Unit,
    onAcceptTask: () -> Unit,
    onVerifyPickup: () -> Unit,
    onVerifyDelivery: () -> Unit
) {
    var modalMode by remember { mutableStateOf<String?>(null) }

    if (modalMode == "pickup") {
        QrPinHandoverDialog(
            title = "Verify Food Pickup",
            subtitle = "Confirm collection from ${donation.donorName} before transport.",
            expectedPin = donation.handoverPin,
            qrPayload = donation.qrPayload,
            onDismiss = { modalMode = null },
            onVerified = {
                modalMode = null
                onVerifyPickup()
            }
        )
    } else if (modalMode == "delivery") {
        QrPinHandoverDialog(
            title = "Confirm Safe Delivery",
            subtitle = "Verify handover to ${donation.smartRelayMatch.recipientOrgName}.",
            expectedPin = donation.handoverPin,
            qrPayload = donation.qrPayload,
            onDismiss = { modalMode = null },
            onVerified = {
                modalMode = null
                onVerifyDelivery()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCream)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        SubScreenTopBar(title = "Collection Task", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                            .height(170.dp)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    )
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(status = donation.status)
                        Text(donation.title, style = MaterialTheme.typography.headlineSmall, color = DarkText)
                        Text(
                            text = "${donation.quantityKg} kg • ${donation.servingsEstimate} est. portions • ${donation.pickupWindow}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedText
                        )
                    }
                }
            }

            // Navigation & Pickup Instructions
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SoftWhite,
                border = BorderStroke(1.dp, CreamCardBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Route & Pickup Instructions", style = MaterialTheme.typography.titleMedium, color = DarkText)
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Storefront, contentDescription = null, tint = ForestGreen)
                        Column {
                            Text("Pickup: ${donation.donorName}", style = MaterialTheme.typography.labelLarge, color = DarkText)
                            Text("${donation.locationName}, ${donation.district}", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                    }
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Flag, contentDescription = null, tint = HarvestGold)
                        Column {
                            Text("Drop-off: ${donation.smartRelayMatch.recipientOrgName}", style = MaterialTheme.typography.labelLarge, color = DarkText)
                            Text("${donation.smartRelayMatch.recipientDistrict} • ${donation.smartRelayMatch.distanceKm} km route", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                    }
                    HorizontalDivider(color = CreamCardBorder)
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.HealthAndSafety, contentDescription = null, tint = ForestGreen)
                        Text(
                            text = donation.category.safetyNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestGreen
                        )
                    }
                }
            }

            // Progressive Volunteer Actions
            when (donation.status) {
                DonationStatus.MATCHING -> {
                    Button(
                        onClick = onAcceptTask,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("accept_task_button")
                    ) {
                        Text("Accept Collection Task")
                    }
                }
                DonationStatus.VOLUNTEER_ASSIGNED -> {
                    Button(
                        onClick = { modalMode = "pickup" },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("verify_pickup_button")
                    ) {
                        Icon(Icons.Outlined.QrCode2, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify Pickup (QR / PIN ${donation.handoverPin})")
                    }
                }
                DonationStatus.PICKED_UP -> {
                    Button(
                        onClick = { modalMode = "delivery" },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HarvestGold,
                            contentColor = DarkText
                        ),
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("confirm_delivery_button")
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm Delivery (QR / PIN ${donation.handoverPin})")
                    }
                }
                DonationStatus.DELIVERED_VERIFIED -> {
                    PipSpeechBubble(
                        text = "Delivery confirmed! Thank you for bridging kindness across Colombo — a blossom grew in your Harvest Garden.",
                        reducedMotion = reducedMotion
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun VolunteerImpactHistoryScreen(
    donations: List<FoodDonation>,
    impact: ImpactSummary,
    lowDataMode: Boolean,
    onTaskClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayedRoutes = remember(donations) {
        donations.filter { it.status == DonationStatus.DELIVERED_VERIFIED }.ifEmpty { donations }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "volunteer_impact_summary", contentType = "impact_summary") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SoftWhite,
                border = BorderStroke(1.dp, CreamCardBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Volunteer Impact Summary", style = MaterialTheme.typography.titleMedium, color = DarkText)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${impact.deliveriesCompleted}", style = MaterialTheme.typography.headlineSmall, color = ForestGreen)
                            Text("Verified Deliveries", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                        Column {
                            Text("${"%.1f".format(impact.measuredWeightKg)} kg", style = MaterialTheme.typography.headlineSmall, color = ForestGreen)
                            Text("Measured Weight", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                        Column {
                            Text("~${impact.estimatedMealsSupported}", style = MaterialTheme.typography.headlineSmall, color = HarvestGold)
                            Text("Est. Meals", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                    }
                }
            }
        }

        item(key = "routes_header", contentType = "section_header") {
            Text("Completed & Active Routes", style = MaterialTheme.typography.titleMedium, color = DarkText)
        }

        items(displayedRoutes, key = { it.id }, contentType = { "opportunity_card" }) { item ->
            VolunteerOpportunityCard(
                donation = item,
                lowDataMode = lowDataMode,
                onClick = { onTaskClick(item.id) }
            )
        }
    }
}
