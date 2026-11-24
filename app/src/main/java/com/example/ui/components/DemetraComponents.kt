package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.DonationStatus
import com.example.model.SRI_LANKA_DISTRICTS
import com.example.model.UserRole
import com.example.model.findSriLankaDistrict
import com.example.ui.theme.*

/**
 * Offloads image decoding and downsampling to Coil's background IO pool and caches bitmaps
 * sized cleanly to the target display bounds so LazyColumns scroll without UI-thread frame drops.
 */
@Composable
fun DemetraAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    targetSizePx: Int = 256,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val request = remember(context, model, targetSizePx) {
        ImageRequest.Builder(context)
            .data(model)
            .size(targetSizePx)
            .allowHardware(true)
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}

@Composable
fun DemetraLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    val targetPx = with(LocalDensity.current) { size.roundToPx().coerceAtLeast(96) }
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(24.dp),
        color = WarmCream,
        border = BorderStroke(1.5.dp, CreamCardBorder),
        shadowElevation = 2.dp
    ) {
        DemetraAsyncImage(
            model = R.drawable.img_demetra_logo,
            contentDescription = "Demetra goddess and wheat crest logo",
            targetSizePx = targetPx,
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun DemetraTopHeader(
    title: String,
    subtitle: String,
    role: UserRole,
    unreadNotificationsCount: Int,
    onNotificationsClick: () -> Unit,
    onRoleBadgeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onRoleBadgeClick,
                    shape = RoundedCornerShape(50),
                    color = SageTint,
                    modifier = Modifier.testTag("role_badge_button")
                ) {
                    Text(
                        text = role.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = ForestGreen,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = DarkText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(contentAlignment = Alignment.TopEnd) {
            Surface(
                onClick = onNotificationsClick,
                shape = CircleShape,
                color = SoftWhite,
                border = BorderStroke(1.dp, CreamCardBorder),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("notifications_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = ForestGreen
                    )
                }
            }
            if (unreadNotificationsCount > 0) {
                Box(
                    modifier = Modifier
                        .offset(x = (-2).dp, y = 2.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(HarvestGold),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = unreadNotificationsCount.coerceAtMost(9).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SubScreenTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = DarkText
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = DarkText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun StatusPill(status: DonationStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (status) {
        DonationStatus.MATCHING -> GoldTint to DeepGreen
        DonationStatus.VOLUNTEER_ASSIGNED -> SageTint to ForestGreen
        DonationStatus.PICKED_UP -> Color(0xFFE5F0EB) to DeepGreen
        DonationStatus.DELIVERED_VERIFIED -> SuccessGreen.copy(alpha = 0.14f) to SuccessGreen
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bgColor
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun MetricStatTile(
    label: String,
    value: String,
    badge: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accentColor: Color = ForestGreen
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = SoftWhite,
        border = BorderStroke(1.dp, CreamCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = WarmCream
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = DarkText
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
        }
    }
}

@Composable
fun FloatingPipFab(
    onClick: () -> Unit,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = ForestGreen,
        shadowElevation = 6.dp,
        border = BorderStroke(2.dp, HarvestGold),
        modifier = modifier
            .size(60.dp)
            .testTag("pip_fab")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(WarmCream),
                contentAlignment = Alignment.Center
            ) {
                PipMascot(size = 42.dp, reducedMotion = reducedMotion, animate = false)
            }
        }
    }
}

@Composable
fun EmptyBotanicalState(
    title: String,
    message: String,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SoftWhite,
        border = BorderStroke(1.dp, CreamCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(SageTint),
                contentAlignment = Alignment.Center
            ) {
                PipMascot(size = 56.dp, reducedMotion = reducedMotion, animate = false)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = DarkText
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun DistrictSelectorField(
    selectedDistrict: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "District (25 Sri Lankan Districts)"
) {
    val matchedProvince = remember(selectedDistrict) {
        findSriLankaDistrict(selectedDistrict)?.province ?: "Sri Lanka"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = WarmCream.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = SageTint,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = "Selected district",
                        tint = ForestGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText
                )
                Text(
                    text = selectedDistrict.ifBlank { "Choose District" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkText
                )
                Text(
                    text = matchedProvince,
                    style = MaterialTheme.typography.labelSmall,
                    color = ForestGreen
                )
            }
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = "Choose district",
                tint = ForestGreen
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SriLankaDistrictPickerSheet(
    selectedDistrict: String,
    onDistrictSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredDistricts = remember(searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            SRI_LANKA_DISTRICTS
        } else {
            SRI_LANKA_DISTRICTS.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.province.contains(q, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SoftCreamSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Select Your District",
                        style = MaterialTheme.typography.titleLarge,
                        color = DarkText
                    )
                    Text(
                        text = "All 25 administrative districts of Sri Lanka",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close district picker",
                        tint = MutedText
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search district or province (e.g. Kandy, Western)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = ForestGreen
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear search",
                                tint = MutedText
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("district_search_input")
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                if (filteredDistricts.isEmpty()) {
                    item(key = "empty_district_search", contentType = "empty") {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = SoftWhite,
                            border = BorderStroke(1.dp, CreamCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No Sri Lankan district matched \"$searchQuery\". Try searching by district or province name.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedText,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(
                        items = filteredDistricts,
                        key = { it.name },
                        contentType = { "district_row" }
                    ) { district ->
                        val isSelected = selectedDistrict.equals(district.name, ignoreCase = true) ||
                            selectedDistrict.startsWith("${district.name} ", ignoreCase = true)
                        Surface(
                            onClick = {
                                onDistrictSelected(district.name)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) SageTint else SoftWhite,
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) ForestGreen else CreamCardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .testTag("district_option_${district.name.lowercase().replace(" ", "_")}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationOn,
                                        contentDescription = null,
                                        tint = if (isSelected) ForestGreen else MutedText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = district.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = DarkText
                                        )
                                        Text(
                                            text = district.province,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) ForestGreen else MutedText
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = ForestGreen
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SriLankaMultiDistrictPickerSheet(
    selectedDistricts: Set<String>,
    activeCountByDistrict: Map<String, Int>,
    onApplySelection: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var draftSelection by remember(selectedDistricts) { mutableStateOf(selectedDistricts) }

    val filteredDistricts = remember(searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            SRI_LANKA_DISTRICTS
        } else {
            SRI_LANKA_DISTRICTS.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.province.contains(q, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SoftCreamSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Filter Map by Districts",
                        style = MaterialTheme.typography.titleLarge,
                        color = DarkText
                    )
                    Text(
                        text = if (draftSelection.isEmpty()) {
                            "Showing all active districts across Sri Lanka"
                        } else {
                            "${draftSelection.size} district(s) selected"
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
                        contentDescription = "Close district filter",
                        tint = MutedText
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { draftSelection = emptySet() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text("All Districts")
                }
                OutlinedButton(
                    onClick = {
                        draftSelection = activeCountByDistrict.filterValues { it > 0 }.keys
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text("Only Active (${activeCountByDistrict.count { it.value > 0 }})")
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search 25 districts or provinces...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = ForestGreen
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear search",
                                tint = MutedText
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("multi_district_search_input")
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filteredDistricts,
                    key = { it.name },
                    contentType = { "multi_district_row" }
                ) { district ->
                    val isSelected = district.name in draftSelection
                    val activeCount = activeCountByDistrict[district.name] ?: 0
                    Surface(
                        onClick = {
                            draftSelection = if (isSelected) {
                                draftSelection - district.name
                            } else {
                                draftSelection + district.name
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) SageTint else SoftWhite,
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) ForestGreen else CreamCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag("multi_district_option_${district.name.lowercase().replace(" ", "_")}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = ForestGreen)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = district.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = DarkText
                                )
                                Text(
                                    text = district.province,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedText
                                )
                            }
                            if (activeCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = GoldTint
                                ) {
                                    Text(
                                        text = "$activeCount active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DeepGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onApplySelection(draftSelection)
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("apply_multi_district_filter_button")
            ) {
                Text(
                    text = if (draftSelection.isEmpty()) {
                        "Show All Active Districts"
                    } else {
                        "Apply ${draftSelection.size} District(s)"
                    }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}


