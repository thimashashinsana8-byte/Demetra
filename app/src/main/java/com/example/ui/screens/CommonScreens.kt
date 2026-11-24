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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.UserSettings
import com.example.i18n.AppLanguage
import com.example.model.AppNotification
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.components.DemetraLogoMark
import com.example.ui.components.SriLankaDistrictPickerSheet
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.*

@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCream)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        SubScreenTopBar(
            title = "Notifications",
            onBack = onBack,
            trailing = {
                TextButton(onClick = onMarkAllRead) {
                    Text("Mark read", color = ForestGreen)
                }
            }
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(notifications, key = { it.id }, contentType = { "notification" }) { note ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (note.isRead) SoftWhite else SageTint,
                    border = BorderStroke(1.dp, CreamCardBorder)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (note.isRead) WarmCream else ForestGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = if (note.isRead) MutedText else HarvestGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DarkText,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = note.timeAgo,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedText
                                )
                            }
                            Text(
                                text = note.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedText
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    role: UserRole,
    profile: UserProfile,
    settings: UserSettings,
    onRoleSwitch: (UserRole) -> Unit,
    onDistrictChange: (String) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onReducedMotionChange: (Boolean) -> Unit,
    onLowDataModeChange: (Boolean) -> Unit,
    onOpenVolunteerRegistration: (() -> Unit)? = null,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showDistrictPicker by remember { mutableStateOf(false) }

    if (showDistrictPicker) {
        SriLankaDistrictPickerSheet(
            selectedDistrict = profile.district,
            onDistrictSelected = { onDistrictChange(it) },
            onDismiss = { showDistrictPicker = false }
        )
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            containerColor = SoftWhite,
            title = { Text("Log out of Demetra?", color = DarkText) },
            text = {
                Text(
                    text = "You will return to the sign-in screen and can choose another role or sign back in anytime.",
                    color = MutedText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorTerracotta),
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel", color = ForestGreen)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Header Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = SoftWhite,
            border = BorderStroke(1.dp, CreamCardBorder)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DemetraLogoMark(size = 64.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(profile.fullName, style = MaterialTheme.typography.titleMedium, color = DarkText)
                    Text(profile.organization, style = MaterialTheme.typography.bodySmall, color = ForestGreen)
                    Text("${profile.district} • ${profile.phone}", style = MaterialTheme.typography.bodySmall, color = MutedText)
                }
                Surface(
                    onClick = { showDistrictPicker = true },
                    shape = RoundedCornerShape(50),
                    color = SageTint,
                    border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.35f)),
                    modifier = Modifier.testTag("settings_change_district_button")
                ) {
                    Text(
                        text = "Change District",
                        style = MaterialTheme.typography.labelMedium,
                        color = ForestGreen,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Switch User Role for Multi-Journey Testing
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = SoftWhite,
            border = BorderStroke(1.dp, CreamCardBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Active Persona (Role Switcher)", style = MaterialTheme.typography.titleMedium, color = DarkText)
                Text(
                    text = "Explore Donor, Volunteer, and Recipient journeys seamlessly from one prototype.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UserRole.entries.forEach { item ->
                        val active = item == role
                        Surface(
                            onClick = { onRoleSwitch(item) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (active) ForestGreen else WarmCream,
                            border = BorderStroke(1.dp, if (active) ForestGreen else CreamCardBorder),
                            modifier = Modifier.weight(1f).testTag("switch_role_${item.name.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) SoftWhite else DarkText
                                )
                            }
                        }
                    }
                }
            }
        }

        if (onOpenVolunteerRegistration != null) {
            Surface(
                onClick = onOpenVolunteerRegistration,
                shape = RoundedCornerShape(22.dp),
                color = SageTint,
                border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth().testTag("settings_volunteer_registration_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(CircleShape).background(SoftWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.HowToReg, contentDescription = "Volunteer Registration", tint = ForestGreen)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Volunteer Registration & Interests",
                            style = MaterialTheme.typography.titleSmall,
                            color = DarkText
                        )
                        Text(
                            text = "Set interest areas, preferred district & transport capacity in Firestore",
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestGreen
                        )
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = ForestGreen)
                }
            }
        }

        // Multilingual Accessibility (English, Sinhala, Tamil)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = SoftWhite,
            border = BorderStroke(1.dp, CreamCardBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Language • භාෂාව • மொழி", style = MaterialTheme.typography.titleMedium, color = DarkText)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        val active = lang == settings.language
                        Surface(
                            onClick = { onLanguageChange(lang) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (active) ForestGreen else WarmCream,
                            border = BorderStroke(1.dp, if (active) ForestGreen else CreamCardBorder),
                            modifier = Modifier.weight(1f).testTag("settings_lang_${lang.code}")
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lang.nativeLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (active) SoftWhite else DarkText
                                )
                            }
                        }
                    }
                }
            }
        }

        // Accessibility & Low-Data Preferences
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = SoftWhite,
            border = BorderStroke(1.dp, CreamCardBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Accessibility & Low-Data", style = MaterialTheme.typography.titleMedium, color = DarkText)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reduced Motion", style = MaterialTheme.typography.bodyLarge, color = DarkText)
                        Text("Disables floating & flower sway animations", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    }
                    Switch(
                        checked = settings.reducedMotion,
                        onCheckedChange = onReducedMotionChange,
                        colors = SwitchDefaults.colors(checkedTrackColor = ForestGreen)
                    )
                }
                HorizontalDivider(color = CreamCardBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Low-Data Friendly Mode", style = MaterialTheme.typography.bodyLarge, color = DarkText)
                        Text("Hides heavy listing photos for faster mobile data", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    }
                    Switch(
                        checked = settings.lowDataMode,
                        onCheckedChange = onLowDataModeChange,
                        colors = SwitchDefaults.colors(checkedTrackColor = ForestGreen)
                    )
                }
            }
        }

        // Honest Prototype Disclosure
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = GoldTint,
            border = BorderStroke(1.dp, HarvestGold.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = DeepGreen)
                Text(
                    text = "Cloud & AI Disclosure: Account profiles, donors, volunteers, listings, and food requests sync with Cloud Firestore via Google Sign-In, while Pip AI Assistant runs on Firebase AI Logic (Gemini).",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkText
                )
            }
        }

        // Log Out Action
        OutlinedButton(
            onClick = { showLogoutConfirm = true },
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, ErrorTerracotta),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("logout_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = "Log out",
                tint = ErrorTerracotta
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Log Out",
                style = MaterialTheme.typography.titleSmall,
                color = ErrorTerracotta
            )
        }

        Spacer(modifier = Modifier.height(84.dp))
    }
}
