package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.model.DonationStatus
import com.example.model.FoodDonation
import com.example.model.UrgencyLevel
import com.example.model.findSriLankaDistrict
import com.example.model.normalizeSriLankaDistrict
import com.example.ui.theme.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val SRI_LANKA_CENTER = LatLng(7.8731, 80.7718)
private val ROUTE_POLYLINE_PATTERN = listOf(Dash(22f), Gap(14f))

@Composable
fun DistrictMapFilterBar(
    donations: List<FoodDonation>,
    userDistrict: String,
    selectedDistricts: Set<String>,
    onToggleDistrict: (String) -> Unit,
    onApplyDistricts: (Set<String>) -> Unit,
    onClearDistricts: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMultiPicker by remember { mutableStateOf(false) }

    val activeCountByDistrict = remember(donations) {
        donations
            .filter { it.status != DonationStatus.DELIVERED_VERIFIED }
            .groupingBy { normalizeSriLankaDistrict(it.district) }
            .eachCount()
    }

    val quickDistricts = remember(activeCountByDistrict, userDistrict, selectedDistricts) {
        val normalizedHome = normalizeSriLankaDistrict(userDistrict)
        val ordered = linkedSetOf(normalizedHome)
        activeCountByDistrict.entries
            .sortedByDescending { it.value }
            .forEach { ordered.add(it.key) }
        selectedDistricts.forEach { ordered.add(it) }
        ordered.toList().take(6)
    }

    if (showMultiPicker) {
        SriLankaMultiDistrictPickerSheet(
            selectedDistricts = selectedDistricts,
            activeCountByDistrict = activeCountByDistrict,
            onApplySelection = onApplyDistricts,
            onDismiss = { showMultiPicker = false }
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedDistricts.isEmpty(),
            onClick = onClearDistricts,
            label = {
                val totalActive = activeCountByDistrict.values.sum()
                Text("All Active ($totalActive)")
            },
            modifier = Modifier.testTag("map_district_all")
        )

        quickDistricts.forEach { districtName ->
            val count = activeCountByDistrict[districtName] ?: 0
            val selected = districtName in selectedDistricts
            FilterChip(
                selected = selected,
                onClick = { onToggleDistrict(districtName) },
                label = {
                    Text(if (count > 0) "$districtName ($count)" else districtName)
                },
                modifier = Modifier.testTag("map_district_${districtName.lowercase().replace(" ", "_")}")
            )
        }

        AssistChip(
            onClick = { showMultiPicker = true },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = ForestGreen,
                    modifier = Modifier.size(16.dp)
                )
            },
            label = {
                Text(
                    if (selectedDistricts.isEmpty()) "25 Districts"
                    else "25 Districts (${selectedDistricts.size})"
                )
            },
            modifier = Modifier.testTag("open_multi_district_picker")
        )
    }
}

@Composable
fun DistrictDonationsGoogleMap(
    donations: List<FoodDonation>,
    selectedDistricts: Set<String>,
    selectedDonationId: String?,
    onSelectDonation: (FoodDonation) -> Unit,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val activeDonations = remember(donations) {
        donations.filter { it.status != DonationStatus.DELIVERED_VERIFIED }
    }
    val selected = remember(activeDonations, selectedDonationId) {
        activeDonations.find { it.id == selectedDonationId } ?: activeDonations.firstOrNull()
    }

    val hasConfiguredMapsKey = remember {
        val key = BuildConfig.MAPS_API_KEY.trim()
        key.isNotEmpty() &&
            key != "YOUR_GOOGLE_MAPS_API_KEY" &&
            !key.startsWith("MAPS_API_KEY_DEFAULT")
    }

    if (hasConfiguredMapsKey) {
        LiveGoogleDistrictMap(
            donations = activeDonations,
            selectedDistricts = selectedDistricts,
            selectedDonation = selected,
            onSelectDonation = onSelectDonation,
            reducedMotion = reducedMotion,
            modifier = modifier
        )
    } else {
        SriLankaDistrictPreviewMap(
            donations = activeDonations,
            selectedDistricts = selectedDistricts,
            selectedDonation = selected,
            onSelectDonation = onSelectDonation,
            modifier = modifier
        )
    }
}

@Composable
private fun LiveGoogleDistrictMap(
    donations: List<FoodDonation>,
    selectedDistricts: Set<String>,
    selectedDonation: FoodDonation?,
    onSelectDonation: (FoodDonation) -> Unit,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val initialCenter = remember(donations, selectedDistricts) {
        computeFallbackCameraCenter(donations, selectedDistricts)
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialCenter, if (donations.size <= 1) 12.5f else 8.2f)
    }
    val scope = rememberCoroutineScope()

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = true,
            compassEnabled = true,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false
        )
    }
    val mapProperties = remember {
        MapProperties(isMyLocationEnabled = false)
    }
    var isMapLoaded by remember { mutableStateOf(false) }
    var mountMapView by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!reducedMotion) delay(140L)
        mountMapView = true
    }

    val cameraScopeKey = remember(donations, selectedDistricts) {
        donations.map { Triple(it.id, it.latitude, it.longitude) } to selectedDistricts
    }

    suspend fun focusCameraToScope() {
        if (!isMapLoaded) return
        val points = buildList {
            donations.forEach { d ->
                add(LatLng(d.latitude, d.longitude))
                add(LatLng(d.smartRelayMatch.recipientLatitude, d.smartRelayMatch.recipientLongitude))
            }
            if (isEmpty()) {
                selectedDistricts.forEach { name ->
                    findSriLankaDistrict(name)?.let {
                        add(LatLng(it.latitude, it.longitude))
                    }
                }
            }
        }
        if (points.isEmpty()) {
            val update = CameraUpdateFactory.newLatLngZoom(SRI_LANKA_CENTER, 7.6f)
            if (reducedMotion) cameraPositionState.move(update) else cameraPositionState.animate(update, 450)
            return
        }
        if (points.size == 1) {
            val update = CameraUpdateFactory.newLatLngZoom(points.first(), 13.2f)
            if (reducedMotion) cameraPositionState.move(update) else cameraPositionState.animate(update, 450)
            return
        }
        val boundsBuilder = LatLngBounds.builder()
        points.forEach { boundsBuilder.include(it) }
        val bounds = boundsBuilder.build()
        val spanLat = kotlin.math.abs(bounds.northeast.latitude - bounds.southwest.latitude)
        val spanLng = kotlin.math.abs(bounds.northeast.longitude - bounds.southwest.longitude)
        val update = if (spanLat < 0.01 && spanLng < 0.01) {
            CameraUpdateFactory.newLatLngZoom(bounds.center, 13.5f)
        } else {
            CameraUpdateFactory.newLatLngBounds(bounds, 96)
        }
        if (reducedMotion) cameraPositionState.move(update) else cameraPositionState.animate(update, 500)
    }

    LaunchedEffect(isMapLoaded, cameraScopeKey) {
        focusCameraToScope()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(310.dp)
            .testTag("google_maps_container"),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, CreamCardBorder),
        color = SageTint
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!mountMapView) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = ForestGreen,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = mapUiSettings,
                    properties = mapProperties,
                    onMapLoaded = { isMapLoaded = true }
                ) {
                    val selectedIcon = remember { BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE) }
                    val urgentIcon = remember { BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE) }
                    val standardIcon = remember { BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN) }
                    val recipientIcon = remember { BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE) }

                donations.forEach { donation ->
                    key(donation.id) {
                        val isSelected = selectedDonation?.id == donation.id
                        val markerState = rememberUpdatedMarkerState(
                            position = LatLng(donation.latitude, donation.longitude)
                        )
                        val markerIcon = when {
                            isSelected -> selectedIcon
                            donation.smartRelayMatch.urgency == UrgencyLevel.URGENT -> urgentIcon
                            else -> standardIcon
                        }
                        Marker(
                            state = markerState,
                            title = donation.title,
                            snippet = "${donation.district} • ${donation.servingsEstimate} portions • ${donation.smartRelayMatch.distanceKm} km",
                            icon = markerIcon,
                            zIndex = if (isSelected) 2f else 1f,
                            onClick = {
                                onSelectDonation(donation)
                                false
                            }
                        )
                    }
                }

                selectedDonation?.let { focused ->
                    val pickupLatLng = LatLng(focused.latitude, focused.longitude)
                    val dropoffLatLng = LatLng(
                        focused.smartRelayMatch.recipientLatitude,
                        focused.smartRelayMatch.recipientLongitude
                    )
                    val recipientState = rememberUpdatedMarkerState(position = dropoffLatLng)
                    Marker(
                        state = recipientState,
                        title = focused.smartRelayMatch.recipientOrgName,
                        snippet = "Matched Kitchen • ${focused.smartRelayMatch.recipientDistrict}",
                        icon = recipientIcon
                    )
                    Polyline(
                        points = listOf(pickupLatLng, dropoffLatLng),
                        color = ForestGreen,
                        width = 8f,
                        pattern = ROUTE_POLYLINE_PATTERN
                    )
                }
                }
            }

            MapStatusOverlay(
                donationsCount = donations.size,
                selectedDistricts = selectedDistricts,
                isPreviewFallback = false,
                onRecenter = { scope.launch { focusCameraToScope() } },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun SriLankaDistrictPreviewMap(
    donations: List<FoodDonation>,
    selectedDistricts: Set<String>,
    selectedDonation: FoodDonation?,
    onSelectDonation: (FoodDonation) -> Unit,
    modifier: Modifier = Modifier
) {
    val bounds = remember(donations, selectedDistricts) {
        val lats = mutableListOf<Double>()
        val lngs = mutableListOf<Double>()
        donations.forEach { d ->
            lats.add(d.latitude)
            lngs.add(d.longitude)
            lats.add(d.smartRelayMatch.recipientLatitude)
            lngs.add(d.smartRelayMatch.recipientLongitude)
        }
        if (lats.isEmpty()) {
            selectedDistricts.forEach { name ->
                findSriLankaDistrict(name)?.let {
                    lats.add(it.latitude)
                    lngs.add(it.longitude)
                }
            }
        }
        if (lats.isEmpty()) {
            // Sri Lanka bounding box fallback
            QuadBounds(5.90, 9.85, 79.65, 81.90)
        } else {
            val minLat = lats.min()
            val maxLat = lats.max()
            val minLng = lngs.min()
            val maxLng = lngs.max()
            val padLat = ((maxLat - minLat) * 0.28).coerceAtLeast(0.03)
            val padLng = ((maxLng - minLng) * 0.28).coerceAtLeast(0.03)
            QuadBounds(minLat - padLat, maxLat + padLat, minLng - padLng, maxLng + padLng)
        }
    }

    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(14f, 10f)) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(310.dp)
            .testTag("google_maps_container"),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, CreamCardBorder),
        color = Color(0xFFEDF2E9)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val mapW = maxWidth
            val mapH = maxHeight

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val w = size.width
                        val h = size.height
                        val coastline = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(w * 0.14f, 0f)
                            quadraticTo(w * 0.10f, h * 0.48f, w * 0.18f, h)
                            lineTo(0f, h)
                            close()
                        }
                        onDrawBehind {
                            drawPath(coastline, color = Color(0xFFD5E6EC))
                            drawOval(
                                color = SageGreen.copy(alpha = 0.24f),
                                topLeft = Offset(w * 0.34f, h * 0.30f),
                                size = androidx.compose.ui.geometry.Size(w * 0.26f, h * 0.22f)
                            )
                            drawLine(
                                color = SoftWhite,
                                start = Offset(w * 0.18f, h * 0.14f),
                                end = Offset(w * 0.36f, h * 0.90f),
                                strokeWidth = 8f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = SoftWhite,
                                start = Offset(w * 0.16f, h * 0.45f),
                                end = Offset(w * 0.88f, h * 0.50f),
                                strokeWidth = 7f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = SoftWhite,
                                start = Offset(w * 0.50f, h * 0.12f),
                                end = Offset(w * 0.58f, h * 0.88f),
                                strokeWidth = 6f,
                                cap = StrokeCap.Round
                            )
                            selectedDonation?.let { focused ->
                                val startX = projectLng(focused.longitude, bounds, w)
                                val startY = projectLat(focused.latitude, bounds, h)
                                val endX = projectLng(focused.smartRelayMatch.recipientLongitude, bounds, w)
                                val endY = projectLat(focused.smartRelayMatch.recipientLatitude, bounds, h)
                                drawLine(
                                    color = ForestGreen,
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = 5f,
                                    pathEffect = dashEffect,
                                    cap = StrokeCap.Round
                                )
                                drawCircle(
                                    color = ErrorTerracotta,
                                    radius = 9f,
                                    center = Offset(endX, endY)
                                )
                            }
                        }
                    }
            )

            MapStatusOverlay(
                donationsCount = donations.size,
                selectedDistricts = selectedDistricts,
                isPreviewFallback = true,
                onRecenter = null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(12.dp)
            )

            val density = LocalDensity.current
            val availWidthPx = with(density) { (mapW - 108.dp).toPx() }
            val availHeightPx = with(density) { (mapH - 56.dp).toPx() }

            donations.forEach { donation ->
                key(donation.id) {
                    val isSelected = donation.id == selectedDonation?.id
                    val pinOffset = remember(
                        bounds,
                        availWidthPx,
                        availHeightPx,
                        donation.longitude,
                        donation.latitude
                    ) {
                        val xFrac = ((donation.longitude - bounds.minLng) / (bounds.maxLng - bounds.minLng)).toFloat().coerceIn(0.06f, 0.88f)
                        val yFrac = (1f - ((donation.latitude - bounds.minLat) / (bounds.maxLat - bounds.minLat)).toFloat()).coerceIn(0.22f, 0.86f)
                        IntOffset((availWidthPx * xFrac).roundToInt(), (availHeightPx * yFrac).roundToInt())
                    }
                    val pinLabel = remember(donation.district, donation.servingsEstimate) {
                        "${normalizeSriLankaDistrict(donation.district)} • ${donation.servingsEstimate}p"
                    }

                    Surface(
                        modifier = Modifier
                            .offset { pinOffset }
                            .clickable { onSelectDonation(donation) }
                            .testTag("map_pin_${donation.id}"),
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) ForestGreen else SoftWhite,
                        border = BorderStroke(1.5.dp, if (isSelected) HarvestGold else ForestGreen),
                        shadowElevation = if (isSelected) 6.dp else 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = donation.district,
                                tint = if (isSelected) HarvestGold else ForestGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = pinLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) SoftWhite else DarkText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapStatusOverlay(
    donationsCount: Int,
    selectedDistricts: Set<String>,
    isPreviewFallback: Boolean,
    onRecenter: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val scopeLabel = if (selectedDistricts.isEmpty()) {
        "All Sri Lanka Districts"
    } else if (selectedDistricts.size <= 2) {
        selectedDistricts.joinToString(", ")
    } else {
        "${selectedDistricts.size} Selected Districts"
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SoftWhite.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, CreamCardBorder),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(
                    text = "$donationsCount active request(s) • $scopeLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = ForestGreen,
                    fontWeight = FontWeight.SemiBold
                )
                if (isPreviewFallback) {
                    Text(
                        text = "Add MAPS_API_KEY in AI Studio Secrets for live Google Map tiles",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText
                    )
                }
            }
        }

        if (onRecenter != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                onClick = onRecenter,
                shape = CircleShape,
                color = SoftWhite.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, CreamCardBorder),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("recenter_google_map_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.CenterFocusStrong,
                        contentDescription = "Fit selected districts on map",
                        tint = ForestGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private data class QuadBounds(
    val minLat: Double,
    val maxLat: Double,
    val minLng: Double,
    val maxLng: Double
)

private fun projectLng(lng: Double, bounds: QuadBounds, width: Float): Float {
    val frac = ((lng - bounds.minLng) / (bounds.maxLng - bounds.minLng)).toFloat().coerceIn(0.06f, 0.94f)
    return width * frac
}

private fun projectLat(lat: Double, bounds: QuadBounds, height: Float): Float {
    val frac = (1f - ((lat - bounds.minLat) / (bounds.maxLat - bounds.minLat)).toFloat()).coerceIn(0.18f, 0.90f)
    return height * frac
}

private fun computeFallbackCameraCenter(
    donations: List<FoodDonation>,
    selectedDistricts: Set<String>
): LatLng {
    val firstDonation = donations.firstOrNull()
    if (firstDonation != null) return LatLng(firstDonation.latitude, firstDonation.longitude)
    val firstDistrict = selectedDistricts.firstOrNull()?.let { findSriLankaDistrict(it) }
    return if (firstDistrict != null) LatLng(firstDistrict.latitude, firstDistrict.longitude) else SRI_LANKA_CENTER
}

@Composable
fun ColomboInteractiveMap(
    donations: List<FoodDonation>,
    selectedDonationId: String?,
    onSelectDonation: (FoodDonation) -> Unit,
    modifier: Modifier = Modifier
) {
    DistrictDonationsGoogleMap(
        donations = donations,
        selectedDistricts = emptySet(),
        selectedDonationId = selectedDonationId,
        onSelectDonation = onSelectDonation,
        modifier = modifier
    )
}
