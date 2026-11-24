package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.auth.GoogleAuthHelper
import com.example.model.UserProfile
import com.example.model.VolunteerEntity
import com.example.model.VolunteerInterestArea
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.VolunteerRegistrationUiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

private val VOLUNTEER_REG_STEP_TITLES = listOf(
    "Basic Info & District",
    "Interest Areas & Transport",
    "Review & Save"
)
private val VEHICLE_OPTIONS = listOf(
    "E-Bike (Insulated Box)",
    "Scooter / Motorbike",
    "Tuk-Tuk / Three-Wheeler",
    "Car / Van",
    "Bicycle / On Foot"
)
private val CAPACITY_OPTIONS = listOf(5.0, 10.0, 15.0, 25.0, 40.0)
private val AVAILABILITY_WINDOW_OPTIONS = listOf(
    "Flexible (Anytime)",
    "Mornings (7 AM - 12 PM)",
    "Afternoons (12 PM - 5 PM)",
    "Evenings (5 PM - 10 PM)",
    "Weekends Only"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VolunteerRegistrationScreen(
    existingVolunteer: VolunteerEntity?,
    userProfile: UserProfile,
    registrationState: VolunteerRegistrationUiState,
    reducedMotion: Boolean,
    onBack: () -> Unit,
    onResetState: () -> Unit,
    onSubmitRegistration: (
        fullName: String,
        phone: String,
        communityGroup: String,
        preferredDistrict: String,
        interestAreas: List<String>,
        vehicleType: String,
        carrierCapacityKg: Double,
        availabilityWindow: String,
        available: Boolean,
        firebaseUser: FirebaseUser?
    ) -> Unit,
    onDoneNavigateHome: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var signedInUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isSigningIn by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    var step by rememberSaveable { mutableIntStateOf(0) }
    var fullName by rememberSaveable {
        mutableStateOf(
            existingVolunteer?.displayName?.takeIf { it.isNotBlank() }
                ?: signedInUser?.displayName?.takeIf { it.isNotBlank() }
                ?: userProfile.fullName
        )
    }
    var phone by rememberSaveable { mutableStateOf(userProfile.phone) }
    var communityGroup by rememberSaveable {
        mutableStateOf(
            existingVolunteer?.communityGroup?.takeIf { it.isNotBlank() }
                ?: userProfile.organization.takeIf { it.isNotBlank() }
                ?: "Colombo Community Riders"
        )
    }
    var preferredDistrict by rememberSaveable {
        mutableStateOf(
            existingVolunteer?.preferredDistrict?.takeIf { it.isNotBlank() }
                ?: userProfile.district.ifBlank { "Colombo" }
        )
    }
    var showDistrictPicker by remember { mutableStateOf(false) }

    var selectedInterestLabels by rememberSaveable {
        mutableStateOf(
            existingVolunteer?.interestAreas?.takeIf { it.isNotEmpty() }
                ?: userProfile.interestAreas.takeIf { it.isNotEmpty() }
                ?: listOf(VolunteerInterestArea.HOT_MEAL_RESCUE.label)
        )
    }
    var vehicleType by rememberSaveable {
        mutableStateOf(existingVolunteer?.vehicleType ?: "E-Bike (Insulated Box)")
    }
    var carrierCapacityKg by rememberSaveable {
        mutableDoubleStateOf(existingVolunteer?.carrierCapacityKg ?: 15.0)
    }
    var availabilityWindow by rememberSaveable {
        mutableStateOf(existingVolunteer?.availabilityWindow ?: "Flexible (Anytime)")
    }
    var availableNow by rememberSaveable {
        mutableStateOf(existingVolunteer?.available ?: true)
    }

    val isSubmitting = registrationState is VolunteerRegistrationUiState.Submitting

    BackHandler {
        if (registrationState is VolunteerRegistrationUiState.Success) {
            onResetState()
            onDoneNavigateHome()
        } else if (step > 0) {
            step -= 1
            localError = null
        } else {
            onResetState()
            onBack()
        }
    }

    if (showDistrictPicker) {
        SriLankaDistrictPickerSheet(
            selectedDistrict = preferredDistrict,
            onDistrictSelected = { picked ->
                preferredDistrict = picked
                localError = null
            },
            onDismiss = { showDistrictPicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCream)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
        ) {
            SubScreenTopBar(
                title = if (existingVolunteer != null) "Volunteer Profile" else "Volunteer Registration",
                onBack = {
                    if (registrationState is VolunteerRegistrationUiState.Success) {
                        onResetState()
                        onDoneNavigateHome()
                    } else if (step > 0) {
                        step -= 1
                        localError = null
                    } else {
                        onResetState()
                        onBack()
                    }
                }
            )

            if (registrationState is VolunteerRegistrationUiState.Success) {
                VolunteerRegistrationSuccessContent(
                    volunteer = registrationState.volunteer,
                    reducedMotion = reducedMotion,
                    onContinue = {
                        onResetState()
                        onDoneNavigateHome()
                    }
                )
                return@Column
            }

            // Progress Bar Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Step ${step + 1} of 3 • ${VOLUNTEER_REG_STEP_TITLES[step]}",
                        style = MaterialTheme.typography.labelLarge,
                        color = ForestGreen
                    )
                    Text(
                        text = "${((step + 1) * 100) / 3}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedText
                    )
                }
                LinearProgressIndicator(
                    progress = { (step + 1) / 3f },
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
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (reducedMotion) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        else fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                    },
                    label = "volunteer_reg_step"
                ) { currentStep ->
                    when (currentStep) {
                        0 -> VolunteerStepBasicInfo(
                            signedInUser = signedInUser,
                            fullName = fullName,
                            onFullNameChange = { fullName = it; localError = null },
                            phone = phone,
                            onPhoneChange = { phone = it; localError = null },
                            communityGroup = communityGroup,
                            onCommunityGroupChange = { communityGroup = it },
                            preferredDistrict = preferredDistrict,
                            onOpenDistrictPicker = { showDistrictPicker = true },
                            reducedMotion = reducedMotion
                        )

                        1 -> VolunteerStepInterestsAndTransport(
                            selectedInterestLabels = selectedInterestLabels,
                            onToggleInterest = { label ->
                                selectedInterestLabels = if (label in selectedInterestLabels) {
                                    selectedInterestLabels - label
                                } else {
                                    (selectedInterestLabels + label).take(6)
                                }
                                localError = null
                            },
                            vehicleType = vehicleType,
                            onSelectVehicle = { vehicleType = it },
                            carrierCapacityKg = carrierCapacityKg,
                            onSelectCapacity = { carrierCapacityKg = it },
                            availabilityWindow = availabilityWindow,
                            onSelectWindow = { availabilityWindow = it },
                            availableNow = availableNow,
                            onAvailableNowChange = { availableNow = it }
                        )

                        else -> VolunteerStepReviewAndSave(
                            signedInUser = signedInUser,
                            isSigningIn = isSigningIn,
                            fullName = fullName,
                            phone = phone,
                            communityGroup = communityGroup,
                            preferredDistrict = preferredDistrict,
                            selectedInterestLabels = selectedInterestLabels,
                            vehicleType = vehicleType,
                            carrierCapacityKg = carrierCapacityKg,
                            availabilityWindow = availabilityWindow,
                            availableNow = availableNow,
                            reducedMotion = reducedMotion,
                            onSignInClick = {
                                if (isSigningIn) return@VolunteerStepReviewAndSave
                                isSigningIn = true
                                localError = null
                                scope.launch {
                                    val result = GoogleAuthHelper.signInWithGoogle(context)
                                    isSigningIn = false
                                    result.onSuccess { user ->
                                        signedInUser = user
                                        if (fullName.isBlank() && !user.displayName.isNullOrBlank()) {
                                            fullName = user.displayName.orEmpty()
                                        }
                                    }.onFailure { err ->
                                        localError = if (err is GetCredentialCancellationException) {
                                            "Google Sign-In was cancelled. Tap 'Sign in with Google' to connect your account."
                                        } else {
                                            err.localizedMessage ?: "Unable to complete Google Sign-In."
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                val activeError = localError
                    ?: (registrationState as? VolunteerRegistrationUiState.Error)?.message
                if (!activeError.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ErrorTerracotta.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, ErrorTerracotta.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth().testTag("volunteer_reg_error_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = "Error",
                                tint = ErrorTerracotta
                            )
                            Text(
                                text = activeError,
                                style = MaterialTheme.typography.bodySmall,
                                color = ErrorTerracotta,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Sticky Bottom Navigation Bar
            Surface(
                color = SoftWhite,
                tonalElevation = 3.dp,
                border = BorderStroke(1.dp, CreamCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 0) {
                        OutlinedButton(
                            onClick = {
                                step -= 1
                                localError = null
                            },
                            enabled = !isSubmitting && !isSigningIn,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CreamCardBorder),
                            modifier = Modifier.height(52.dp).testTag("volunteer_reg_back_step_button")
                        ) {
                            Text("Back", color = DarkText)
                        }
                    }

                    Button(
                        onClick = {
                            when (step) {
                                0 -> {
                                    when {
                                        fullName.trim().length < 2 -> {
                                            localError = "Please enter your full name (at least 2 characters)."
                                        }
                                        phone.trim().length < 7 -> {
                                            localError = "Please enter a valid Sri Lanka contact number."
                                        }
                                        preferredDistrict.isBlank() -> {
                                            localError = "Please choose your preferred district."
                                        }
                                        else -> {
                                            localError = null
                                            step = 1
                                        }
                                    }
                                }
                                1 -> {
                                    if (selectedInterestLabels.isEmpty()) {
                                        localError = "Select at least 1 interest area so Smart Relay can match routes for you."
                                    } else {
                                        localError = null
                                        step = 2
                                    }
                                }
                                else -> {
                                    localError = null
                                    val user = signedInUser
                                    if (user != null) {
                                        onSubmitRegistration(
                                            fullName,
                                            phone,
                                            communityGroup,
                                            preferredDistrict,
                                            selectedInterestLabels,
                                            vehicleType,
                                            carrierCapacityKg,
                                            availabilityWindow,
                                            availableNow,
                                            user
                                        )
                                    } else {
                                        isSigningIn = true
                                        scope.launch {
                                            val authResult = GoogleAuthHelper.signInWithGoogle(context)
                                            isSigningIn = false
                                            authResult.onSuccess { authedUser ->
                                                signedInUser = authedUser
                                                onSubmitRegistration(
                                                    fullName.ifBlank { authedUser.displayName.orEmpty() },
                                                    phone,
                                                    communityGroup,
                                                    preferredDistrict,
                                                    selectedInterestLabels,
                                                    vehicleType,
                                                    carrierCapacityKg,
                                                    availabilityWindow,
                                                    availableNow,
                                                    authedUser
                                                )
                                            }.onFailure { err ->
                                                localError = if (err is GetCredentialCancellationException) {
                                                    "Google Sign-In was cancelled. Please select an account to save to Firestore."
                                                } else {
                                                    err.localizedMessage ?: "Unable to sign in with Google."
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isSubmitting && !isSigningIn,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag(if (step < 2) "volunteer_reg_next_button" else "volunteer_reg_submit_button")
                    ) {
                        if (isSubmitting || isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = SoftWhite
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isSigningIn) "Connecting Google..." else "Saving to Firestore...",
                                style = MaterialTheme.typography.titleSmall
                            )
                        } else {
                            Text(
                                text = when {
                                    step < 2 -> "Continue"
                                    signedInUser == null -> "Sign in with Google & Save"
                                    else -> "Save Volunteer Profile"
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (step < 2) Icons.AutoMirrored.Filled.ArrowForward else Icons.Outlined.CloudDone,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VolunteerStepBasicInfo(
    signedInUser: FirebaseUser?,
    fullName: String,
    onFullNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    communityGroup: String,
    onCommunityGroupChange: (String) -> Unit,
    preferredDistrict: String,
    onOpenDistrictPicker: () -> Unit,
    reducedMotion: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PipSpeechBubble(
            text = "Welcome to the volunteer relay! Share your basic details and preferred Sri Lankan district so we can prioritize nearby food rescues.",
            reducedMotion = reducedMotion
        )

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SoftWhite,
            border = BorderStroke(1.dp, CreamCardBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (signedInUser != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SageTint,
                        border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VerifiedUser,
                                contentDescription = "Signed in with Google",
                                tint = ForestGreen
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = signedInUser.displayName ?: "Signed in with Google",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DarkText
                                )
                                Text(
                                    text = signedInUser.email ?: "Connected Firebase account",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedText
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = onFullNameChange,
                    label = { Text("Full Name *") },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = ForestGreen) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("volunteer_reg_fullname")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Sri Lanka Mobile (+94) *") },
                    leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null, tint = ForestGreen) },
                    supportingText = { Text("Stored privately in your personal account record only") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("volunteer_reg_phone")
                )

                OutlinedTextField(
                    value = communityGroup,
                    onValueChange = onCommunityGroupChange,
                    label = { Text("Community Group or Team (Optional)") },
                    leadingIcon = { Icon(Icons.Outlined.Groups, contentDescription = null, tint = ForestGreen) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("volunteer_reg_group")
                )

                DistrictSelectorField(
                    label = "Preferred District (25 Districts in Sri Lanka) *",
                    selectedDistrict = preferredDistrict,
                    onClick = onOpenDistrictPicker,
                    modifier = Modifier.testTag("volunteer_reg_district_selector")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VolunteerStepInterestsAndTransport(
    selectedInterestLabels: List<String>,
    onToggleInterest: (String) -> Unit,
    vehicleType: String,
    onSelectVehicle: (String) -> Unit,
    carrierCapacityKg: Double,
    onSelectCapacity: (Double) -> Unit,
    availabilityWindow: String,
    onSelectWindow: (String) -> Unit,
    availableNow: Boolean,
    onAvailableNowChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Interest Areas Section
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SoftWhite,
            border = BorderStroke(1.dp, CreamCardBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Volunteer Interest Areas *",
                            style = MaterialTheme.typography.titleMedium,
                            color = DarkText
                        )
                        Text(
                            text = "Choose 1 or more food rescue categories you enjoy helping with.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )
                    }
                    Surface(shape = RoundedCornerShape(50), color = GoldTint) {
                        Text(
                            text = "${selectedInterestLabels.size} selected",
                            style = MaterialTheme.typography.labelMedium,
                            color = DeepGreen,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                VolunteerInterestArea.entries.forEach { area ->
                    key(area.name) {
                        val selected = area.label in selectedInterestLabels
                        val icon: ImageVector = when (area) {
                            VolunteerInterestArea.HOT_MEAL_RESCUE -> Icons.Outlined.Restaurant
                            VolunteerInterestArea.BAKERY_EVENING -> Icons.Outlined.BakeryDining
                            VolunteerInterestArea.FRESH_PRODUCE -> Icons.Outlined.Eco
                            VolunteerInterestArea.URGENT_DISPATCH -> Icons.Outlined.Bolt
                            VolunteerInterestArea.COMMUNITY_DRIVES -> Icons.Outlined.VolunteerActivism
                            VolunteerInterestArea.SORTING_PACKING -> Icons.Outlined.Inventory2
                        }
                        Surface(
                            onClick = { onToggleInterest(area.label) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) SageTint else WarmCream,
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) ForestGreen else CreamCardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .testTag("volunteer_reg_interest_${area.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) ForestGreen else SoftWhite),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (selected) SoftWhite else ForestGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = area.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DarkText
                                    )
                                    Text(
                                        text = area.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedText
                                    )
                                }
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = { onToggleInterest(area.label) },
                                    colors = CheckboxDefaults.colors(checkedColor = ForestGreen)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Transport & Schedule Section
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SoftWhite,
            border = BorderStroke(1.dp, CreamCardBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Transport & Carrier Capacity",
                    style = MaterialTheme.typography.titleMedium,
                    color = DarkText
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VEHICLE_OPTIONS.forEachIndexed { index, option ->
                        val active = option == vehicleType
                        FilterChip(
                            selected = active,
                            onClick = { onSelectVehicle(option) },
                            label = { Text(option) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SageTint,
                                selectedLabelColor = ForestGreen
                            ),
                            modifier = Modifier.testTag("volunteer_reg_vehicle_$index")
                        )
                    }
                }

                Text(
                    text = "Safe Food Carrier Capacity",
                    style = MaterialTheme.typography.labelLarge,
                    color = DarkText
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CAPACITY_OPTIONS.forEach { cap ->
                        val active = carrierCapacityKg == cap
                        FilterChip(
                            selected = active,
                            onClick = { onSelectCapacity(cap) },
                            label = { Text("${cap.toInt()} kg") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldTint,
                                selectedLabelColor = DeepGreen
                            ),
                            modifier = Modifier.testTag("volunteer_reg_capacity_${cap.toInt()}")
                        )
                    }
                }

                HorizontalDivider(color = CreamCardBorder)

                Text(
                    text = "Preferred Schedule Window",
                    style = MaterialTheme.typography.labelLarge,
                    color = DarkText
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AVAILABILITY_WINDOW_OPTIONS.forEachIndexed { index, window ->
                        val active = window == availabilityWindow
                        FilterChip(
                            selected = active,
                            onClick = { onSelectWindow(window) },
                            label = { Text(window) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SageTint,
                                selectedLabelColor = ForestGreen
                            ),
                            modifier = Modifier.testTag("volunteer_reg_window_$index")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Available for pickups now",
                            style = MaterialTheme.typography.titleSmall,
                            color = DarkText
                        )
                        Text(
                            text = "Show up in active Smart Relay suggestions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )
                    }
                    Switch(
                        checked = availableNow,
                        onCheckedChange = onAvailableNowChange,
                        colors = SwitchDefaults.colors(checkedTrackColor = ForestGreen),
                        modifier = Modifier.testTag("volunteer_reg_available_switch")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VolunteerStepReviewAndSave(
    signedInUser: FirebaseUser?,
    isSigningIn: Boolean,
    fullName: String,
    phone: String,
    communityGroup: String,
    preferredDistrict: String,
    selectedInterestLabels: List<String>,
    vehicleType: String,
    carrierCapacityKg: Double,
    availabilityWindow: String,
    availableNow: Boolean,
    reducedMotion: Boolean,
    onSignInClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PipSpeechBubble(
            text = "Everything looks great! Review your preferred district and interest areas, then save your volunteer profile to Cloud Firestore.",
            reducedMotion = reducedMotion
        )

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SoftWhite,
            border = BorderStroke(1.dp, CreamCardBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Volunteer Profile Summary",
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkText
                    )
                    Surface(shape = RoundedCornerShape(50), color = SageTint) {
                        Text(
                            text = preferredDistrict,
                            style = MaterialTheme.typography.labelMedium,
                            color = ForestGreen,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                SummaryRow(label = "Name", value = fullName.trim())
                SummaryRow(label = "Mobile (Private)", value = phone.trim())
                SummaryRow(
                    label = "Community Group",
                    value = communityGroup.trim().ifBlank { "$preferredDistrict Community Riders" }
                )
                SummaryRow(label = "Preferred District", value = "$preferredDistrict District")
                SummaryRow(label = "Transport", value = "$vehicleType • ${carrierCapacityKg.toInt()} kg")
                SummaryRow(
                    label = "Schedule",
                    value = "$availabilityWindow • ${if (availableNow) "Active now" else "Paused"}"
                )

                HorizontalDivider(color = CreamCardBorder)

                Text(
                    text = "Interest Areas",
                    style = MaterialTheme.typography.labelLarge,
                    color = DarkText
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedInterestLabels.forEach { interest ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = GoldTint,
                            border = BorderStroke(1.dp, HarvestGold.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = interest,
                                style = MaterialTheme.typography.labelMedium,
                                color = DeepGreen,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Google Sign-In Gate Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = if (signedInUser != null) SageTint else GoldTint,
            border = BorderStroke(
                1.dp,
                if (signedInUser != null) ForestGreen.copy(alpha = 0.35f) else HarvestGold.copy(alpha = 0.55f)
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (signedInUser != null) Icons.Outlined.CloudDone else Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        tint = ForestGreen
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (signedInUser != null) {
                                "Google Account Connected"
                            } else {
                                "Sign in with Google to Sync"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = DarkText
                        )
                        Text(
                            text = if (signedInUser != null) {
                                "${signedInUser.displayName ?: "Member"} (${signedInUser.email ?: "Verified"}) — ready to save to Cloud Firestore."
                            } else {
                                "Your private mobile number stays owner-only while your volunteer profile, preferred district, and interest areas sync securely with Cloud Firestore."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )
                    }
                }

                if (signedInUser == null) {
                    OutlinedButton(
                        onClick = onSignInClick,
                        enabled = !isSigningIn,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ForestGreen),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SoftWhite,
                            contentColor = ForestGreen
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("volunteer_reg_google_signin_button")
                    ) {
                        Icon(Icons.Outlined.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSigningIn) "Connecting Google..." else "Sign in with Google")
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MutedText,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = DarkText,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.58f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VolunteerRegistrationSuccessContent(
    volunteer: VolunteerEntity,
    reducedMotion: Boolean,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(SageTint),
            contentAlignment = Alignment.Center
        ) {
            PipMascot(
                size = 86.dp,
                mood = PipMood.CELEBRATING,
                reducedMotion = reducedMotion
            )
        }

        Text(
            text = "Volunteer Profile Saved!",
            style = MaterialTheme.typography.headlineMedium,
            color = ForestGreen,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Welcome aboard, ${volunteer.displayName}! Your volunteer profile for ${volunteer.preferredDistrict} District is now live in Cloud Firestore.",
            style = MaterialTheme.typography.bodyLarge,
            color = DarkText
        )

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SoftWhite,
            border = BorderStroke(1.dp, CreamCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryRow("Preferred District", volunteer.preferredDistrict)
                SummaryRow("Transport", "${volunteer.vehicleType} (${volunteer.carrierCapacityKg.toInt()} kg)")
                SummaryRow("Availability", volunteer.availabilityWindow)

                HorizontalDivider(color = CreamCardBorder)

                Text(
                    text = "Saved Interest Areas",
                    style = MaterialTheme.typography.labelLarge,
                    color = DarkText
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    volunteer.interestAreas.forEach { area ->
                        Surface(shape = RoundedCornerShape(50), color = SageTint) {
                            Text(
                                text = area,
                                style = MaterialTheme.typography.labelMedium,
                                color = ForestGreen,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))

        Button(
            onClick = onContinue,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("volunteer_reg_done_button")
        ) {
            Text("Go to Volunteer Dashboard", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}
