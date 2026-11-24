package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.model.DonationStatus
import com.example.model.FoodCategory
import com.example.model.FoodDonation
import com.example.model.normalizeSriLankaDistrict
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun RecipientDashboardScreen(
    donations: List<FoodDonation>,
    language: AppLanguage,
    lowDataMode: Boolean,
    reducedMotion: Boolean,
    onBrowseFoodClick: () -> Unit,
    onFoodClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = stringsFor(language)
    val activeRequests = remember(donations) {
        donations.filter { it.requestedByRecipient && it.status != DonationStatus.DELIVERED_VERIFIED }
    }
    val availableFood = remember(donations) {
        donations.filter { it.status != DonationStatus.DELIVERED_VERIFIED }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "recipient_hero", contentType = "recipient_hero") {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ForestGreen
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(shape = RoundedCornerShape(50), color = SoftWhite.copy(alpha = 0.16f)) {
                        Text(
                            text = "Verified Food Support",
                            style = MaterialTheme.typography.labelMedium,
                            color = HarvestGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = copy.availableFoodTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = SoftWhite
                    )
                    Text(
                        text = "${availableFood.size} fresh offerings nearby with verified hygiene labels and volunteer delivery support.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmCream.copy(alpha = 0.9f)
                    )
                    Button(
                        onClick = onBrowseFoodClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HarvestGold,
                            contentColor = DarkText
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("recipient_browse_cta")
                    ) {
                        Text("Browse Suitable Food")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }

        if (activeRequests.isNotEmpty()) {
            item(key = "incoming_header", contentType = "section_header") {
                Text("Incoming Deliveries", style = MaterialTheme.typography.titleMedium, color = DarkText)
            }
            items(activeRequests, key = { "req_${it.id}" }, contentType = { "incoming_request" }) { req ->
                Surface(
                    onClick = { onFoodClick(req.id) },
                    shape = RoundedCornerShape(20.dp),
                    color = GoldTint,
                    border = BorderStroke(1.dp, HarvestGold)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(req.title, style = MaterialTheme.typography.titleSmall, color = DarkText)
                            Text(
                                text = "${req.requestedPortions} portions requested • Courier: ${req.assignedVolunteerName ?: "Assigning"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DeepGreen
                            )
                        }
                        StatusPill(status = req.status)
                    }
                }
            }
        }

        item(key = "recipient_pip_tip", contentType = "pip_bubble") {
            PipSpeechBubble(
                text = "All food listings include preparation notes, allergen tags, and dietary details. Tap any item to request portions safely.",
                reducedMotion = reducedMotion
            )
        }

        item(key = "recommended_header", contentType = "section_header") {
            Text("Recommended for Your Kitchen", style = MaterialTheme.typography.titleMedium, color = DarkText)
        }

        items(availableFood.take(3), key = { "rec_${it.id}" }, contentType = { "recipient_food_card" }) { food ->
            RecipientFoodCard(
                donation = food,
                lowDataMode = lowDataMode,
                onClick = { onFoodClick(food.id) }
            )
        }
    }
}

@Composable
fun RecipientAvailableFoodScreen(
    donations: List<FoodDonation>,
    userDistrict: String,
    selectedMapDistricts: Set<String>,
    lowDataMode: Boolean,
    reducedMotion: Boolean,
    onToggleMapDistrict: (String) -> Unit,
    onApplyMapDistricts: (Set<String>) -> Unit,
    onClearMapDistricts: () -> Unit,
    onFoodClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<FoodCategory?>(null) }
    var dietaryFilter by remember { mutableStateOf<String?>(null) }
    var showMapView by remember { mutableStateOf(false) }
    var selectedPinId by remember { mutableStateOf(donations.firstOrNull()?.id) }

    val available = remember(donations, selectedCategory, dietaryFilter, selectedMapDistricts) {
        donations.filter { item ->
            item.status != DonationStatus.DELIVERED_VERIFIED &&
                (selectedCategory == null || item.category == selectedCategory) &&
                (dietaryFilter == null || item.dietaryTags.any { it.contains(dietaryFilter!!, ignoreCase = true) }) &&
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
        item(key = "filter_chips", contentType = "filter_chips") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null && dietaryFilter == null,
                        onClick = {
                            selectedCategory = null
                            dietaryFilter = null
                        },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = dietaryFilter == "Vegetarian",
                        onClick = { dietaryFilter = if (dietaryFilter == "Vegetarian") null else "Vegetarian" },
                        label = { Text("Vegetarian") }
                    )
                    FilterChip(
                        selected = dietaryFilter == "Halal",
                        onClick = { dietaryFilter = if (dietaryFilter == "Halal") null else "Halal" },
                        label = { Text("Halal") }
                    )
                    FoodCategory.entries.take(3).forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat.label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    onClick = { showMapView = !showMapView },
                    shape = RoundedCornerShape(14.dp),
                    color = ForestGreen,
                    modifier = Modifier.testTag("recipient_toggle_map_button")
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

        item(key = "recipient_district_filter", contentType = "district_filter") {
            DistrictMapFilterBar(
                donations = donations,
                userDistrict = userDistrict,
                selectedDistricts = selectedMapDistricts,
                onToggleDistrict = onToggleMapDistrict,
                onApplyDistricts = onApplyMapDistricts,
                onClearDistricts = onClearMapDistricts
            )
        }

        if (showMapView) {
            item(key = "recipient_google_map", contentType = "district_map") {
                DistrictDonationsGoogleMap(
                    donations = available,
                    selectedDistricts = selectedMapDistricts,
                    selectedDonationId = selectedPinId,
                    onSelectDonation = onPinSelected,
                    reducedMotion = reducedMotion
                )
            }
            if (pinnedDonation != null) {
                item(key = "recipient_pinned_${pinnedDonation.id}", contentType = "recipient_food_card") {
                    RecipientFoodCard(
                        donation = pinnedDonation,
                        lowDataMode = lowDataMode,
                        onClick = { onFoodClick(pinnedDonation.id) }
                    )
                }
            } else {
                item(key = "empty_recipient_map", contentType = "empty_state") {
                    EmptyBotanicalState(
                        title = "No matching food in selected districts",
                        message = "Try resetting dietary or district filters to view active donations across Sri Lanka.",
                        reducedMotion = reducedMotion,
                        actionLabel = "Reset Filters",
                        onAction = {
                            selectedCategory = null
                            dietaryFilter = null
                            onClearMapDistricts()
                        }
                    )
                }
            }
        } else {
            if (available.isEmpty()) {
                item(key = "empty_food", contentType = "empty_state") {
                    EmptyBotanicalState(
                        title = "No matching food right now",
                        message = "Try clearing your filters or ask Pip about upcoming listings.",
                        reducedMotion = reducedMotion,
                        actionLabel = "Reset Filters",
                        onAction = {
                            selectedCategory = null
                            dietaryFilter = null
                            onClearMapDistricts()
                        }
                    )
                }
            } else {
                items(available, key = { it.id }, contentType = { "recipient_food_card" }) { item ->
                    RecipientFoodCard(
                        donation = item,
                        lowDataMode = lowDataMode,
                        onClick = { onFoodClick(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun RecipientFoodCard(
    donation: FoodDonation,
    lowDataMode: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = SoftWhite,
        border = BorderStroke(1.dp, CreamCardBorder),
        modifier = Modifier.fillMaxWidth().testTag("recipient_food_${donation.id}")
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
                    modifier = Modifier.size(76.dp).clip(RoundedCornerShape(16.dp))
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(50), color = SageTint) {
                        Text(
                            text = donation.category.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = ForestGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = "${donation.servingsEstimate} portions",
                        style = MaterialTheme.typography.labelMedium,
                        color = DeepGreen
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
                    text = "${donation.district} • ${donation.pickupWindow}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
                Text(
                    text = donation.dietaryTags.joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = ForestGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RecipientFoodDetailScreen(
    donation: FoodDonation,
    reducedMotion: Boolean,
    onBack: () -> Unit,
    onRequestFood: (Int) -> Unit,
    onConfirmReceipt: () -> Unit
) {
    var portionCount by remember(donation.id) { mutableIntStateOf(donation.servingsEstimate.coerceAtMost(20)) }
    var showReceiptModal by remember { mutableStateOf(false) }

    if (showReceiptModal) {
        QrPinHandoverDialog(
            title = "Confirm Safe Receipt",
            subtitle = "Confirm safe delivery of \"${donation.title}\" (${donation.requestedPortions.coerceAtLeast(portionCount)} portions).",
            expectedPin = donation.handoverPin,
            qrPayload = donation.qrPayload,
            onDismiss = { showReceiptModal = false },
            onVerified = {
                showReceiptModal = false
                onConfirmReceipt()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCream)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        SubScreenTopBar(title = "Food Details", onBack = onBack)

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
                            .height(190.dp)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    )
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(status = donation.status)
                        Text(donation.title, style = MaterialTheme.typography.headlineSmall, color = DarkText)
                        Text(
                            text = "Offered by ${donation.donorName} (${donation.district})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedText
                        )
                        val valueSuffix = donation.estimatedValueLkr?.takeIf { it > 0 }?.let { " • Est. Value LKR $it" }.orEmpty()
                        Text(
                            text = "${donation.servingsEstimate} est. portions (${donation.quantityKg} kg)$valueSuffix",
                            style = MaterialTheme.typography.labelLarge,
                            color = ForestGreen
                        )
                    }
                }
            }

            // Food Safety & Dietary Transparency
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SoftWhite,
                border = BorderStroke(1.dp, CreamCardBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Safety & Dietary Information", style = MaterialTheme.typography.titleMedium, color = DarkText)
                    Text(donation.expiryNote, style = MaterialTheme.typography.bodyMedium, color = DarkText)
                    Text(donation.category.safetyNote, style = MaterialTheme.typography.bodySmall, color = ForestGreen)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        donation.dietaryTags.forEach { tag ->
                            Surface(shape = RoundedCornerShape(50), color = SageTint) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepGreen,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!donation.requestedByRecipient && donation.status != DonationStatus.DELIVERED_VERIFIED) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = SoftWhite,
                    border = BorderStroke(1.dp, CreamCardBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Select Portions Needed", style = MaterialTheme.typography.titleMedium, color = DarkText)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { if (portionCount > 5) portionCount -= 5 },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("-5")
                            }
                            Text(
                                text = "$portionCount portions",
                                style = MaterialTheme.typography.headlineSmall,
                                color = ForestGreen
                            )
                            OutlinedButton(
                                onClick = { if (portionCount + 5 <= donation.servingsEstimate) portionCount += 5 },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("+5")
                            }
                        }

                        Button(
                            onClick = { onRequestFood(portionCount) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("request_food_button")
                        ) {
                            Text("Request $portionCount Portions")
                        }
                    }
                }
            } else if (donation.status != DonationStatus.DELIVERED_VERIFIED) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = GoldTint,
                    border = BorderStroke(1.dp, HarvestGold)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Incoming Delivery Tracked", style = MaterialTheme.typography.titleMedium, color = DarkText)
                        Text(
                            text = "Volunteer ${donation.assignedVolunteerName ?: donation.smartRelayMatch.suggestedVolunteerName} is coordinating your ${donation.requestedPortions} portions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepGreen
                        )
                        Button(
                            onClick = { showReceiptModal = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("confirm_receipt_button")
                        ) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirm Receipt (PIN ${donation.handoverPin})")
                        }
                    }
                }
            } else {
                PipSpeechBubble(
                    text = "Receipt confirmed! Wishing warmth and nourishment to everyone in your community.",
                    reducedMotion = reducedMotion
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun RecipientRequestsHistoryScreen(
    donations: List<FoodDonation>,
    lowDataMode: Boolean,
    reducedMotion: Boolean,
    onFoodClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val requests = remember(donations) {
        donations.filter { it.requestedByRecipient || it.status == DonationStatus.DELIVERED_VERIFIED }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "requests_header", contentType = "section_header") {
            Text("Request & Receipt History", style = MaterialTheme.typography.titleMedium, color = DarkText)
        }
        if (requests.isEmpty()) {
            item(key = "empty_requests", contentType = "empty_state") {
                EmptyBotanicalState(
                    title = "No requests yet",
                    message = "Browse available food near you to submit a dignified food request.",
                    reducedMotion = reducedMotion
                )
            }
        } else {
            items(requests, key = { it.id }, contentType = { "recipient_food_card" }) { item ->
                RecipientFoodCard(
                    donation = item,
                    lowDataMode = lowDataMode,
                    onClick = { onFoodClick(item.id) }
                )
            }
        }
    }
}
