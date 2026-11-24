package com.example.data

import androidx.compose.runtime.Immutable
import com.example.R
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

@Immutable
data class FlowerGrowthEvent(
    val role: UserRole,
    val slotIndex: Int,
    val flowerSpecies: String,
    val earnedFromTitle: String,
    val reachedFullBloom: Boolean,
    val completedTheme: GardenTheme? = null,
    val nextUnlockedTheme: GardenTheme? = null,
    val completedGardenFlowers: List<FlowerSlot> = emptyList(),
    val completedArchiveId: String? = null
)

class DemetraRepository {

    private val speciesPool = listOf(
        "Ceylon Lotus",
        "Temple Frangipani",
        "Golden Marigold",
        "Blue Water Lily",
        "Cinnamon Jasmine",
        "Kandyan Orchid"
    )

    private val _donations = MutableStateFlow(seedDonations())
    val donations: StateFlow<List<FoodDonation>> = _donations.asStateFlow()

    private val _donorGarden = MutableStateFlow(seedDonorGarden())
    val donorGarden: StateFlow<GardenState> = _donorGarden.asStateFlow()

    private val _volunteerGarden = MutableStateFlow(seedVolunteerGarden())
    val volunteerGarden: StateFlow<GardenState> = _volunteerGarden.asStateFlow()

    private val _communityGrove = MutableStateFlow(seedCommunityGrove())
    val communityGrove: StateFlow<CommunityGroveStats> = _communityGrove.asStateFlow()

    private val _notifications = MutableStateFlow(seedNotifications())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _donorImpact = MutableStateFlow(
        ImpactSummary(
            verifiedHandoversCount = 11,
            measuredWeightKg = 84.5,
            estimatedMealsSupported = 211,
            estimatedValueLkr = 94500,
            deliveriesCompleted = 0
        )
    )
    val donorImpact: StateFlow<ImpactSummary> = _donorImpact.asStateFlow()

    private val _volunteerImpact = MutableStateFlow(
        ImpactSummary(
            verifiedHandoversCount = 9,
            measuredWeightKg = 62.0,
            estimatedMealsSupported = 155,
            estimatedValueLkr = 68200,
            deliveriesCompleted = 9
        )
    )
    val volunteerImpact: StateFlow<ImpactSummary> = _volunteerImpact.asStateFlow()

    private val _latestFlowerEvent = MutableStateFlow<FlowerGrowthEvent?>(null)
    val latestFlowerEvent: StateFlow<FlowerGrowthEvent?> = _latestFlowerEvent.asStateFlow()

    private val _latestMilestone = MutableStateFlow<PipMilestoneEvent?>(null)
    val latestMilestone: StateFlow<PipMilestoneEvent?> = _latestMilestone.asStateFlow()

    private val _donationSuccessState = MutableStateFlow<DonationSuccessState>(DonationSuccessState.Idle)
    val donationSuccessState: StateFlow<DonationSuccessState> = _donationSuccessState.asStateFlow()

    fun clearFlowerEvent() {
        _latestFlowerEvent.value = null
    }

    fun clearMilestone() {
        _latestMilestone.value = null
    }

    fun clearDonationSuccessState() {
        _donationSuccessState.value = DonationSuccessState.Idle
    }

    private fun emitMilestone(
        headline: String,
        message: String,
        isCelebration: Boolean = false,
        actionLabel: String? = null,
        actionRoute: String? = null
    ) {
        _latestMilestone.value = PipMilestoneEvent(
            id = System.nanoTime(),
            headline = headline,
            message = message,
            isCelebration = isCelebration,
            actionLabel = actionLabel,
            actionRoute = actionRoute
        )
    }

    fun createDonation(
        title: String,
        category: FoodCategory,
        quantityKg: Double,
        servingsEstimate: Int,
        donorName: String,
        locationName: String,
        district: String,
        pickupWindow: String,
        expiryNote: String,
        dietaryTags: List<String>,
        pickedPhotoUri: String?
    ): FoodDonation {
        val pin = (1000 + Random.nextInt(9000)).toString()
        val id = "DON-${1040 + _donations.value.size}"
        val match = buildSmartRelayMatch(category, quantityKg, district, dietaryTags, pickupWindow)
        val (pickupLat, pickupLng) = resolveSriLankaCoordinates(district, _donations.value.size + 1)

        val newDonation = FoodDonation(
            id = id,
            title = title,
            category = category,
            quantityKg = quantityKg,
            servingsEstimate = servingsEstimate,
            estimatedValueLkr = null,
            donorName = donorName.ifBlank { "Cinnamon Hearth Kitchen" },
            donorType = "Verified Food Donor",
            locationName = locationName.ifBlank { "Ward Place, $district" },
            district = district,
            pickupWindow = pickupWindow,
            expiryNote = expiryNote,
            dietaryTags = dietaryTags.ifEmpty { listOf("Freshly Packed") },
            safetyConfirmed = true,
            photoResId = category.defaultImageRes,
            pickedPhotoUri = pickedPhotoUri,
            status = DonationStatus.MATCHING,
            handoverPin = pin,
            qrPayload = "DEMETRA:$id:PIN:$pin",
            smartRelayMatch = match,
            assignedVolunteerName = null,
            createdAtLabel = "Just now",
            mapXFraction = 0.42f,
            mapYFraction = 0.38f,
            latitude = pickupLat,
            longitude = pickupLng
        )

        _donations.update { listOf(newDonation) + it }
        addNotification(
            title = "Smart Relay found a ${match.matchPercentage}% match",
            body = "${match.recipientOrgName} (${match.distanceKm} km) matches your ${title.lowercase()}.",
            targetRole = UserRole.DONOR
        )
        _donationSuccessState.value = DonationSuccessState.Published(
            eventId = System.nanoTime(),
            donationId = id,
            title = title,
            recipientOrgName = match.recipientOrgName,
            matchPercentage = match.matchPercentage
        )
        return newDonation
    }

    fun assignSuggestedVolunteer(donationId: String) {
        _donations.update { list ->
            list.map { item ->
                if (item.id == donationId && item.status == DonationStatus.MATCHING) {
                    item.copy(
                        status = DonationStatus.VOLUNTEER_ASSIGNED,
                        assignedVolunteerName = item.smartRelayMatch.suggestedVolunteerName
                    )
                } else item
            }
        }
        addNotification(
            title = "Volunteer assigned to pickup",
            body = "Collection rider is en route with insulated carrier.",
            targetRole = UserRole.DONOR
        )
    }

    fun acceptVolunteerTask(donationId: String, volunteerName: String) {
        val target = _donations.value.find { it.id == donationId }
        _donations.update { list ->
            list.map { item ->
                if (item.id == donationId) {
                    item.copy(
                        status = DonationStatus.VOLUNTEER_ASSIGNED,
                        assignedVolunteerName = volunteerName
                    )
                } else item
            }
        }
        addNotification(
            title = "Collection accepted",
            body = "Use the 4-digit PIN or QR verification at pickup.",
            targetRole = UserRole.VOLUNTEER
        )
        emitMilestone(
            headline = "Relay Accepted",
            message = "Safe journey! Show your PIN or QR at ${target?.donorName ?: "the pickup kitchen"}.",
            isCelebration = false
        )
    }

    fun verifyPickup(donationId: String) {
        val target = _donations.value.find { it.id == donationId }
        _donations.update { list ->
            list.map { item ->
                if (item.id == donationId) {
                    item.copy(
                        status = DonationStatus.PICKED_UP,
                        pickupVerified = true
                    )
                } else item
            }
        }
        emitMilestone(
            headline = "Pickup Verified",
            message = "Got it! Keeping ${target?.title ?: "meals"} fresh on the way to ${target?.smartRelayMatch?.recipientOrgName ?: "the kitchen"}.",
            isCelebration = false
        )
    }

    fun verifyDonorHandover(donationId: String) {
        val target = _donations.value.find { it.id == donationId } ?: return
        if (target.status == DonationStatus.DELIVERED_VERIFIED) return

        _donations.update { list ->
            list.map { item ->
                if (item.id == donationId) {
                    item.copy(
                        status = DonationStatus.DELIVERED_VERIFIED,
                        pickupVerified = true,
                        deliveryVerified = true
                    )
                } else item
            }
        }

        _donorImpact.update { current ->
            current.copy(
                verifiedHandoversCount = current.verifiedHandoversCount + 1,
                measuredWeightKg = current.measuredWeightKg + target.quantityKg,
                estimatedMealsSupported = current.estimatedMealsSupported + target.servingsEstimate,
                estimatedValueLkr = current.estimatedValueLkr + (target.estimatedValueLkr ?: 0)
            )
        }

        val event = growFlowerForRole(
            role = UserRole.DONOR,
            earnedFromTitle = target.title,
            contributionSummary = "${"%.1f".format(target.quantityKg)} kg • ${target.servingsEstimate} servings → ${target.smartRelayMatch.recipientOrgName}",
            district = target.district
        )
        incrementCommunityGrove(target.district)
        _donationSuccessState.value = DonationSuccessState.Completed(
            eventId = System.nanoTime(),
            donationId = target.id,
            title = target.title,
            servingsEstimate = target.servingsEstimate,
            flowerSpecies = event?.flowerSpecies,
            reachedFullBloom = event?.reachedFullBloom == true
        )
    }

    fun verifyVolunteerDelivery(donationId: String) {
        val target = _donations.value.find { it.id == donationId } ?: return
        if (target.deliveryVerified) return

        _donations.update { list ->
            list.map { item ->
                if (item.id == donationId) {
                    item.copy(
                        status = DonationStatus.DELIVERED_VERIFIED,
                        pickupVerified = true,
                        deliveryVerified = true
                    )
                } else item
            }
        }

        _volunteerImpact.update { current ->
            current.copy(
                verifiedHandoversCount = current.verifiedHandoversCount + 1,
                measuredWeightKg = current.measuredWeightKg + target.quantityKg,
                estimatedMealsSupported = current.estimatedMealsSupported + target.servingsEstimate,
                estimatedValueLkr = current.estimatedValueLkr + (target.estimatedValueLkr ?: 0),
                deliveriesCompleted = current.deliveriesCompleted + 1
            )
        }

        val event = growFlowerForRole(
            role = UserRole.VOLUNTEER,
            earnedFromTitle = target.title,
            contributionSummary = "${"%.1f".format(target.quantityKg)} kg delivered to ${target.smartRelayMatch.recipientOrgName}",
            district = target.district
        )
        incrementCommunityGrove(target.district)
        if (event != null && !event.reachedFullBloom) {
            emitMilestone(
                headline = "Delivery Complete",
                message = "Thank you for riding! ${target.servingsEstimate} portions delivered safely — a ${event.flowerSpecies} bloomed.",
                isCelebration = true,
                actionLabel = "View Garden",
                actionRoute = "harvest_garden"
            )
        }
    }

    fun requestFoodAsRecipient(donationId: String, portions: Int) {
        val target = _donations.value.find { it.id == donationId }
        _donations.update { list ->
            list.map { item ->
                if (item.id == donationId) {
                    item.copy(
                        requestedByRecipient = true,
                        requestedPortions = portions,
                        status = if (item.status == DonationStatus.MATCHING) {
                            DonationStatus.VOLUNTEER_ASSIGNED
                        } else item.status,
                        assignedVolunteerName = item.assignedVolunteerName ?: item.smartRelayMatch.suggestedVolunteerName
                    )
                } else item
            }
        }
        addNotification(
            title = "Food request confirmed",
            body = "A volunteer courier is scheduled to bring your requested portions.",
            targetRole = UserRole.RECIPIENT
        )
        emitMilestone(
            headline = "Request Confirmed",
            message = "Reserved $portions portions of ${target?.title ?: "fresh food"}. A courier is on schedule.",
            isCelebration = false
        )
    }

    fun confirmRecipientReceipt(donationId: String) {
        val target = _donations.value.find { it.id == donationId }
        _donations.update { list ->
            list.map { item ->
                if (item.id == donationId) {
                    item.copy(
                        status = DonationStatus.DELIVERED_VERIFIED,
                        pickupVerified = true,
                        deliveryVerified = true
                    )
                } else item
            }
        }
        addNotification(
            title = "Receipt verified",
            body = "Thank you for confirming safe delivery.",
            targetRole = UserRole.RECIPIENT
        )
        emitMilestone(
            headline = "Safe Delivery Confirmed",
            message = "Ayubowan! Thank you for confirming \"${target?.title ?: "your meals"}\" arrived safely.",
            isCelebration = true
        )
    }

    fun selectGardenTheme(role: UserRole, theme: GardenTheme) {
        val targetFlow = if (role == UserRole.VOLUNTEER) _volunteerGarden else _donorGarden
        targetFlow.update { current ->
            if (theme in current.unlockedThemes) current.copy(activeTheme = theme) else current
        }
    }

    fun markAllNotificationsRead() {
        _notifications.update { list -> list.map { it.copy(isRead = true) } }
    }

    private fun growFlowerForRole(
        role: UserRole,
        earnedFromTitle: String,
        contributionSummary: String,
        district: String
    ): FlowerGrowthEvent? {
        val gardenFlow = if (role == UserRole.VOLUNTEER) _volunteerGarden else _donorGarden
        val current = gardenFlow.value
        val nextEmpty = current.slots.firstOrNull { !it.isBloomed } ?: return null
        val newSpecies = speciesPool[(nextEmpty.slotIndex + current.archivedGardens.size) % speciesPool.size]

        val updatedSlots = current.slots.map { slot ->
            if (slot.slotIndex == nextEmpty.slotIndex) {
                slot.copy(
                    isBloomed = true,
                    flowerSpecies = newSpecies,
                    earnedFromTitle = earnedFromTitle,
                    earnedDateLabel = "Today",
                    contributionSummary = contributionSummary,
                    district = district
                )
            } else slot
        }

        val allFilled = updatedSlots.all { it.isBloomed }
        if (allFilled) {
            val completedTheme = current.activeTheme
            val archiveId = "ARCH-${current.archivedGardens.size + 1}"
            val archived = ArchivedGarden(
                id = archiveId,
                theme = completedTheme,
                completedDate = "Today",
                badgeTitle = completedTheme.badgeTitle,
                flowers = updatedSlots,
                totalMealsCultivated = 135,
                totalWeightKg = 54.0
            )
            val allThemes = GardenTheme.entries
            val nextTheme = allThemes.firstOrNull { it !in current.unlockedThemes }
                ?: allThemes[(allThemes.indexOf(completedTheme) + 1) % allThemes.size]
            val updatedUnlocked = (current.unlockedThemes + nextTheme).distinct()

            gardenFlow.value = GardenState(
                activeTheme = nextTheme,
                slots = emptySixSlots(),
                archivedGardens = listOf(archived) + current.archivedGardens,
                unlockedThemes = updatedUnlocked
            )

            val event = FlowerGrowthEvent(
                role = role,
                slotIndex = nextEmpty.slotIndex,
                flowerSpecies = newSpecies,
                earnedFromTitle = earnedFromTitle,
                reachedFullBloom = true,
                completedTheme = completedTheme,
                nextUnlockedTheme = nextTheme,
                completedGardenFlowers = updatedSlots,
                completedArchiveId = archiveId
            )
            _latestFlowerEvent.value = event
            return event
        } else {
            gardenFlow.value = current.copy(slots = updatedSlots)
            val event = FlowerGrowthEvent(
                role = role,
                slotIndex = nextEmpty.slotIndex,
                flowerSpecies = newSpecies,
                earnedFromTitle = earnedFromTitle,
                reachedFullBloom = false
            )
            _latestFlowerEvent.value = event
            return event
        }
    }

    private fun incrementCommunityGrove(district: String) {
        _communityGrove.update { stats ->
            val newTotal = stats.totalCommunityFlowers + 1
            stats.copy(
                totalCommunityFlowers = newTotal,
                recentCommunityBlooms = listOf("Verified bloom in $district • Just now") +
                    stats.recentCommunityBlooms.take(3)
            )
        }
    }

    private fun addNotification(title: String, body: String, targetRole: UserRole?) {
        val note = AppNotification(
            id = "NOTIF-${System.currentTimeMillis()}",
            title = title,
            body = body,
            timeAgo = "Just now",
            isRead = false,
            targetRole = targetRole
        )
        _notifications.update { listOf(note) + it }
    }

    private fun buildSmartRelayMatch(
        category: FoodCategory,
        quantityKg: Double,
        district: String,
        dietaryTags: List<String>,
        pickupWindow: String
    ): SmartRelayMatch {
        val canonicalDistrict = normalizeSriLankaDistrict(district)
        val recipient = if (canonicalDistrict != "Colombo") {
            "$canonicalDistrict Community Kitchen" to canonicalDistrict
        } else {
            when (category) {
                FoodCategory.COOKED_MEALS -> "Sarana Community Kitchen" to "Maradana, Colombo 10"
                FoodCategory.BAKERY -> "Lotus Elders' Home" to "Dehiwala"
                FoodCategory.FRESH_PRODUCE -> "Methsevana Women's Co-op" to "Borella, Colombo 08"
                FoodCategory.SHORT_EATS -> "Viharamahadevi Youth Shelter" to "Colombo 07"
                FoodCategory.DRY_RATIONS -> "Sarana Community Kitchen" to "Colombo"
            }
        }
        val (recLatBase, recLngBase) = resolveSriLankaCoordinates(recipient.second)
        val recLat = recLatBase + 0.0085
        val recLng = recLngBase + 0.0075
        val volunteer = if (quantityKg >= 10.0) {
            "Kasun Perera" to "Tuk-Tuk (Covered Crate)"
        } else {
            "Nadeesha Fernando" to "E-Bike (Insulated Box)"
        }
        val dietaryLabel = dietaryTags.firstOrNull() ?: "Standard"
        return SmartRelayMatch(
            matchPercentage = 95,
            recipientOrgName = recipient.first,
            recipientDistrict = recipient.second,
            distanceKm = 2.1,
            urgency = UrgencyLevel.URGENT,
            suggestedVolunteerName = volunteer.first,
            volunteerVehicle = volunteer.second,
            summaryReason = "Strong match for ${category.label.lowercase()}, $dietaryLabel suitability, and 2.1 km proximity.",
            factors = listOf(
                MatchingFactor("Food Category & Demand", "${recipient.first} actively needs ${category.label}", 30),
                MatchingFactor("Distance & Pickup Window", "2.1 km route fits $pickupWindow", 25),
                MatchingFactor("Transport Capacity", "${volunteer.second} fits ${"%.1f".format(quantityKg)} kg", 20),
                MatchingFactor("Dietary Suitability", "$dietaryLabel verified compatible", 20)
            ),
            recipientLatitude = recLat,
            recipientLongitude = recLng
        )
    }

    private fun emptySixSlots(): List<FlowerSlot> = (0..5).map { idx ->
        FlowerSlot(
            slotIndex = idx,
            isBloomed = false,
            flowerSpecies = speciesPool[idx]
        )
    }

    private fun seedDonorGarden(): GardenState = GardenState(
        activeTheme = GardenTheme.SPRING_BLOSSOM,
        slots = listOf(
            FlowerSlot(0, true, "Ceylon Lotus", "24 Turmeric Rice & Curry Packs", "2 days ago", "9.6 kg • 24 meals → Sarana Kitchen", "Colombo"),
            FlowerSlot(1, true, "Temple Frangipani", "18 Roast Paan & Loaves", "3 days ago", "7.2 kg • 18 servings → Lotus Elders' Home", "Colombo"),
            FlowerSlot(2, true, "Golden Marigold", "12 kg Tropical Fruit Crate", "5 days ago", "12.0 kg • 30 portions → Methsevana Co-op", "Gampaha"),
            FlowerSlot(3, true, "Blue Water Lily", "30 Vegetable Roti Portions", "Last week", "6.0 kg • 30 servings → Youth Shelter", "Colombo"),
            FlowerSlot(4, true, "Cinnamon Jasmine", "15 Family Dinner Boxes", "Last week", "7.5 kg • 15 families → Maradana Kitchen", "Colombo"),
            FlowerSlot(5, false, "Kandyan Orchid", null, null)
        ),
        archivedGardens = listOf(
            ArchivedGarden(
                id = "ARCH-0",
                theme = GardenTheme.WILDFLOWER,
                completedDate = "Oct 2026",
                badgeTitle = "Full Bloom • Wildflower",
                totalMealsCultivated = 148,
                totalWeightKg = 56.4,
                flowers = listOf(
                    FlowerSlot(0, true, "Ceylon Lotus", "28 Vegetable Fried Rice Packs", "Oct 4", "11.2 kg • 28 meals → Sarana Community Kitchen", "Colombo"),
                    FlowerSlot(1, true, "Temple Frangipani", "22 Milk Rice & Lunu Miris Trays", "Oct 9", "8.4 kg • 22 meals → Borella Morning Shelter", "Colombo"),
                    FlowerSlot(2, true, "Golden Marigold", "16 kg King Coconut & Banana Harvest", "Oct 14", "16.0 kg • 32 servings → Methsevana Women's Co-op", "Gampaha"),
                    FlowerSlot(3, true, "Blue Water Lily", "24 String Hopper & Kiri Hodi Boxes", "Oct 19", "7.8 kg • 24 meals → Lotus Elders' Home", "Kalutara"),
                    FlowerSlot(4, true, "Cinnamon Jasmine", "20 Pol Sambol & Kurakkan Roti Packs", "Oct 24", "6.5 kg • 20 meals → Viharamahadevi Youth Shelter", "Colombo"),
                    FlowerSlot(5, true, "Kandyan Orchid", "22 Red Rice & Dhal Curry Parcels", "Oct 29", "6.5 kg • 22 meals → Kandy Relief Pantry", "Kandy")
                )
            )
        ),
        unlockedThemes = listOf(GardenTheme.SPRING_BLOSSOM, GardenTheme.WILDFLOWER)
    )

    private fun seedVolunteerGarden(): GardenState = GardenState(
        activeTheme = GardenTheme.SPRING_BLOSSOM,
        slots = listOf(
            FlowerSlot(0, true, "Ceylon Lotus", "Kollupitiya Bakery Relay", "Yesterday", "7.2 kg delivered to Lotus Elders' Home", "Colombo"),
            FlowerSlot(1, true, "Temple Frangipani", "Cinnamon Gardens Hot Meals", "3 days ago", "9.6 kg delivered to Sarana Kitchen", "Colombo"),
            FlowerSlot(2, true, "Golden Marigold", "Borella Produce Basket", "6 days ago", "14.0 kg delivered to Methsevana Co-op", "Colombo"),
            FlowerSlot(3, false, "Blue Water Lily", null, null),
            FlowerSlot(4, false, "Cinnamon Jasmine", null, null),
            FlowerSlot(5, false, "Kandyan Orchid", null, null)
        ),
        archivedGardens = listOf(
            ArchivedGarden(
                id = "ARCH-V0",
                theme = GardenTheme.SUNFLOWER,
                completedDate = "Oct 2026",
                badgeTitle = "Full Bloom • Sunflower",
                totalMealsCultivated = 122,
                totalWeightKg = 44.8,
                flowers = listOf(
                    FlowerSlot(0, true, "Ceylon Lotus", "Galle Face Evening Bakery Relay", "Oct 3", "6.4 kg via E-Bike → Lotus Elders' Home", "Colombo"),
                    FlowerSlot(1, true, "Temple Frangipani", "Nugegoda Community Kitchen Run", "Oct 8", "8.2 kg via Insulated Carrier → Sarana Kitchen", "Colombo"),
                    FlowerSlot(2, true, "Golden Marigold", "Mount Lavinia Fresh Greens Drop", "Oct 13", "9.5 kg → Dehiwala Care Home", "Colombo"),
                    FlowerSlot(3, true, "Blue Water Lily", "Kandy Temple Surplus Collection", "Oct 18", "7.8 kg → Peradeniya Relief Pantry", "Kandy"),
                    FlowerSlot(4, true, "Cinnamon Jasmine", "Galle Fort Pantry Bread Relay", "Oct 22", "5.9 kg → Southern Elders' Guild", "Galle"),
                    FlowerSlot(5, true, "Kandyan Orchid", "Borella Farmers' Produce Relay", "Oct 28", "7.0 kg → Methsevana Co-op", "Colombo")
                )
            )
        ),
        unlockedThemes = listOf(GardenTheme.SPRING_BLOSSOM, GardenTheme.SUNFLOWER)
    )

    private fun seedCommunityGrove(): CommunityGroveStats = CommunityGroveStats(
        totalCommunityFlowers = 342,
        milestoneGoal = 400,
        milestoneLabel = "Colombo & Kandy Monsoon Harvest Milestone",
        activeDonorsCount = 48,
        activeVolunteersCount = 73,
        recentCommunityBlooms = listOf(
            "Verified bloom in Colombo 07 • 18m ago",
            "Verified bloom in Kollupitiya • 45m ago",
            "Verified bloom in Kandy • 2h ago",
            "Verified bloom in Galle • 3h ago"
        )
    )

    private fun seedNotifications(): List<AppNotification> = listOf(
        AppNotification(
            id = "N-1",
            title = "Smart Relay Match: 96% Compatibility",
            body = "Sarana Community Kitchen is 1.8 km from your Cinnamon Gardens kitchen.",
            timeAgo = "12m ago",
            isRead = false,
            targetRole = UserRole.DONOR
        ),
        AppNotification(
            id = "N-2",
            title = "Urgent Nearby Pickup • 1.4 km",
            body = "24 Turmeric Rice & Curry Packs ready at Upali's Kitchen before 8:30 PM.",
            timeAgo = "18m ago",
            isRead = false,
            targetRole = UserRole.VOLUNTEER
        ),
        AppNotification(
            id = "N-3",
            title = "Fresh Meals Available Nearby",
            body = "24 vegetarian & halal rice & curry portions verified for safe collection.",
            timeAgo = "20m ago",
            isRead = false,
            targetRole = UserRole.RECIPIENT
        ),
        AppNotification(
            id = "N-4",
            title = "Harvest Garden Almost in Full Bloom!",
            body = "Complete 1 more verified handover to unlock Golden Harvest Garden.",
            timeAgo = "2h ago",
            isRead = true,
            targetRole = UserRole.DONOR
        )
    )

    private fun seedDonations(): List<FoodDonation> = listOf(
        FoodDonation(
            id = "DON-1041",
            title = "Turmeric Rice & Vegetable Curry Packs",
            category = FoodCategory.COOKED_MEALS,
            quantityKg = 9.6,
            servingsEstimate = 24,
            estimatedValueLkr = 12000,
            donorName = "Upali's Kitchen",
            donorType = "Restaurant Partner",
            locationName = "65 Ward Place, Cinnamon Gardens",
            district = "Colombo 07",
            pickupWindow = "Today, 7:00 PM – 8:30 PM",
            expiryNote = "Freshly cooked at 5:30 PM • Best before 10:30 PM",
            dietaryTags = listOf("Vegetarian", "Halal-friendly", "Hot Meal"),
            safetyConfirmed = true,
            photoResId = R.drawable.img_food_meals,
            status = DonationStatus.VOLUNTEER_ASSIGNED,
            handoverPin = "4829",
            qrPayload = "DEMETRA:DON-1041:PIN:4829",
            smartRelayMatch = SmartRelayMatch(
                matchPercentage = 96,
                recipientOrgName = "Sarana Community Kitchen",
                recipientDistrict = "Maradana, Colombo 10",
                distanceKm = 1.8,
                urgency = UrgencyLevel.URGENT,
                suggestedVolunteerName = "Nadeesha Fernando",
                volunteerVehicle = "E-Bike (Thermal Carrier)",
                summaryReason = "Thermal carrier keeps meals >60°C on a 1.8 km route matching Sarana's evening dinner demand.",
                factors = listOf(
                    MatchingFactor("Dietary & Portion Fit", "24 vegetarian packs match evening shelter count", 30),
                    MatchingFactor("Proximity & Urgency", "1.8 km transit completes within 1h 30m safe window", 28),
                    MatchingFactor("Thermal Equipment", "Volunteer carries insulated food-grade box", 20),
                    MatchingFactor("Recipient Readiness", "Receiving coordinator confirmed on-site", 18)
                ),
                recipientLatitude = 6.9296,
                recipientLongitude = 79.8669
            ),
            assignedVolunteerName = "Nadeesha Fernando",
            createdAtLabel = "25m ago",
            mapXFraction = 0.45f,
            mapYFraction = 0.42f,
            latitude = 6.9122,
            longitude = 79.8648
        ),
        FoodDonation(
            id = "DON-1042",
            title = "Artisan Roast Paan & Sourdough Basket",
            category = FoodCategory.BAKERY,
            quantityKg = 7.2,
            servingsEstimate = 18,
            estimatedValueLkr = 5400,
            donorName = "Perera & Sons Pantry",
            donorType = "Artisan Bakery",
            locationName = "112 Galle Road, Kollupitiya",
            district = "Colombo 03",
            pickupWindow = "Today, 7:30 PM – 9:30 PM",
            expiryNote = "Baked this morning • Best within 24 hours",
            dietaryTags = listOf("Vegetarian", "Contains Gluten"),
            safetyConfirmed = true,
            photoResId = R.drawable.img_food_bakery,
            status = DonationStatus.MATCHING,
            handoverPin = "7315",
            qrPayload = "DEMETRA:DON-1042:PIN:7315",
            smartRelayMatch = SmartRelayMatch(
                matchPercentage = 92,
                recipientOrgName = "Lotus Elders' Home",
                recipientDistrict = "Dehiwala",
                distanceKm = 3.4,
                urgency = UrgencyLevel.SOON,
                suggestedVolunteerName = "Kasun Perera",
                volunteerVehicle = "Tuk-Tuk (Sealed Crate)",
                summaryReason = "High demand for breakfast bread at Lotus Elders' Home, 3.4 km south along Galle Road.",
                factors = listOf(
                    MatchingFactor("Category Demand", "Elders' Home requested soft bread for breakfast", 30),
                    MatchingFactor("Corridor Route", "Direct 3.4 km route via Galle Road", 24),
                    MatchingFactor("Sealed Transport", "Covered crate protects from evening humidity", 20),
                    MatchingFactor("Shelf Window", "24h safe consumption window", 18)
                ),
                recipientLatitude = 6.8511,
                recipientLongitude = 79.8659
            ),
            createdAtLabel = "40m ago",
            mapXFraction = 0.28f,
            mapYFraction = 0.62f,
            latitude = 6.9102,
            longitude = 79.8505
        ),
        FoodDonation(
            id = "DON-1043",
            title = "Fresh Papayas, Mangoes & Gotukola Crate",
            category = FoodCategory.FRESH_PRODUCE,
            quantityKg = 14.0,
            servingsEstimate = 35,
            estimatedValueLkr = 6800,
            donorName = "Good Market Organic Stall",
            donorType = "Farmers' Collective",
            locationName = "D.S. Senanayake Mawatha, Borella",
            district = "Colombo 08",
            pickupWindow = "Today, 6:30 PM – 8:00 PM",
            expiryNote = "Harvested yesterday • Keep cool & shaded",
            dietaryTags = listOf("Vegan", "Organic", "Allergen-free"),
            safetyConfirmed = true,
            photoResId = R.drawable.img_food_produce,
            status = DonationStatus.MATCHING,
            handoverPin = "9104",
            qrPayload = "DEMETRA:DON-1043:PIN:9104",
            smartRelayMatch = SmartRelayMatch(
                matchPercentage = 94,
                recipientOrgName = "Methsevana Women's Co-op",
                recipientDistrict = "Rajagiriya",
                distanceKm = 2.6,
                urgency = UrgencyLevel.SOON,
                suggestedVolunteerName = "Ruwan Silva",
                volunteerVehicle = "Hatchback Car",
                summaryReason = "14 kg fresh produce suits car boot capacity and matches Methsevana's daily cooking.",
                factors = listOf(
                    MatchingFactor("Capacity Fit", "14 kg crate suited for car transport", 28),
                    MatchingFactor("Nutritional Need", "Fresh greens & fruit prioritized for co-op", 28),
                    MatchingFactor("Distance", "2.6 km eastbound pickup", 22),
                    MatchingFactor("Allergen-free", "Safe for all community kitchen residents", 16)
                ),
                recipientLatitude = 6.9096,
                recipientLongitude = 79.8950
            ),
            createdAtLabel = "1h ago",
            mapXFraction = 0.72f,
            mapYFraction = 0.48f,
            latitude = 6.9147,
            longitude = 79.8778
        ),
        FoodDonation(
            id = "DON-1044",
            title = "Vegetable Roti & Fish Cutlet Platter",
            category = FoodCategory.SHORT_EATS,
            quantityKg = 6.0,
            servingsEstimate = 30,
            estimatedValueLkr = 9000,
            donorName = "Galle Face Heritage Pantry",
            donorType = "Hospitality Partner",
            locationName = "2 Galle Face Center Rd, Colombo 01",
            district = "Colombo 01",
            pickupWindow = "Today, 6:00 PM – 7:45 PM",
            expiryNote = "Event surplus packed hygienically • Consume within 3h",
            dietaryTags = listOf("Halal-certified", "Contains Fish & Wheat"),
            safetyConfirmed = true,
            photoResId = R.drawable.img_food_shorteats,
            status = DonationStatus.MATCHING,
            handoverPin = "3652",
            qrPayload = "DEMETRA:DON-1044:PIN:3652",
            smartRelayMatch = SmartRelayMatch(
                matchPercentage = 90,
                recipientOrgName = "Viharamahadevi Youth Shelter",
                recipientDistrict = "Colombo 07",
                distanceKm = 2.9,
                urgency = UrgencyLevel.URGENT,
                suggestedVolunteerName = "Tharindu Jayawardena",
                volunteerVehicle = "Scooter (Food Box)",
                summaryReason = "Urgent 3h window paired with fast scooter courier 2.9 km away.",
                factors = listOf(
                    MatchingFactor("Urgency Speed", "Scooter rider nearby for fast 12-min pickup", 30),
                    MatchingFactor("Halal Suitability", "Certified Halal platter matches shelter policy", 24),
                    MatchingFactor("Portion Count", "30 portions fit youth evening tea", 20),
                    MatchingFactor("Allergen Labeled", "Fish & wheat clearly tagged", 16)
                ),
                recipientLatitude = 6.9135,
                recipientLongitude = 79.8608
            ),
            createdAtLabel = "1h 15m ago",
            mapXFraction = 0.22f,
            mapYFraction = 0.24f,
            latitude = 6.9344,
            longitude = 79.8428
        ),
        FoodDonation(
            id = "DON-1045",
            title = "Kandy Hill-Country Jackfruit & Red Rice Parcels",
            category = FoodCategory.COOKED_MEALS,
            quantityKg = 11.5,
            servingsEstimate = 28,
            donorName = "Queen's Heritage Kitchen",
            donorType = "Hotel Partner",
            locationName = "Dalada Veediya, Kandy",
            district = "Kandy",
            pickupWindow = "Today, 6:30 PM – 8:15 PM",
            expiryNote = "Kept hot in insulated cambros • Consume within 3h",
            dietaryTags = listOf("Vegetarian", "Vegan", "Halal-friendly"),
            safetyConfirmed = true,
            photoResId = R.drawable.img_food_meals,
            status = DonationStatus.MATCHING,
            handoverPin = "5284",
            qrPayload = "DEMETRA:DON-1045:PIN:5284",
            smartRelayMatch = SmartRelayMatch(
                matchPercentage = 95,
                recipientOrgName = "Peradeniya Relief Pantry",
                recipientDistrict = "Peradeniya, Kandy",
                distanceKm = 4.2,
                urgency = UrgencyLevel.URGENT,
                suggestedVolunteerName = "Chamara Bandara",
                volunteerVehicle = "Scooter (Thermal Bag)",
                summaryReason = "4.2 km direct corridor from Dalada Veediya to Peradeniya Relief Pantry.",
                factors = listOf(
                    MatchingFactor("Dietary & Portion Fit", "28 vegan rice packs fit evening pantry meal", 32),
                    MatchingFactor("Hill Corridor Route", "4.2 km straight run along Peradeniya Rd", 26),
                    MatchingFactor("Urgency Speed", "Thermal rider ready nearby", 22),
                    MatchingFactor("Safety Verified", "Food-grade sealed boxes", 15)
                ),
                recipientLatitude = 7.2699,
                recipientLongitude = 80.5938
            ),
            createdAtLabel = "32m ago",
            mapXFraction = 0.54f,
            mapYFraction = 0.44f,
            latitude = 7.2936,
            longitude = 80.6350
        ),
        FoodDonation(
            id = "DON-1046",
            title = "Southern Bakery Buns & Coconut Bread Basket",
            category = FoodCategory.BAKERY,
            quantityKg = 8.0,
            servingsEstimate = 26,
            donorName = "Fort Rampart Bakehouse",
            donorType = "Artisan Bakery",
            locationName = "Lighthouse Street, Galle Fort",
            district = "Galle",
            pickupWindow = "Today, 5:45 PM – 7:30 PM",
            expiryNote = "Afternoon bake • Sealed paper crates",
            dietaryTags = listOf("Vegetarian", "Halal-friendly"),
            safetyConfirmed = true,
            photoResId = R.drawable.img_food_bakery,
            status = DonationStatus.VOLUNTEER_ASSIGNED,
            handoverPin = "6193",
            qrPayload = "DEMETRA:DON-1046:PIN:6193",
            smartRelayMatch = SmartRelayMatch(
                matchPercentage = 93,
                recipientOrgName = "Southern Elders' Guild",
                recipientDistrict = "Karapitiya, Galle",
                distanceKm = 3.1,
                urgency = UrgencyLevel.SOON,
                suggestedVolunteerName = "Dilshan Mendis",
                volunteerVehicle = "E-Bike (Sealed Box)",
                summaryReason = "3.1 km route from Galle Fort to Southern Elders' Guild evening tea.",
                factors = listOf(
                    MatchingFactor("Category Match", "Fresh soft bread for elders' evening tea", 30),
                    MatchingFactor("Short Distance", "3.1 km coastal road route", 26),
                    MatchingFactor("Protected Packaging", "Moisture-sealed crates", 20),
                    MatchingFactor("Volunteer Assigned", "Dilshan en route", 17)
                ),
                recipientLatitude = 6.0652,
                recipientLongitude = 80.2264
            ),
            assignedVolunteerName = "Dilshan Mendis",
            createdAtLabel = "50m ago",
            mapXFraction = 0.36f,
            mapYFraction = 0.78f,
            latitude = 6.0267,
            longitude = 80.2170
        ),
        FoodDonation(
            id = "DON-1047",
            title = "Negombo Coastal Produce & Plantain Crate",
            category = FoodCategory.FRESH_PRODUCE,
            quantityKg = 15.0,
            servingsEstimate = 38,
            donorName = "Negombo Harbour Green Market",
            donorType = "Farmers' Collective",
            locationName = "Main Street, Negombo",
            district = "Gampaha",
            pickupWindow = "Today, 6:00 PM – 8:30 PM",
            expiryNote = "Morning harvest • Shaded crates",
            dietaryTags = listOf("Vegan", "Organic", "Allergen-free"),
            safetyConfirmed = true,
            photoResId = R.drawable.img_food_produce,
            status = DonationStatus.MATCHING,
            handoverPin = "2048",
            qrPayload = "DEMETRA:DON-1047:PIN:2048",
            smartRelayMatch = SmartRelayMatch(
                matchPercentage = 91,
                recipientOrgName = "Gampaha St. Jude Community Kitchen",
                recipientDistrict = "Gampaha",
                distanceKm = 5.4,
                urgency = UrgencyLevel.SOON,
                suggestedVolunteerName = "Sanjaya Fernando",
                volunteerVehicle = "Tuk-Tuk (Covered Bed)",
                summaryReason = "15 kg fresh vegetables matched with St. Jude Kitchen community dinner prep.",
                factors = listOf(
                    MatchingFactor("Produce Volume", "15 kg fresh staples fit community kitchen prep", 30),
                    MatchingFactor("Cargo Fit", "Tuk-tuk covered bed handles large crates", 24),
                    MatchingFactor("Allergen-free", "Whole fresh produce", 20),
                    MatchingFactor("Pickup Window", "2.5h flexible afternoon window", 17)
                ),
                recipientLatitude = 7.0917,
                recipientLongitude = 79.9950
            ),
            createdAtLabel = "1h 10m ago",
            mapXFraction = 0.25f,
            mapYFraction = 0.35f,
            latitude = 7.2008,
            longitude = 79.8737
        ),
        FoodDonation(
            id = "DON-1048",
            title = "Jaffna Vegetarian Dosai & Sambar Packs",
            category = FoodCategory.COOKED_MEALS,
            quantityKg = 8.4,
            servingsEstimate = 25,
            donorName = "Nallur Annadanam Kitchen",
            donorType = "Community Partner",
            locationName = "Temple Road, Nallur, Jaffna",
            district = "Jaffna",
            pickupWindow = "Today, 6:15 PM – 7:45 PM",
            expiryNote = "Freshly steamed • Consume within 3 hours",
            dietaryTags = listOf("Vegetarian", "Vegan", "Halal-friendly"),
            safetyConfirmed = true,
            photoResId = R.drawable.img_food_meals,
            status = DonationStatus.MATCHING,
            handoverPin = "8471",
            qrPayload = "DEMETRA:DON-1048:PIN:8471",
            smartRelayMatch = SmartRelayMatch(
                matchPercentage = 96,
                recipientOrgName = "Nallur Children's Study Home",
                recipientDistrict = "Jaffna",
                distanceKm = 1.5,
                urgency = UrgencyLevel.URGENT,
                suggestedVolunteerName = "Kugan Selvarajah",
                volunteerVehicle = "E-Bike (Thermal Carrier)",
                summaryReason = "1.5 km hyper-local delivery of 25 warm vegetarian meal packs.",
                factors = listOf(
                    MatchingFactor("Local Proximity", "1.5 km bike route in Nallur", 32),
                    MatchingFactor("Dietary Match", "100% vegetarian warm meal packs", 28),
                    MatchingFactor("Urgency Speed", "Thermal box preserves heat", 20),
                    MatchingFactor("Confirmed Shelter", "Coordinator standing by", 16)
                ),
                recipientLatitude = 9.6645,
                recipientLongitude = 80.0220
            ),
            createdAtLabel = "20m ago",
            mapXFraction = 0.42f,
            mapYFraction = 0.14f,
            latitude = 9.6742,
            longitude = 80.0294
        )
    )
}
