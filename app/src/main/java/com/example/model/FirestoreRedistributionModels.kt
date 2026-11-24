package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class FoodRequestStatus(val label: String) {
    PENDING("Pending Review"),
    APPROVED("Approved"),
    IN_TRANSIT("In Transit"),
    FULFILLED("Fulfilled"),
    CANCELLED("Cancelled")
}

sealed interface RedistributionUiState<out T> {
    data object Loading : RedistributionUiState<Nothing>
    data class Success<T>(val data: T) : RedistributionUiState<T>
    data class Error(val message: String) : RedistributionUiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserAccountEntity(
    @DocumentId val id: String = "",
    val uid: String = "",
    val fullName: String = "",
    val organization: String = "",
    val district: String = "Colombo",
    val email: String = "",
    val phone: String = "",
    val activeRole: String = UserRole.DONOR.name,
    val vehicleOrDietaryNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedRole(): UserRole =
        UserRole.entries.firstOrNull { it.name == activeRole } ?: UserRole.DONOR

    fun toCreateMap(): Map<String, Any> = mapOf(
        "uid" to uid.take(128),
        "fullName" to fullName.trim().take(120),
        "organization" to organization.trim().take(140),
        "district" to district.trim().take(60),
        "email" to email.trim().take(160),
        "phone" to phone.trim().take(32),
        "activeRole" to resolvedRole().name,
        "vehicleOrDietaryNote" to vehicleOrDietaryNote.trim().take(200),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class DonorEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val organizationName: String = "",
    val donorType: String = "Verified Food Donor",
    val district: String = "Colombo",
    val locationName: String = "",
    val typicalSurplus: String = "",
    val verified: Boolean = true,
    val totalHandovers: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId.take(128),
        "organizationName" to organizationName.trim().take(140),
        "donorType" to donorType.trim().take(80),
        "district" to district.trim().take(60),
        "locationName" to locationName.trim().take(160),
        "typicalSurplus" to typicalSurplus.trim().take(200),
        "verified" to verified,
        "totalHandovers" to totalHandovers.coerceIn(0, 100_000),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class VolunteerEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val communityGroup: String = "",
    val district: String = "Colombo",
    val preferredDistrict: String = "Colombo",
    val interestAreas: List<String> = emptyList(),
    val vehicleType: String = "E-Bike (Insulated Box)",
    val carrierCapacityKg: Double = 12.0,
    val availabilityWindow: String = "Flexible (Anytime)",
    val available: Boolean = true,
    val deliveriesCompleted: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val resolvedPreferredDistrict = preferredDistrict.trim()
            .ifBlank { district.trim().ifBlank { "Colombo" } }
            .take(60)
        val resolvedDistrict = district.trim()
            .ifBlank { resolvedPreferredDistrict }
            .take(60)
        val sanitizedInterests = interestAreas
            .map { it.trim().take(60) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(8)
            .ifEmpty { listOf(VolunteerInterestArea.HOT_MEAL_RESCUE.label) }
        return mapOf(
            "userId" to userId.take(128),
            "displayName" to displayName.trim().take(120),
            "communityGroup" to communityGroup.trim().take(140),
            "district" to resolvedDistrict,
            "preferredDistrict" to resolvedPreferredDistrict,
            "interestAreas" to sanitizedInterests,
            "vehicleType" to vehicleType.trim().take(100),
            "carrierCapacityKg" to carrierCapacityKg.coerceIn(0.5, 500.0),
            "availabilityWindow" to availabilityWindow.trim().ifBlank { "Flexible (Anytime)" }.take(80),
            "available" to available,
            "deliveriesCompleted" to deliveriesCompleted.coerceIn(0, 100_000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@Keep
@IgnoreExtraProperties
data class FoodDonationEntity(
    @DocumentId val id: String = "",
    val donorUserId: String = "",
    val donorName: String = "",
    val donorType: String = "Verified Food Donor",
    val title: String = "",
    val category: String = FoodCategory.COOKED_MEALS.name,
    val quantityKg: Double = 1.0,
    val servingsEstimate: Int = 1,
    val locationName: String = "",
    val district: String = "Colombo",
    val pickupWindow: String = "",
    val expiryNote: String = "",
    val dietaryTags: List<String> = emptyList(),
    val safetyConfirmed: Boolean = true,
    val status: String = DonationStatus.MATCHING.name,
    val handoverPin: String = "0000",
    val pickupVerified: Boolean = false,
    val deliveryVerified: Boolean = false,
    val assignedVolunteerUserId: String? = null,
    val assignedVolunteerName: String? = null,
    val recipientUserId: String? = null,
    val recipientOrgName: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedCategory(): FoodCategory =
        FoodCategory.entries.firstOrNull { it.name == category } ?: FoodCategory.COOKED_MEALS

    fun resolvedStatus(): DonationStatus =
        DonationStatus.entries.firstOrNull { it.name == status } ?: DonationStatus.MATCHING

    fun toCreateMap(): Map<String, Any> = mapOf<String, Any?>(
        "donorUserId" to donorUserId.take(128),
        "donorName" to donorName.trim().take(140),
        "donorType" to donorType.trim().take(80),
        "title" to title.trim().take(160),
        "category" to resolvedCategory().name,
        "quantityKg" to quantityKg.coerceIn(0.1, 5000.0),
        "servingsEstimate" to servingsEstimate.coerceIn(1, 50_000),
        "locationName" to locationName.trim().take(180),
        "district" to district.trim().take(60),
        "pickupWindow" to pickupWindow.trim().take(120),
        "expiryNote" to expiryNote.trim().take(240),
        "dietaryTags" to dietaryTags.take(12),
        "safetyConfirmed" to safetyConfirmed,
        "status" to DonationStatus.MATCHING.name,
        "handoverPin" to handoverPin.padStart(4, '0').take(4),
        "pickupVerified" to false,
        "deliveryVerified" to false,
        "assignedVolunteerUserId" to assignedVolunteerUserId?.take(128),
        "assignedVolunteerName" to assignedVolunteerName?.trim()?.take(120),
        "recipientUserId" to recipientUserId?.take(128),
        "recipientOrgName" to recipientOrgName?.trim()?.take(140),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class FoodRequestEntity(
    @DocumentId val id: String = "",
    val donationId: String = "",
    val donationTitle: String = "",
    val recipientUserId: String = "",
    val recipientOrgName: String = "",
    val donorUserId: String = "",
    val volunteerUserId: String? = null,
    val volunteerName: String? = null,
    val district: String = "Colombo",
    val requestedPortions: Int = 1,
    val dietaryNotes: String = "",
    val status: String = FoodRequestStatus.PENDING.name,
    val participantIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedStatus(): FoodRequestStatus =
        FoodRequestStatus.entries.firstOrNull { it.name == status } ?: FoodRequestStatus.PENDING

    fun toCreateMap(): Map<String, Any> = mapOf<String, Any?>(
        "donationId" to donationId.take(128),
        "donationTitle" to donationTitle.trim().take(160),
        "recipientUserId" to recipientUserId.take(128),
        "recipientOrgName" to recipientOrgName.trim().take(140),
        "donorUserId" to donorUserId.take(128),
        "volunteerUserId" to volunteerUserId?.take(128),
        "volunteerName" to volunteerName?.trim()?.take(120),
        "district" to district.trim().take(60),
        "requestedPortions" to requestedPortions.coerceIn(1, 5000),
        "dietaryNotes" to dietaryNotes.trim().take(240),
        "status" to FoodRequestStatus.PENDING.name,
        "participantIds" to listOf(recipientUserId, donorUserId).distinct().take(3),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}
