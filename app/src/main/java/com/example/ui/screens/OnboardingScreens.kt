package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.auth.GoogleAuthHelper
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.model.UserRole
import com.example.ui.components.*
import com.example.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    language: AppLanguage,
    onContinue: () -> Unit
) {
    val copy = stringsFor(language)
    var visible by remember { mutableStateOf(false) }
    val alphaState = animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "splash_alpha"
    )
    LaunchedEffect(Unit) {
        visible = true
        delay(700)
        onContinue()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(WarmCream, SageTint.copy(alpha = 0.65f), WarmCream)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .clickable { onContinue() }
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 420.dp)
                .graphicsLayer { this.alpha = alphaState.value }
        ) {
            DemetraLogoMark(size = 132.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "DEMETRA",
                style = MaterialTheme.typography.displayLarge,
                color = ForestGreen,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = copy.tagline,
                style = MaterialTheme.typography.titleMedium,
                color = DarkText,
                textAlign = TextAlign.Center
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = GoldTint
            ) {
                Text(
                    text = "Sri Lanka Food Redistribution Ecosystem",
                    style = MaterialTheme.typography.labelMedium,
                    color = DeepGreen,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

private data class OnboardingSlide(
    val title: String,
    val body: String,
    val pipNote: String,
    val badge: String
)

@Composable
fun WelcomeOnboardingScreen(
    language: AppLanguage,
    reducedMotion: Boolean,
    onLanguageSelected: (AppLanguage) -> Unit,
    onFinished: () -> Unit
) {
    val copy = stringsFor(language)
    val slides = remember(language) {
        listOf(
            OnboardingSlide(
                title = copy.welcomeTitle,
                body = "Demetra connects restaurants, bakeries, hotels, and households with local volunteers and community kitchens across Sri Lanka.",
                pipNote = "Ayubowan! I'm Pip, your gentle guide to sharing surplus food safely.",
                badge = "Community Nourishment"
            ),
            OnboardingSlide(
                title = "Meet Pip, Your AI Guide",
                body = "Tap Pip anytime for natural-language help drafting donations, checking food safety windows, or understanding pickup routes.",
                pipNote = "I'll stay right at the corner of your screen whenever you need a quick tip!",
                badge = "Intelligent Assistance"
            ),
            OnboardingSlide(
                title = "Smart Relay & Verified Handover",
                body = "Explainable matching pairs surplus food with nearby kitchens based on diet, capacity, and urgency — verified by QR or 4-digit PIN.",
                pipNote = "Every pickup and drop-off is verified so trust and impact stay accurate.",
                badge = "Explainable Matching"
            ),
            OnboardingSlide(
                title = "Grow Your Harvest Garden",
                body = "Each verified donation or delivery blooms a native flower in your 6-slot garden. Reach Full Bloom to unlock and archive new gardens.",
                pipNote = "Every good deed leaves something growing — let's choose your role!",
                badge = "Every Good Deed Grows"
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCream)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row: Language quick switcher + Skip button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AppLanguage.entries.forEach { lang ->
                    val selected = lang == language
                    Surface(
                        onClick = { onLanguageSelected(lang) },
                        shape = RoundedCornerShape(50),
                        color = if (selected) ForestGreen else SoftWhite,
                        border = BorderStroke(1.dp, if (selected) ForestGreen else CreamCardBorder),
                        modifier = Modifier.testTag("lang_chip_${lang.code}")
                    ) {
                        Text(
                            text = lang.nativeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) SoftWhite else DarkText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            TextButton(
                onClick = onFinished,
                modifier = Modifier.testTag("skip_onboarding_button")
            ) {
                Text("Skip", color = ForestGreen)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 540.dp)
        ) { page ->
            val slide = slides[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = SoftWhite,
                    border = BorderStroke(1.dp, CreamCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(50), color = GoldTint) {
                            Text(
                                text = slide.badge,
                                style = MaterialTheme.typography.labelMedium,
                                color = DeepGreen,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }

                        if (page == 0) {
                            DemetraLogoMark(size = 112.dp)
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(112.dp)
                                    .clip(CircleShape)
                                    .background(SageTint),
                                contentAlignment = Alignment.Center
                            ) {
                                PipMascot(
                                    size = 86.dp,
                                    mood = if (page == 3) PipMood.CELEBRATING else PipMood.HAPPY,
                                    reducedMotion = reducedMotion
                                )
                            }
                        }

                        Text(
                            text = slide.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = DarkText,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = slide.body,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MutedText,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                PipSpeechBubble(
                    text = slide.pipNote,
                    reducedMotion = reducedMotion
                )
            }
        }

        // Page dots + Next / Get Started button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(slides.size) { index ->
                    val active = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (active) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (active) ForestGreen else CreamCardBorder)
                    )
                }
            }

            Button(
                onClick = {
                    if (pagerState.currentPage < slides.lastIndex) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinished()
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_next_button")
            ) {
                Text(
                    text = if (pagerState.currentPage < slides.lastIndex) "Continue" else "Choose Your Role",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
fun RoleSelectionScreen(
    selectedRole: UserRole,
    reducedMotion: Boolean,
    onRolePicked: (UserRole) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCream)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "How will you nourish your community?",
                style = MaterialTheme.typography.headlineMedium,
                color = DarkText
            )
            PipSpeechBubble(
                text = "Pick a role to explore its tailored Demetra experience. You can switch roles anytime in Settings during testing!",
                reducedMotion = reducedMotion
            )

            UserRole.entries.forEach { role ->
                val isSelected = role == selectedRole
                val icon: ImageVector = when (role) {
                    UserRole.DONOR -> Icons.Outlined.VolunteerActivism
                    UserRole.VOLUNTEER -> Icons.Outlined.DeliveryDining
                    UserRole.RECIPIENT -> Icons.Outlined.SoupKitchen
                }
                Surface(
                    onClick = { onRolePicked(role) },
                    shape = RoundedCornerShape(22.dp),
                    color = if (isSelected) SageTint else SoftWhite,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) ForestGreen else CreamCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("role_card_${role.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) ForestGreen else WarmCream),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = role.title,
                                tint = if (isSelected) SoftWhite else ForestGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = role.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = DarkText
                            )
                            Text(
                                text = role.subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = ForestGreen
                            )
                            Text(
                                text = role.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedText
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { onRolePicked(role) },
                            colors = RadioButtonDefaults.colors(selectedColor = ForestGreen)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("continue_role_button")
            ) {
                Text(
                    text = "Continue as ${selectedRole.title}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
fun AuthAndProfileScreen(
    role: UserRole,
    reducedMotion: Boolean,
    initialSignUp: Boolean = true,
    onBack: () -> Unit,
    onComplete: (fullName: String, org: String, district: String, phone: String, note: String, firebaseUser: FirebaseUser?) -> Unit
) {
    val context: Context = LocalContext.current
    val scope = rememberCoroutineScope()
    var signedInUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    var fullName by remember(signedInUser) {
        mutableStateOf(signedInUser?.displayName?.takeIf { it.isNotBlank() } ?: "Amaya Wickramasinghe")
    }
    var orgName by remember(role) {
        mutableStateOf(
            when (role) {
                UserRole.DONOR -> "Upali's Kitchen"
                UserRole.VOLUNTEER -> "Colombo Community Riders"
                UserRole.RECIPIENT -> "Sarana Community Kitchen"
            }
        )
    }
    var selectedDistrict by remember { mutableStateOf("Colombo") }
    var showDistrictPicker by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("+94 77 412 8930") }
    var roleNote by remember(role) {
        mutableStateOf(
            when (role) {
                UserRole.DONOR -> "Restaurant & Catering Surplus"
                UserRole.VOLUNTEER -> "E-Bike • Insulated Carrier"
                UserRole.RECIPIENT -> "Vegetarian & Halal Meals"
            }
        )
    }

    if (showDistrictPicker) {
        SriLankaDistrictPickerSheet(
            selectedDistrict = selectedDistrict,
            onDistrictSelected = { selectedDistrict = it },
            onDismiss = { showDistrictPicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCream)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
    ) {
        SubScreenTopBar(title = "${role.title} Account Setup", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SoftWhite,
                border = BorderStroke(1.dp, CreamCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (signedInUser != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SageTint,
                            border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.VerifiedUser,
                                    contentDescription = "Google account connected",
                                    tint = ForestGreen
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = signedInUser?.displayName ?: "Signed in with Google",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DarkText
                                    )
                                    Text(
                                        text = signedInUser?.email ?: "Authenticated Firebase account",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedText
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = orgName,
                        onValueChange = { orgName = it },
                        label = {
                            Text(
                                when (role) {
                                    UserRole.DONOR -> "Kitchen / Business Name"
                                    UserRole.VOLUNTEER -> "Community Group (Optional)"
                                    UserRole.RECIPIENT -> "Shelter / Kitchen Name"
                                }
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Sri Lanka Mobile (+94)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    DistrictSelectorField(
                        label = "Primary District (25 Districts in Sri Lanka)",
                        selectedDistrict = selectedDistrict,
                        onClick = { showDistrictPicker = true },
                        modifier = Modifier.testTag("auth_district_selector")
                    )

                    OutlinedTextField(
                        value = roleNote,
                        onValueChange = { roleNote = it },
                        label = {
                            Text(
                                when (role) {
                                    UserRole.DONOR -> "Typical Surplus Type"
                                    UserRole.VOLUNTEER -> "Vehicle & Carrier Capacity"
                                    UserRole.RECIPIENT -> "Dietary & Storage Notes"
                                }
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (authError != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ErrorTerracotta.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, ErrorTerracotta.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = authError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorTerracotta,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            PipSpeechBubble(
                text = if (signedInUser != null) {
                    "Your Google account is connected. Save your ${role.title.lowercase()} profile to sync donations, volunteers, and food requests securely with Cloud Firestore."
                } else {
                    "Sign in with Google via Credential Manager to sync your ${role.title.lowercase()} profile, donations, volunteers, and requests securely with Cloud Firestore."
                },
                reducedMotion = reducedMotion
            )

            Button(
                onClick = {
                    if (isSigningIn) return@Button
                    val existing = signedInUser
                    if (existing != null) {
                        onComplete(fullName, orgName, selectedDistrict, phone, roleNote, existing)
                    } else {
                        isSigningIn = true
                        authError = null
                        scope.launch {
                            val result = GoogleAuthHelper.signInWithGoogle(context)
                            isSigningIn = false
                            result.onSuccess { user ->
                                signedInUser = user
                                val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: fullName
                                onComplete(displayName, orgName, selectedDistrict, phone, roleNote, user)
                            }.onFailure { err ->
                                authError = if (err is GetCredentialCancellationException) {
                                    "Google Sign-In was cancelled. Tap below whenever you're ready."
                                } else {
                                    err.localizedMessage ?: "Unable to complete Google Sign-In."
                                }
                            }
                        }
                    }
                },
                enabled = !isSigningIn,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("complete_auth_button")
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = SoftWhite
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Connecting Google Account...", style = MaterialTheme.typography.titleMedium)
                } else {
                    Text(
                        text = if (signedInUser != null) {
                            "Continue to ${role.title} Dashboard"
                        } else {
                            "Continue with Google as ${role.title}"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
