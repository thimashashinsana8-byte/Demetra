package com.example.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.example.R

enum class UserRole(
    val title: String,
    val subtitle: String,
    val description: String
) {
    DONOR(
        title = "Donor",
        subtitle = "Restaurants, hotels, bakeries & homes",
        description = "Share surplus wholesome food easily with verified local kitchens and charities."
    ),
    VOLUNTEER(
        title = "Volunteer",
        subtitle = "Community riders & delivery helpers",
        description = "Discover nearby pickups, follow clear route guides, and verify safe handovers."
    ),
    RECIPIENT(
        title = "Recipient",
        subtitle = "Community kitchens, shelters & charities",
        description = "Browse suitable food nearby, request portions respectfully, and confirm receipt."
    )
}

enum class VolunteerInterestArea(
    val label: String,
    val description: String
) {
    HOT_MEAL_RESCUE(
        label = "Hot Meal Rescue",
        description = "Rapid insulated pickups from restaurants, hotels & events"
    ),
    BAKERY_EVENING(
        label = "Bakery & Evening Runs",
        description = "End-of-day bread, short eats & pastry collection"
    ),
    FRESH_PRODUCE(
        label = "Fresh Produce & Markets",
        description = "Fruit, vegetable & market surplus redistribution"
    ),
    URGENT_DISPATCH(
        label = "Urgent Dispatch (<2h)",
        description = "Priority time-sensitive food rescue routes"
    ),
    COMMUNITY_DRIVES(
        label = "Community & Weekend Drives",
        description = "Neighborhood temple, school & pantry deliveries"
    ),
    SORTING_PACKING(
        label = "Sorting & Safe Packing",
        description = "Food hygiene checks & meal parcel preparation"
    )
}

enum class FoodCategory(
    val label: String,
    val safetyNote: String,
    @DrawableRes val defaultImageRes: Int
) {
    COOKED_MEALS(
        label = "Cooked Meals",
        safetyNote = "Keep hot above 60°C or chill below 5°C; consume within 3 hours.",
        defaultImageRes = R.drawable.img_food_meals
    ),
    BAKERY(
        label = "Bakery & Bread",
        safetyNote = "Store in sealed food-grade bags away from direct humidity.",
        defaultImageRes = R.drawable.img_food_bakery
    ),
    FRESH_PRODUCE(
        label = "Fresh Produce",
        safetyNote = "Inspect for freshness; keep leafy greens shaded during transport.",
        defaultImageRes = R.drawable.img_food_produce
    ),
    SHORT_EATS(
        label = "Short Eats",
        safetyNote = "Transport in ventilated containers; verify allergen labels.",
        defaultImageRes = R.drawable.img_food_shorteats
    ),
    DRY_RATIONS(
        label = "Dry Rations",
        safetyNote = "Verify sealed packaging and best-before dates prior to handover.",
        defaultImageRes = R.drawable.img_food_produce
    )
}

enum class DonationStatus(val label: String, val stepIndex: Int) {
    MATCHING("Smart Relay Matching", 0),
    VOLUNTEER_ASSIGNED("Volunteer Assigned", 1),
    PICKED_UP("In Transit", 2),
    DELIVERED_VERIFIED("Verified Handover", 3)
}

enum class UrgencyLevel(val label: String) {
    URGENT("Urgent • < 2h"),
    SOON("Today • < 4h"),
    FLEXIBLE("Flexible Window")
}

@Immutable
data class MatchingFactor(
    val title: String,
    val detail: String,
    val scoreContribution: Int
)

@Immutable
data class SmartRelayMatch(
    val matchPercentage: Int,
    val recipientOrgName: String,
    val recipientDistrict: String,
    val distanceKm: Double,
    val urgency: UrgencyLevel,
    val suggestedVolunteerName: String,
    val volunteerVehicle: String,
    val summaryReason: String,
    val factors: List<MatchingFactor>,
    val recipientLatitude: Double = 6.9319,
    val recipientLongitude: Double = 79.8650
)

@Immutable
data class FoodDonation(
    val id: String,
    val title: String,
    val category: FoodCategory,
    val quantityKg: Double,
    val servingsEstimate: Int,
    val estimatedValueLkr: Int? = null,
    val donorName: String,
    val donorType: String,
    val locationName: String,
    val district: String,
    val pickupWindow: String,
    val expiryNote: String,
    val dietaryTags: List<String>,
    val safetyConfirmed: Boolean,
    @DrawableRes val photoResId: Int,
    val pickedPhotoUri: String? = null,
    val status: DonationStatus,
    val handoverPin: String,
    val qrPayload: String,
    val smartRelayMatch: SmartRelayMatch,
    val assignedVolunteerName: String? = null,
    val pickupVerified: Boolean = false,
    val deliveryVerified: Boolean = false,
    val requestedByRecipient: Boolean = false,
    val requestedPortions: Int = 0,
    val createdAtLabel: String,
    val mapXFraction: Float = 0.5f,
    val mapYFraction: Float = 0.5f,
    val latitude: Double = 6.9271,
    val longitude: Double = 79.8612
)

enum class GardenTheme(
    val title: String,
    val subtitle: String,
    val badgeTitle: String
) {
    SPRING_BLOSSOM(
        title = "Spring Blossom Garden",
        subtitle = "Soft lotus, frangipani & jasmine blooms",
        badgeTitle = "Full Bloom • Spring Blossom"
    ),
    GOLDEN_HARVEST(
        title = "Golden Harvest Garden",
        subtitle = "Sunlit marigolds & Ceylon paddy blossoms",
        badgeTitle = "Full Bloom • Golden Harvest"
    ),
    WILDFLOWER(
        title = "Wildflower Garden",
        subtitle = "Hill-country orchids & meadow blooms",
        badgeTitle = "Full Bloom • Wildflower"
    ),
    SUNFLOWER(
        title = "Sunflower Garden",
        subtitle = "Radiant golden blooms of community warmth",
        badgeTitle = "Full Bloom • Sunflower"
    ),
    ORCHARD(
        title = "Orchard Garden",
        subtitle = "Fruiting mango & citrus blossoms",
        badgeTitle = "Full Bloom • Orchard"
    )
}

@Immutable
data class FlowerSlot(
    val slotIndex: Int,
    val isBloomed: Boolean,
    val flowerSpecies: String,
    val earnedFromTitle: String? = null,
    val earnedDateLabel: String? = null,
    val contributionSummary: String? = null,
    val district: String? = null
)

@Immutable
data class ArchivedGarden(
    val id: String,
    val theme: GardenTheme,
    val completedDate: String,
    val badgeTitle: String,
    val flowers: List<FlowerSlot>,
    val totalMealsCultivated: Int = 0,
    val totalWeightKg: Double = 0.0
)

@Immutable
data class GardenState(
    val activeTheme: GardenTheme,
    val slots: List<FlowerSlot>,
    val archivedGardens: List<ArchivedGarden>,
    val unlockedThemes: List<GardenTheme>
) {
    val bloomedCount: Int get() = slots.count { it.isBloomed }
}

@Immutable
data class CommunityGroveStats(
    val totalCommunityFlowers: Int,
    val milestoneGoal: Int,
    val milestoneLabel: String,
    val activeDonorsCount: Int,
    val activeVolunteersCount: Int,
    val recentCommunityBlooms: List<String>
)

@Immutable
data class ImpactSummary(
    val verifiedHandoversCount: Int,
    val measuredWeightKg: Double,
    val estimatedMealsSupported: Int,
    val estimatedValueLkr: Int,
    val deliveriesCompleted: Int
)

@Immutable
data class UserProfile(
    val fullName: String,
    val organization: String,
    val district: String,
    val phone: String,
    val vehicleOrDietaryNote: String,
    val interestAreas: List<String> = emptyList(),
    val isAuthenticated: Boolean = false
)

@Immutable
data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val timeAgo: String,
    val isRead: Boolean,
    val targetRole: UserRole?
)

@Immutable
data class PipSuggestion(
    val label: String,
    val prompt: String
)

@Immutable
data class PipChatMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val isStreaming: Boolean = false,
    val quickActionLabel: String? = null,
    val quickActionRoute: String? = null
)

@Immutable
data class PipMilestoneEvent(
    val id: Long,
    val headline: String,
    val message: String,
    val isCelebration: Boolean = false,
    val actionLabel: String? = null,
    val actionRoute: String? = null
)

sealed interface DonationSuccessState {
    data object Idle : DonationSuccessState

    @Immutable
    data class Published(
        val eventId: Long,
        val donationId: String,
        val title: String,
        val recipientOrgName: String,
        val matchPercentage: Int
    ) : DonationSuccessState

    @Immutable
    data class Completed(
        val eventId: Long,
        val donationId: String,
        val title: String,
        val servingsEstimate: Int,
        val flowerSpecies: String?,
        val reachedFullBloom: Boolean
    ) : DonationSuccessState
}

@Immutable
data class SriLankaDistrict(
    val name: String,
    val province: String,
    val latitude: Double,
    val longitude: Double
)

val SRI_LANKA_DISTRICTS: List<SriLankaDistrict> = listOf(
    SriLankaDistrict("Ampara", "Eastern Province", 7.2975, 81.6820),
    SriLankaDistrict("Anuradhapura", "North Central Province", 8.3114, 80.4037),
    SriLankaDistrict("Badulla", "Uva Province", 6.9934, 81.0550),
    SriLankaDistrict("Batticaloa", "Eastern Province", 7.7310, 81.6747),
    SriLankaDistrict("Colombo", "Western Province", 6.9271, 79.8612),
    SriLankaDistrict("Galle", "Southern Province", 6.0535, 80.2210),
    SriLankaDistrict("Gampaha", "Western Province", 7.0873, 80.0144),
    SriLankaDistrict("Hambantota", "Southern Province", 6.1429, 81.1212),
    SriLankaDistrict("Jaffna", "Northern Province", 9.6615, 80.0255),
    SriLankaDistrict("Kalutara", "Western Province", 6.5854, 79.9607),
    SriLankaDistrict("Kandy", "Central Province", 7.2906, 80.6337),
    SriLankaDistrict("Kegalle", "Sabaragamuwa Province", 7.2513, 80.3464),
    SriLankaDistrict("Kilinochchi", "Northern Province", 9.3803, 80.3770),
    SriLankaDistrict("Kurunegala", "North Western Province", 7.4863, 80.3623),
    SriLankaDistrict("Mannar", "Northern Province", 8.9810, 79.9044),
    SriLankaDistrict("Matale", "Central Province", 7.4675, 80.6234),
    SriLankaDistrict("Matara", "Southern Province", 5.9549, 80.5550),
    SriLankaDistrict("Monaragala", "Uva Province", 6.8728, 81.3507),
    SriLankaDistrict("Mullaitivu", "Northern Province", 9.2671, 80.8142),
    SriLankaDistrict("Nuwara Eliya", "Central Province", 6.9497, 80.7891),
    SriLankaDistrict("Polonnaruwa", "North Central Province", 7.9403, 81.0188),
    SriLankaDistrict("Puttalam", "North Western Province", 8.0408, 79.8394),
    SriLankaDistrict("Ratnapura", "Sabaragamuwa Province", 6.7056, 80.3847),
    SriLankaDistrict("Trincomalee", "Eastern Province", 8.5874, 81.2152),
    SriLankaDistrict("Vavuniya", "Northern Province", 8.7542, 80.4982)
)

private val LOCALITY_TO_COORDS: Map<String, Pair<Double, Double>> = mapOf(
    "colombo 01" to (6.9344 to 79.8428),
    "colombo 03" to (6.9102 to 79.8505),
    "colombo 07" to (6.9122 to 79.8648),
    "colombo 08" to (6.9147 to 79.8778),
    "maradana, colombo 10" to (6.9296 to 79.8669),
    "dehiwala" to (6.8511 to 79.8659),
    "rajagiriya" to (6.9096 to 79.8950),
    "peradeniya" to (7.2699 to 80.5938),
    "fort, galle" to (6.0267 to 80.2170),
    "negombo" to (7.2008 to 79.8737),
    "nallur, jaffna" to (9.6742 to 80.0294)
)

private val DISTRICT_LOWER_PAIRS: List<Pair<String, SriLankaDistrict>> =
    SRI_LANKA_DISTRICTS.map { it.name.lowercase() to it }

private val DISTRICT_BY_LOWER: Map<String, SriLankaDistrict> =
    SRI_LANKA_DISTRICTS.associateBy { it.name.lowercase() }

private val DISTRICT_COORDS_BY_LOWER: Map<String, Pair<Double, Double>> =
    SRI_LANKA_DISTRICTS.associate { it.name.lowercase() to (it.latitude to it.longitude) }

fun findSriLankaDistrict(districtOrLocality: String): SriLankaDistrict? {
    val lower = districtOrLocality.trim().lowercase()
    if (lower.isEmpty()) return null
    DISTRICT_BY_LOWER[lower]?.let { return it }
    return DISTRICT_LOWER_PAIRS.firstOrNull { (key, _) -> lower.contains(key) }?.second
}

fun normalizeSriLankaDistrict(districtOrLocality: String): String {
    val trimmed = districtOrLocality.trim()
    if (trimmed.isEmpty()) return "Colombo"
    val lower = trimmed.lowercase()
    DISTRICT_BY_LOWER[lower]?.let { return it.name }
    DISTRICT_LOWER_PAIRS.firstOrNull { (key, _) -> lower.contains(key) }?.let { return it.second.name }
    return when {
        lower.contains("dehiwala") || lower.contains("rajagiriya") || lower.contains("nugegoda") ||
            lower.contains("borella") || lower.contains("kollupitiya") || lower.contains("maradana") -> "Colombo"
        lower.contains("peradeniya") -> "Kandy"
        lower.contains("negombo") -> "Gampaha"
        else -> trimmed
    }
}

fun resolveSriLankaCoordinates(
    districtOrLocality: String,
    seedOffsetIndex: Int = 0
): Pair<Double, Double> {
    val lower = districtOrLocality.trim().lowercase()
    val base = LOCALITY_TO_COORDS[lower]
        ?: DISTRICT_COORDS_BY_LOWER[lower]
        ?: DISTRICT_LOWER_PAIRS.firstOrNull { (key, _) -> lower.contains(key) }?.second?.let { it.latitude to it.longitude }
        ?: (6.9271 to 79.8612)
    if (seedOffsetIndex == 0) return base
    val latDelta = ((seedOffsetIndex % 5) - 2) * 0.0045
    val lngDelta = (((seedOffsetIndex / 5) % 5) - 2) * 0.0045
    return (base.first + latDelta) to (base.second + lngDelta)
}


