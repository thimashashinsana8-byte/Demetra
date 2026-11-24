package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.PipAiService
import com.example.ai.PipStreamEvent
import com.example.auth.GoogleAuthHelper
import com.example.data.DemetraRepository
import com.example.data.FirestoreRedistributionRepository
import com.example.data.FlowerGrowthEvent
import com.example.data.PreferencesRepository
import com.example.data.UserSettings
import com.example.i18n.AppLanguage
import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface PipUiState {
    data object Idle : PipUiState
    data object Loading : PipUiState
    data class Error(val message: String) : PipUiState
}

sealed interface VolunteerRegistrationUiState {
    data object Idle : VolunteerRegistrationUiState
    data object Submitting : VolunteerRegistrationUiState
    data class Success(val volunteer: VolunteerEntity) : VolunteerRegistrationUiState
    data class Error(val message: String) : VolunteerRegistrationUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class DemetraViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DemetraRepository()
    private val preferencesRepository = PreferencesRepository(application.applicationContext)
    private val pipAiService = PipAiService()
    private val firebaseAuth: FirebaseAuth = Firebase.auth
    val firestoreRepository = FirestoreRedistributionRepository(application.applicationContext)

    private val _authUid = MutableStateFlow<String?>(firebaseAuth.currentUser?.uid)
    val authUid: StateFlow<String?> = _authUid.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        _authUid.value = user?.uid
        if (user != null) {
            _userProfile.update { current ->
                current.copy(
                    fullName = user.displayName?.takeIf { it.isNotBlank() } ?: current.fullName,
                    isAuthenticated = true
                )
            }
        }
    }

    init {
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        firebaseAuth.removeAuthStateListener(authStateListener)
        super.onCleared()
    }

    val donations: StateFlow<List<FoodDonation>> = repository.donations
    val donorGarden: StateFlow<GardenState> = repository.donorGarden
    val volunteerGarden: StateFlow<GardenState> = repository.volunteerGarden
    val communityGrove: StateFlow<CommunityGroveStats> = repository.communityGrove
    val notifications: StateFlow<List<AppNotification>> = repository.notifications
    val donorImpact: StateFlow<ImpactSummary> = repository.donorImpact
    val volunteerImpact: StateFlow<ImpactSummary> = repository.volunteerImpact
    val latestFlowerEvent: StateFlow<FlowerGrowthEvent?> = repository.latestFlowerEvent
    val latestMilestone: StateFlow<PipMilestoneEvent?> = repository.latestMilestone
    val donationSuccessState: StateFlow<DonationSuccessState> = repository.donationSuccessState

    val cloudDonors: StateFlow<RedistributionUiState<List<DonorEntity>>> = _authUid
        .flatMapLatest { uid ->
            if (uid == null) flowOf(RedistributionUiState.Success(emptyList()))
            else firestoreRepository.observeDonors()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RedistributionUiState.Loading)

    val cloudVolunteers: StateFlow<RedistributionUiState<List<VolunteerEntity>>> = _authUid
        .flatMapLatest { uid ->
            if (uid == null) flowOf(RedistributionUiState.Success(emptyList()))
            else firestoreRepository.observeVolunteers()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RedistributionUiState.Loading)

    val myVolunteerProfile: StateFlow<RedistributionUiState<VolunteerEntity?>> = _authUid
        .flatMapLatest { uid ->
            if (uid == null) flowOf(RedistributionUiState.Success(null))
            else firestoreRepository.observeVolunteerProfile(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RedistributionUiState.Success(null))

    private val _volunteerRegistrationState =
        MutableStateFlow<VolunteerRegistrationUiState>(VolunteerRegistrationUiState.Idle)
    val volunteerRegistrationState: StateFlow<VolunteerRegistrationUiState> =
        _volunteerRegistrationState.asStateFlow()

    val cloudDonations: StateFlow<RedistributionUiState<List<FoodDonationEntity>>> = _authUid
        .flatMapLatest { uid ->
            if (uid == null) flowOf(RedistributionUiState.Success(emptyList()))
            else firestoreRepository.observeDonations()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RedistributionUiState.Loading)

    val cloudRequests: StateFlow<RedistributionUiState<List<FoodRequestEntity>>> = _authUid
        .flatMapLatest { uid ->
            if (uid == null) flowOf(RedistributionUiState.Success(emptyList()))
            else firestoreRepository.observeParticipantRequests(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RedistributionUiState.Loading)

    private val _cloudSyncError = MutableStateFlow<String?>(null)
    val cloudSyncError: StateFlow<String?> = _cloudSyncError.asStateFlow()

    fun clearCloudSyncError() {
        _cloudSyncError.value = null
    }

    val settings: StateFlow<UserSettings> = preferencesRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val _currentRole = MutableStateFlow(UserRole.DONOR)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val _userProfile = MutableStateFlow(
        UserProfile(
            fullName = "Amaya Wickramasinghe",
            organization = "Upali's Kitchen",
            district = "Colombo",
            phone = "+94 77 412 8930",
            vehicleOrDietaryNote = "Insulated Packaging Ready",
            isAuthenticated = firebaseAuth.currentUser != null
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _selectedDonationId = MutableStateFlow<String?>(null)
    val selectedDonationId: StateFlow<String?> = _selectedDonationId.asStateFlow()

    private val _selectedMapDistricts = MutableStateFlow<Set<String>>(emptySet())
    val selectedMapDistricts: StateFlow<Set<String>> = _selectedMapDistricts.asStateFlow()

    private val _pipMessages = MutableStateFlow(
        listOf(
            PipChatMessage(
                id = "pip-welcome",
                text = "Ayubowan! I'm Pip, your Demetra guide. Ask me about creating a safe food listing, how Smart Relay picks a match, or nearby redistribution tips in Sri Lanka.",
                isFromUser = false
            )
        )
    )
    val pipMessages: StateFlow<List<PipChatMessage>> = _pipMessages.asStateFlow()

    private val _pipUiState = MutableStateFlow<PipUiState>(PipUiState.Idle)
    val pipUiState: StateFlow<PipUiState> = _pipUiState.asStateFlow()

    private val _isPipOpen = MutableStateFlow(false)
    val isPipOpen: StateFlow<Boolean> = _isPipOpen.asStateFlow()

    private val _currentScreenContext = MutableStateFlow("Dashboard")
    val currentScreenContext: StateFlow<String> = _currentScreenContext.asStateFlow()

    private var pipJob: Job? = null

    fun selectRole(role: UserRole) {
        _currentRole.value = role
        val defaultOrg = when (role) {
            UserRole.DONOR -> "Upali's Kitchen"
            UserRole.VOLUNTEER -> "Colombo Community Riders"
            UserRole.RECIPIENT -> "Sarana Community Kitchen"
        }
        val defaultNote = when (role) {
            UserRole.DONOR -> "Restaurant & Catering Surplus"
            UserRole.VOLUNTEER -> "E-Bike • Insulated Box"
            UserRole.RECIPIENT -> "Vegetarian & Halal Meals"
        }
        _userProfile.update {
            it.copy(
                organization = defaultOrg,
                vehicleOrDietaryNote = defaultNote
            )
        }
    }

    fun completeAuthAndProfile(
        fullName: String,
        organization: String,
        district: String,
        phone: String,
        note: String,
        firebaseUser: FirebaseUser? = firebaseAuth.currentUser
    ) {
        val resolvedName = fullName.ifBlank {
            firebaseUser?.displayName?.takeIf { it.isNotBlank() } ?: "Amaya Wickramasinghe"
        }
        val resolvedOrg = organization.ifBlank { _userProfile.value.organization }
        val resolvedDistrict = district.ifBlank { "Colombo" }
        val resolvedPhone = phone.ifBlank { "+94 77 412 8930" }
        val resolvedNote = note.ifBlank { _userProfile.value.vehicleOrDietaryNote }

        _userProfile.value = UserProfile(
            fullName = resolvedName,
            organization = resolvedOrg,
            district = resolvedDistrict,
            phone = resolvedPhone,
            vehicleOrDietaryNote = resolvedNote,
            isAuthenticated = true
        )

        if (firebaseUser != null) {
            _authUid.value = firebaseUser.uid
            val activeRole = _currentRole.value
            viewModelScope.launch {
                firestoreRepository.upsertUserAccount(
                    fullName = resolvedName,
                    organization = resolvedOrg,
                    district = resolvedDistrict,
                    email = firebaseUser.email.orEmpty(),
                    phone = resolvedPhone,
                    role = activeRole,
                    vehicleOrDietaryNote = resolvedNote
                ).onFailure { _cloudSyncError.value = it.message }

                when (activeRole) {
                    UserRole.DONOR -> firestoreRepository.upsertDonorProfile(
                        organizationName = resolvedOrg,
                        donorType = "Verified Food Donor",
                        district = resolvedDistrict,
                        locationName = resolvedDistrict,
                        typicalSurplus = resolvedNote
                    ).onFailure { _cloudSyncError.value = it.message }

                    UserRole.VOLUNTEER -> firestoreRepository.upsertVolunteerProfile(
                        displayName = resolvedName,
                        communityGroup = resolvedOrg,
                        district = resolvedDistrict,
                        preferredDistrict = resolvedDistrict,
                        interestAreas = _userProfile.value.interestAreas.ifEmpty {
                            listOf(VolunteerInterestArea.HOT_MEAL_RESCUE.label)
                        },
                        vehicleType = resolvedNote,
                        carrierCapacityKg = 15.0,
                        available = true
                    ).onFailure { _cloudSyncError.value = it.message }

                    UserRole.RECIPIENT -> Unit
                }
            }
        }
    }

    fun resetVolunteerRegistrationState() {
        _volunteerRegistrationState.value = VolunteerRegistrationUiState.Idle
    }

    fun registerVolunteer(
        fullName: String,
        phone: String,
        communityGroup: String,
        preferredDistrict: String,
        interestAreas: List<String>,
        vehicleType: String,
        carrierCapacityKg: Double,
        availabilityWindow: String,
        available: Boolean,
        firebaseUser: FirebaseUser? = firebaseAuth.currentUser
    ) {
        val cleanName = fullName.trim()
        val cleanPhone = phone.trim()
        val cleanDistrict = preferredDistrict.trim()
        val cleanInterests = interestAreas
            .map { it.trim().take(60) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(8)
        val cleanGroup = communityGroup.trim().ifBlank { "$cleanDistrict Community Riders" }
        val cleanVehicle = vehicleType.trim().ifBlank { "E-Bike (Insulated Box)" }
        val cleanWindow = availabilityWindow.trim().ifBlank { "Flexible (Anytime)" }

        if (cleanName.length < 2) {
            _volunteerRegistrationState.value =
                VolunteerRegistrationUiState.Error("Please enter your full name (at least 2 characters).")
            return
        }
        if (cleanDistrict.isEmpty()) {
            _volunteerRegistrationState.value =
                VolunteerRegistrationUiState.Error("Please choose your preferred district in Sri Lanka.")
            return
        }
        if (cleanInterests.isEmpty()) {
            _volunteerRegistrationState.value =
                VolunteerRegistrationUiState.Error("Please select at least one volunteer interest area.")
            return
        }
        if (firebaseUser == null) {
            _volunteerRegistrationState.value =
                VolunteerRegistrationUiState.Error("Please sign in with Google to save your volunteer registration to Cloud Firestore.")
            return
        }

        _volunteerRegistrationState.value = VolunteerRegistrationUiState.Submitting
        _authUid.value = firebaseUser.uid

        viewModelScope.launch {
            val userResult = firestoreRepository.upsertUserAccount(
                fullName = cleanName,
                organization = cleanGroup,
                district = cleanDistrict,
                email = firebaseUser.email.orEmpty(),
                phone = cleanPhone,
                role = UserRole.VOLUNTEER,
                vehicleOrDietaryNote = "$cleanVehicle • ${cleanCapacityLabel(carrierCapacityKg)}"
            )
            if (userResult.isFailure) {
                val msg = userResult.exceptionOrNull()?.localizedMessage
                    ?: "Unable to save account details to Cloud Firestore."
                _cloudSyncError.value = msg
                _volunteerRegistrationState.value = VolunteerRegistrationUiState.Error(msg)
                return@launch
            }

            val volunteerResult = firestoreRepository.upsertVolunteerProfile(
                displayName = cleanName,
                communityGroup = cleanGroup,
                district = cleanDistrict,
                preferredDistrict = cleanDistrict,
                interestAreas = cleanInterests,
                vehicleType = cleanVehicle,
                carrierCapacityKg = carrierCapacityKg,
                availabilityWindow = cleanWindow,
                available = available
            )

            volunteerResult.onSuccess { savedVolunteer ->
                _currentRole.value = UserRole.VOLUNTEER
                _userProfile.value = UserProfile(
                    fullName = cleanName,
                    organization = cleanGroup,
                    district = cleanDistrict,
                    phone = cleanPhone.ifBlank { _userProfile.value.phone },
                    vehicleOrDietaryNote = "$cleanVehicle • ${cleanCapacityLabel(carrierCapacityKg)}",
                    interestAreas = cleanInterests,
                    isAuthenticated = true
                )
                _volunteerRegistrationState.value = VolunteerRegistrationUiState.Success(savedVolunteer)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Failed to save volunteer profile to Cloud Firestore."
                _cloudSyncError.value = msg
                _volunteerRegistrationState.value = VolunteerRegistrationUiState.Error(msg)
            }
        }
    }

    private fun cleanCapacityLabel(capacityKg: Double): String {
        return if (capacityKg % 1.0 == 0.0) "${capacityKg.toInt()} kg capacity" else "${"%.1f".format(capacityKg)} kg capacity"
    }

    fun updateDistrict(district: String) {
        if (district.isBlank()) return
        val trimmed = district.trim()
        _userProfile.update { it.copy(district = trimmed) }
        if (firebaseAuth.currentUser != null) {
            viewModelScope.launch {
                firestoreRepository.updateUserDistrict(trimmed)
                    .onFailure { _cloudSyncError.value = it.message }
            }
        }
    }

    fun toggleMapDistrict(districtName: String) {
        val canonical = normalizeSriLankaDistrict(districtName)
        _selectedMapDistricts.update { current ->
            if (canonical in current) current - canonical else current + canonical
        }
    }

    fun setMapDistricts(districts: Set<String>) {
        _selectedMapDistricts.value = districts.map { normalizeSriLankaDistrict(it) }.toSet()
    }

    fun clearMapDistrictFilters() {
        _selectedMapDistricts.value = emptySet()
    }

    fun logout() {
        pipJob?.cancel()
        _isPipOpen.value = false
        _pipUiState.value = PipUiState.Idle
        // Detach Firestore listeners cleanly before FirebaseAuth.signOut()
        _authUid.value = null
        _userProfile.update { it.copy(isAuthenticated = false) }
        viewModelScope.launch {
            GoogleAuthHelper.signOut(getApplication(), firebaseAuth)
        }
    }

    fun updateCurrentScreen(screenName: String) {
        _currentScreenContext.value = screenName
    }

    fun selectDonation(id: String) {
        _selectedDonationId.value = id
    }

    fun createDonation(
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
    ): FoodDonation {
        val created = repository.createDonation(
            title = title,
            category = category,
            quantityKg = quantityKg,
            servingsEstimate = servingsEstimate,
            donorName = _userProfile.value.organization,
            locationName = locationName,
            district = district,
            pickupWindow = pickupWindow,
            expiryNote = expiryNote,
            dietaryTags = dietaryTags,
            pickedPhotoUri = pickedPhotoUri
        )
        _selectedDonationId.value = created.id
        if (firebaseAuth.currentUser != null) {
            viewModelScope.launch {
                firestoreRepository.createFoodDonation(
                    title = title,
                    category = category,
                    quantityKg = quantityKg,
                    servingsEstimate = servingsEstimate,
                    donorName = _userProfile.value.organization,
                    locationName = locationName,
                    district = district,
                    pickupWindow = pickupWindow,
                    expiryNote = expiryNote,
                    dietaryTags = dietaryTags,
                    handoverPin = created.handoverPin
                ).onFailure { _cloudSyncError.value = it.message }
            }
        }
        return created
    }

    fun assignSuggestedVolunteer(donationId: String) =
        repository.assignSuggestedVolunteer(donationId)

    fun acceptVolunteerTask(donationId: String) {
        repository.acceptVolunteerTask(donationId, _userProfile.value.fullName)
        if (firebaseAuth.currentUser != null && !donationId.startsWith("DON-")) {
            viewModelScope.launch {
                firestoreRepository.acceptDonationAsVolunteer(donationId, _userProfile.value.fullName)
                    .onFailure { _cloudSyncError.value = it.message }
            }
        }
    }

    fun verifyPickup(donationId: String) {
        repository.verifyPickup(donationId)
        if (firebaseAuth.currentUser != null && !donationId.startsWith("DON-")) {
            viewModelScope.launch {
                firestoreRepository.markDonationPickedUp(donationId)
                    .onFailure { _cloudSyncError.value = it.message }
            }
        }
    }

    fun verifyDonorHandover(donationId: String) {
        repository.verifyDonorHandover(donationId)
        if (firebaseAuth.currentUser != null && !donationId.startsWith("DON-")) {
            viewModelScope.launch {
                firestoreRepository.markDonationDelivered(donationId)
                    .onFailure { _cloudSyncError.value = it.message }
            }
        }
    }

    fun verifyVolunteerDelivery(donationId: String) {
        repository.verifyVolunteerDelivery(donationId)
        if (firebaseAuth.currentUser != null && !donationId.startsWith("DON-")) {
            viewModelScope.launch {
                firestoreRepository.markDonationDelivered(donationId)
                    .onFailure { _cloudSyncError.value = it.message }
            }
        }
    }

    fun requestFoodAsRecipient(donationId: String, portions: Int) {
        repository.requestFoodAsRecipient(donationId, portions)
        val currentUid = firebaseAuth.currentUser?.uid
        val targetDonation = donations.value.find { it.id == donationId }
        if (currentUid != null && targetDonation != null) {
            viewModelScope.launch {
                firestoreRepository.createFoodRequest(
                    donationId = donationId,
                    donationTitle = targetDonation.title,
                    donorUserId = currentUid,
                    recipientOrgName = _userProfile.value.organization,
                    district = targetDonation.district,
                    requestedPortions = portions,
                    dietaryNotes = _userProfile.value.vehicleOrDietaryNote
                ).onFailure { _cloudSyncError.value = it.message }
            }
        }
    }

    fun confirmRecipientReceipt(donationId: String) {
        repository.confirmRecipientReceipt(donationId)
        if (firebaseAuth.currentUser != null && !donationId.startsWith("DON-")) {
            viewModelScope.launch {
                firestoreRepository.markDonationDelivered(donationId)
                    .onFailure { _cloudSyncError.value = it.message }
            }
        }
    }

    fun selectGardenTheme(theme: GardenTheme) =
        repository.selectGardenTheme(_currentRole.value, theme)

    fun clearFlowerEvent() = repository.clearFlowerEvent()

    fun clearMilestone() = repository.clearMilestone()

    fun clearDonationSuccessState() = repository.clearDonationSuccessState()

    fun markAllNotificationsRead() = repository.markAllNotificationsRead()

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { preferencesRepository.setLanguage(language) }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setReducedMotion(enabled) }
    }

    fun setLowDataMode(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setLowDataMode(enabled) }
    }

    fun openPip() {
        _isPipOpen.value = true
    }

    fun closePip() {
        _isPipOpen.value = false
    }

    fun contextualSuggestions(): List<PipSuggestion> {
        val screen = _currentScreenContext.value
        return when (_currentRole.value) {
            UserRole.DONOR -> when {
                screen.contains("Create", ignoreCase = true) -> listOf(
                    PipSuggestion("Safe packing checklist", "What food safety rules should I check before packing surplus rice and curry?"),
                    PipSuggestion("Estimate portions", "How many servings is 8 kg of cooked rice and curry roughly equal to?")
                )
                screen.contains("Garden", ignoreCase = true) -> listOf(
                    PipSuggestion("How flowers grow", "How do I earn Full Bloom and unlock a new Harvest Garden theme?"),
                    PipSuggestion("Community Grove", "How does Community Grove protect recipient privacy?")
                )
                else -> listOf(
                    PipSuggestion("Explain Smart Relay", "Explain why Smart Relay matched my listing with Sarana Community Kitchen."),
                    PipSuggestion("Draft a donation", "Help me write a clear surplus food listing for 20 bakery buns in Colombo 03."),
                    PipSuggestion("Food safety window", "How long can warm Sri Lankan cooked meals stay safe for redistribution?")
                )
            }
            UserRole.VOLUNTEER -> listOf(
                PipSuggestion("Best nearby pickup", "Which active pickup near ${_userProfile.value.district} should I prioritize right now and why?"),
                PipSuggestion("Transport safety tips", "How should I transport hot cooked meals vs fresh produce safely?"),
                PipSuggestion("PIN & QR handover", "How does Verified Handover work at pickup and drop-off?")
            )
            UserRole.RECIPIENT -> listOf(
                PipSuggestion("Dietary check", "Which available donations right now are vegetarian or Halal-friendly?"),
                PipSuggestion("Reheating & safety", "What safe storage and reheating steps should we follow when meals arrive?"),
                PipSuggestion("Track incoming food", "How do I confirm receipt when the volunteer arrives?")
            )
        }
    }

    fun askPip(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        pipJob?.cancel()
        val userMsg = PipChatMessage(
            id = "u-${System.currentTimeMillis()}",
            text = trimmed,
            isFromUser = true
        )
        val assistantMsgId = "p-${System.currentTimeMillis() + 1}"
        val placeholderMsg = PipChatMessage(
            id = assistantMsgId,
            text = "",
            isFromUser = false,
            isStreaming = true
        )

        _pipMessages.update { it + userMsg + placeholderMsg }
        _pipUiState.value = PipUiState.Loading

        val activeListSummary = donations.value.take(4).joinToString("; ") {
            "${it.title} (${it.district}, ${it.servingsEstimate} portions, ${it.status.label}, ${it.dietaryTags.joinToString("/")})"
        }

        pipJob = viewModelScope.launch {
            pipAiService.streamAssistantReply(
                userQuery = trimmed,
                role = _currentRole.value,
                screenName = _currentScreenContext.value,
                contextSummary = "User District: ${_userProfile.value.district}. Active listings: $activeListSummary"
            ).collect { event ->
                when (event) {
                    is PipStreamEvent.Chunk -> {
                        _pipMessages.update { msgs ->
                            val idx = msgs.indexOfLast { it.id == assistantMsgId }
                            if (idx < 0 || msgs[idx].text == event.accumulatedText) msgs
                            else msgs.toMutableList().apply {
                                set(idx, msgs[idx].copy(text = event.accumulatedText, isStreaming = true))
                            }
                        }
                    }
                    is PipStreamEvent.Completed -> {
                        val (actionLabel, actionRoute) = inferNextAction(event.finalText, _currentRole.value)
                        _pipMessages.update { msgs ->
                            val idx = msgs.indexOfLast { it.id == assistantMsgId }
                            if (idx < 0) msgs
                            else msgs.toMutableList().apply {
                                set(
                                    idx,
                                    msgs[idx].copy(
                                        text = event.finalText,
                                        isStreaming = false,
                                        quickActionLabel = actionLabel,
                                        quickActionRoute = actionRoute
                                    )
                                )
                            }
                        }
                        _pipUiState.value = PipUiState.Idle
                    }
                    is PipStreamEvent.Error -> {
                        _pipMessages.update { msgs ->
                            msgs.filterNot { it.id == assistantMsgId && it.text.isBlank() }
                        }
                        _pipUiState.value = PipUiState.Error(event.message)
                    }
                }
            }
        }
    }

    fun dismissPipError() {
        _pipUiState.value = PipUiState.Idle
    }

    private fun inferNextAction(reply: String, role: UserRole): Pair<String?, String?> {
        val lower = reply.lowercase()
        return when {
            role == UserRole.DONOR && (lower.contains("listing") || lower.contains("donate") || lower.contains("pack")) ->
                "Open Quick Donation" to "create_donation"
            role == UserRole.DONOR && (lower.contains("garden") || lower.contains("bloom")) ->
                "Explore Harvest Garden" to "harvest_garden"
            role == UserRole.VOLUNTEER && (lower.contains("pickup") || lower.contains("route") || lower.contains("nearby")) ->
                "Browse Pickups" to "volunteer_explore"
            role == UserRole.RECIPIENT && (lower.contains("available") || lower.contains("vegetarian") || lower.contains("request")) ->
                "Browse Available Food" to "recipient_browse"
            else -> null to null
        }
    }
}
