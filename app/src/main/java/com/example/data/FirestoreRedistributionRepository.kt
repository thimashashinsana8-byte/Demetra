package com.example.data

import android.content.Context
import com.example.R
import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Cloud Firestore repository managing food redistribution entities across Sri Lanka:
 * - User Accounts (`/users/{userId}`)
 * - Donors (`/donors/{donorId}`)
 * - Volunteers (`/volunteers/{volunteerId}`)
 * - Surplus Food Donations (`/donations/{donationId}`)
 * - Recipient Food Requests (`/requests/{requestId}`)
 */
class FirestoreRedistributionRepository(
    private val dbProvider: () -> FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private val db: FirebaseFirestore by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { dbProvider() }

    constructor(
        db: FirebaseFirestore,
        auth: FirebaseAuth = Firebase.auth
    ) : this(dbProvider = { db }, auth = auth)

    constructor(context: Context) : this(
        dbProvider = {
            FirebaseFirestore.getInstance(
                context.applicationContext.getString(R.string.firestore_database_id)
            )
        },
        auth = Firebase.auth
    )

    companion object {
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_DONORS = "donors"
        private const val COLLECTION_VOLUNTEERS = "volunteers"
        private const val COLLECTION_DONATIONS = "donations"
        private const val COLLECTION_REQUESTS = "requests"
        private const val DEFAULT_QUERY_LIMIT = 50L
    }

    private fun requireUid(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Google Sign-In required before accessing cloud redistribution data.")

    private fun isValidDocId(id: String): Boolean =
        id.isNotBlank() && id.length <= 128 && !id.contains('/') && !id.contains('\\') && !id.contains("..")

    // =========================================================================
    // User Account Profile (/users/{userId})
    // =========================================================================

    fun observeUserAccount(uid: String): Flow<RedistributionUiState<UserAccountEntity?>> {
        if (!isValidDocId(uid)) {
            return kotlinx.coroutines.flow.flowOf(RedistributionUiState.Error("Invalid user ID"))
        }
        val path = "$COLLECTION_USERS/$uid"
        return db.collection(COLLECTION_USERS)
            .document(uid)
            .snapshots(MetadataChanges.EXCLUDE)
            .map<DocumentSnapshot, RedistributionUiState<UserAccountEntity?>> { snapshot ->
                val entity = snapshot.toObject(
                    UserAccountEntity::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                RedistributionUiState.Success(entity)
            }
            .catch { error ->
                emitFirestoreStreamError(error, OperationType.GET, path)
            }
    }

    suspend fun upsertUserAccount(
        fullName: String,
        organization: String,
        district: String,
        email: String,
        phone: String,
        role: UserRole,
        vehicleOrDietaryNote: String
    ): Result<UserAccountEntity> = runCatchingFirestore(OperationType.WRITE, COLLECTION_USERS) {
        val uid = requireUid()
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        val existing = docRef.get().await()

        val safeName = fullName.trim().ifBlank { "Demetra Member" }.take(120)
        val safeOrg = organization.trim().ifBlank { safeName }.take(140)
        val safeDistrict = district.trim().ifBlank { "Colombo" }.take(60)
        val safeEmail = email.trim().ifBlank { auth.currentUser?.email.orEmpty() }.take(160)
        val safePhone = phone.trim().take(32)
        val safeNote = vehicleOrDietaryNote.trim().take(200)

        if (existing.exists()) {
            docRef.update(
                mapOf(
                    "fullName" to safeName,
                    "organization" to safeOrg,
                    "district" to safeDistrict,
                    "email" to safeEmail,
                    "phone" to safePhone,
                    "activeRole" to role.name,
                    "vehicleOrDietaryNote" to safeNote,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            val entity = UserAccountEntity(
                uid = uid,
                fullName = safeName,
                organization = safeOrg,
                district = safeDistrict,
                email = safeEmail,
                phone = safePhone,
                activeRole = role.name,
                vehicleOrDietaryNote = safeNote
            )
            docRef.set(entity.toCreateMap()).await()
        }

        UserAccountEntity(
            id = uid,
            uid = uid,
            fullName = safeName,
            organization = safeOrg,
            district = safeDistrict,
            email = safeEmail,
            phone = safePhone,
            activeRole = role.name,
            vehicleOrDietaryNote = safeNote
        )
    }

    suspend fun updateUserDistrict(district: String): Result<Unit> =
        runCatchingFirestore(OperationType.UPDATE, COLLECTION_USERS) {
            val uid = requireUid()
            val safeDistrict = district.trim().take(60)
            require(safeDistrict.isNotBlank()) { "District must not be blank" }
            db.collection(COLLECTION_USERS).document(uid).update(
                mapOf(
                    "district" to safeDistrict,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    // =========================================================================
    // Donors (/donors/{donorId})
    // =========================================================================

    fun observeDonors(districtFilter: String? = null): Flow<RedistributionUiState<List<DonorEntity>>> {
        val baseQuery: Query = if (!districtFilter.isNullOrBlank()) {
            db.collection(COLLECTION_DONORS)
                .whereEqualTo("district", districtFilter.trim())
                .limit(DEFAULT_QUERY_LIMIT)
        } else {
            db.collection(COLLECTION_DONORS)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(DEFAULT_QUERY_LIMIT)
        }

        return baseQuery
            .snapshots(MetadataChanges.EXCLUDE)
            .map<_, RedistributionUiState<List<DonorEntity>>> { snapshot ->
                val donors = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        DonorEntity::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                RedistributionUiState.Success(donors)
            }
            .catch { error ->
                emitFirestoreStreamError(error, OperationType.LIST, COLLECTION_DONORS)
            }
    }

    suspend fun upsertDonorProfile(
        organizationName: String,
        donorType: String,
        district: String,
        locationName: String,
        typicalSurplus: String
    ): Result<DonorEntity> = runCatchingFirestore(OperationType.WRITE, COLLECTION_DONORS) {
        val uid = requireUid()
        val docRef = db.collection(COLLECTION_DONORS).document(uid)
        val existing = docRef.get().await()

        val safeOrg = organizationName.trim().ifBlank { "Community Donor" }.take(140)
        val safeType = donorType.trim().ifBlank { "Verified Food Donor" }.take(80)
        val safeDistrict = district.trim().ifBlank { "Colombo" }.take(60)
        val safeLoc = locationName.trim().ifBlank { safeDistrict }.take(160)
        val safeSurplus = typicalSurplus.trim().take(200)

        if (existing.exists()) {
            docRef.update(
                mapOf(
                    "organizationName" to safeOrg,
                    "donorType" to safeType,
                    "district" to safeDistrict,
                    "locationName" to safeLoc,
                    "typicalSurplus" to safeSurplus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            val entity = DonorEntity(
                userId = uid,
                organizationName = safeOrg,
                donorType = safeType,
                district = safeDistrict,
                locationName = safeLoc,
                typicalSurplus = safeSurplus,
                verified = true,
                totalHandovers = 0
            )
            docRef.set(entity.toCreateMap()).await()
        }

        DonorEntity(
            id = uid,
            userId = uid,
            organizationName = safeOrg,
            donorType = safeType,
            district = safeDistrict,
            locationName = safeLoc,
            typicalSurplus = safeSurplus
        )
    }

    suspend fun getDonorProfile(donorId: String): Result<DonorEntity?> =
        runCatchingFirestore(OperationType.GET, "$COLLECTION_DONORS/$donorId") {
            requireUid()
            require(isValidDocId(donorId)) { "Invalid donor ID" }
            db.collection(COLLECTION_DONORS)
                .document(donorId)
                .get()
                .await()
                .toObject(DonorEntity::class.java)
        }

    // =========================================================================
    // Volunteers (/volunteers/{volunteerId})
    // =========================================================================

    fun observeVolunteerProfile(uid: String): Flow<RedistributionUiState<VolunteerEntity?>> {
        if (!isValidDocId(uid)) {
            return kotlinx.coroutines.flow.flowOf(RedistributionUiState.Error("Invalid volunteer ID"))
        }
        val path = "$COLLECTION_VOLUNTEERS/$uid"
        return db.collection(COLLECTION_VOLUNTEERS)
            .document(uid)
            .snapshots(MetadataChanges.EXCLUDE)
            .map<DocumentSnapshot, RedistributionUiState<VolunteerEntity?>> { snapshot ->
                val entity = snapshot.toObject(
                    VolunteerEntity::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                RedistributionUiState.Success(entity)
            }
            .catch { error ->
                emitFirestoreStreamError(error, OperationType.GET, path)
            }
    }

    fun observeVolunteers(onlyAvailable: Boolean = false): Flow<RedistributionUiState<List<VolunteerEntity>>> {
        val baseQuery: Query = if (onlyAvailable) {
            db.collection(COLLECTION_VOLUNTEERS)
                .whereEqualTo("available", true)
                .limit(DEFAULT_QUERY_LIMIT)
        } else {
            db.collection(COLLECTION_VOLUNTEERS)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(DEFAULT_QUERY_LIMIT)
        }

        return baseQuery
            .snapshots(MetadataChanges.EXCLUDE)
            .map<_, RedistributionUiState<List<VolunteerEntity>>> { snapshot ->
                val volunteers = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        VolunteerEntity::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                RedistributionUiState.Success(volunteers)
            }
            .catch { error ->
                emitFirestoreStreamError(error, OperationType.LIST, COLLECTION_VOLUNTEERS)
            }
    }

    suspend fun upsertVolunteerProfile(
        displayName: String,
        communityGroup: String,
        district: String,
        preferredDistrict: String = district,
        interestAreas: List<String> = listOf(VolunteerInterestArea.HOT_MEAL_RESCUE.label),
        vehicleType: String,
        carrierCapacityKg: Double,
        availabilityWindow: String = "Flexible (Anytime)",
        available: Boolean = true
    ): Result<VolunteerEntity> = runCatchingFirestore(OperationType.WRITE, "$COLLECTION_VOLUNTEERS/${auth.currentUser?.uid ?: "unauthenticated"}") {
        val uid = requireUid()
        val docRef = db.collection(COLLECTION_VOLUNTEERS).document(uid)
        val existing = docRef.get().await()

        val safeName = displayName.trim().ifBlank { "Community Rider" }.take(120)
        val safeGroup = communityGroup.trim().take(140)
        val safePreferredDistrict = preferredDistrict.trim()
            .ifBlank { district.trim().ifBlank { "Colombo" } }
            .take(60)
        val safeDistrict = district.trim()
            .ifBlank { safePreferredDistrict }
            .take(60)
        val safeInterests = interestAreas
            .map { it.trim().take(60) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(8)
            .ifEmpty { listOf(VolunteerInterestArea.HOT_MEAL_RESCUE.label) }
        val safeVehicle = vehicleType.trim().ifBlank { "E-Bike (Insulated Box)" }.take(100)
        val safeCapacity = carrierCapacityKg.coerceIn(0.5, 500.0)
        val safeWindow = availabilityWindow.trim().ifBlank { "Flexible (Anytime)" }.take(80)

        if (existing.exists()) {
            docRef.update(
                mapOf(
                    "displayName" to safeName,
                    "communityGroup" to safeGroup,
                    "district" to safeDistrict,
                    "preferredDistrict" to safePreferredDistrict,
                    "interestAreas" to safeInterests,
                    "vehicleType" to safeVehicle,
                    "carrierCapacityKg" to safeCapacity,
                    "availabilityWindow" to safeWindow,
                    "available" to available,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            val entity = VolunteerEntity(
                userId = uid,
                displayName = safeName,
                communityGroup = safeGroup,
                district = safeDistrict,
                preferredDistrict = safePreferredDistrict,
                interestAreas = safeInterests,
                vehicleType = safeVehicle,
                carrierCapacityKg = safeCapacity,
                availabilityWindow = safeWindow,
                available = available,
                deliveriesCompleted = 0
            )
            docRef.set(entity.toCreateMap()).await()
        }

        VolunteerEntity(
            id = uid,
            userId = uid,
            displayName = safeName,
            communityGroup = safeGroup,
            district = safeDistrict,
            preferredDistrict = safePreferredDistrict,
            interestAreas = safeInterests,
            vehicleType = safeVehicle,
            carrierCapacityKg = safeCapacity,
            availabilityWindow = safeWindow,
            available = available
        )
    }

    suspend fun setVolunteerAvailability(available: Boolean): Result<Unit> =
        runCatchingFirestore(OperationType.UPDATE, COLLECTION_VOLUNTEERS) {
            val uid = requireUid()
            db.collection(COLLECTION_VOLUNTEERS).document(uid).update(
                mapOf(
                    "available" to available,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    // =========================================================================
    // Surplus Food Donations (/donations/{donationId})
    // =========================================================================

    fun observeDonations(districtFilter: String? = null): Flow<RedistributionUiState<List<FoodDonationEntity>>> {
        val baseQuery: Query = if (!districtFilter.isNullOrBlank()) {
            db.collection(COLLECTION_DONATIONS)
                .whereEqualTo("district", districtFilter.trim())
                .limit(DEFAULT_QUERY_LIMIT)
        } else {
            db.collection(COLLECTION_DONATIONS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(DEFAULT_QUERY_LIMIT)
        }

        return baseQuery
            .snapshots(MetadataChanges.EXCLUDE)
            .map<_, RedistributionUiState<List<FoodDonationEntity>>> { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        FoodDonationEntity::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                RedistributionUiState.Success(list)
            }
            .catch { error ->
                emitFirestoreStreamError(error, OperationType.LIST, COLLECTION_DONATIONS)
            }
    }

    suspend fun createFoodDonation(
        title: String,
        category: FoodCategory,
        quantityKg: Double,
        servingsEstimate: Int,
        donorName: String,
        donorType: String = "Verified Food Donor",
        locationName: String,
        district: String,
        pickupWindow: String,
        expiryNote: String,
        dietaryTags: List<String>,
        handoverPin: String
    ): Result<FoodDonationEntity> = runCatchingFirestore(OperationType.CREATE, COLLECTION_DONATIONS) {
        val uid = requireUid()
        val docRef = db.collection(COLLECTION_DONATIONS).document()
        val entity = FoodDonationEntity(
            id = docRef.id,
            donorUserId = uid,
            donorName = donorName.ifBlank { "Community Donor" },
            donorType = donorType,
            title = title.ifBlank { "Surplus Food Pack" },
            category = category.name,
            quantityKg = quantityKg.coerceIn(0.1, 5000.0),
            servingsEstimate = servingsEstimate.coerceIn(1, 50_000),
            locationName = locationName.ifBlank { district },
            district = district.ifBlank { "Colombo" },
            pickupWindow = pickupWindow.ifBlank { "Today" },
            expiryNote = expiryNote.ifBlank { "Consume promptly" },
            dietaryTags = dietaryTags.take(12),
            safetyConfirmed = true,
            status = DonationStatus.MATCHING.name,
            handoverPin = handoverPin.filter { it.isDigit() }.padStart(4, '0').take(4),
            pickupVerified = false,
            deliveryVerified = false
        )
        docRef.set(entity.toCreateMap()).await()
        entity
    }

    suspend fun acceptDonationAsVolunteer(
        donationId: String,
        volunteerName: String
    ): Result<Unit> = runCatchingFirestore(OperationType.UPDATE, "$COLLECTION_DONATIONS/$donationId") {
        val uid = requireUid()
        require(isValidDocId(donationId)) { "Invalid donation ID" }
        val safeVolunteerName = volunteerName.trim().ifBlank { "Community Rider" }.take(120)
        db.collection(COLLECTION_DONATIONS).document(donationId).update(
            mapOf(
                "status" to DonationStatus.VOLUNTEER_ASSIGNED.name,
                "assignedVolunteerUserId" to uid,
                "assignedVolunteerName" to safeVolunteerName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun markDonationPickedUp(donationId: String): Result<Unit> =
        runCatchingFirestore(OperationType.UPDATE, "$COLLECTION_DONATIONS/$donationId") {
            requireUid()
            require(isValidDocId(donationId)) { "Invalid donation ID" }
            db.collection(COLLECTION_DONATIONS).document(donationId).update(
                mapOf(
                    "status" to DonationStatus.PICKED_UP.name,
                    "pickupVerified" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    suspend fun markDonationDelivered(donationId: String): Result<Unit> =
        runCatchingFirestore(OperationType.UPDATE, "$COLLECTION_DONATIONS/$donationId") {
            requireUid()
            require(isValidDocId(donationId)) { "Invalid donation ID" }
            db.collection(COLLECTION_DONATIONS).document(donationId).update(
                mapOf(
                    "status" to DonationStatus.DELIVERED_VERIFIED.name,
                    "pickupVerified" to true,
                    "deliveryVerified" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    suspend fun deleteDonation(donationId: String): Result<Unit> =
        runCatchingFirestore(OperationType.DELETE, "$COLLECTION_DONATIONS/$donationId") {
            requireUid()
            require(isValidDocId(donationId)) { "Invalid donation ID" }
            db.collection(COLLECTION_DONATIONS).document(donationId).delete().await()
        }

    // =========================================================================
    // Recipient Food Requests (/requests/{requestId})
    // =========================================================================

    /**
     * Observes food redistribution requests where [uid] is recipient, donor, or assigned volunteer.
     * Matches Firestore rule: `allow list: if isSignedIn() && request.auth.uid in resource.data.participantIds;`
     */
    fun observeParticipantRequests(uid: String): Flow<RedistributionUiState<List<FoodRequestEntity>>> {
        if (!isValidDocId(uid)) {
            return kotlinx.coroutines.flow.flowOf(RedistributionUiState.Error("Invalid user ID"))
        }
        return db.collection(COLLECTION_REQUESTS)
            .whereArrayContains("participantIds", uid)
            .limit(DEFAULT_QUERY_LIMIT)
            .snapshots(MetadataChanges.EXCLUDE)
            .map<_, RedistributionUiState<List<FoodRequestEntity>>> { snapshot ->
                val list = snapshot.documents
                    .mapNotNull { it.toObject(FoodRequestEntity::class.java) }
                    .sortedByDescending {
                        it.getEstimatedCreatedMillis(snapshot.documents.firstOrNull { d -> d.id == it.id })
                    }
                RedistributionUiState.Success(list)
            }
            .catch { error ->
                emitFirestoreStreamError(error, OperationType.LIST, COLLECTION_REQUESTS)
            }
    }

    suspend fun createFoodRequest(
        donationId: String,
        donationTitle: String,
        donorUserId: String,
        recipientOrgName: String,
        district: String,
        requestedPortions: Int,
        dietaryNotes: String = ""
    ): Result<FoodRequestEntity> = runCatchingFirestore(OperationType.CREATE, COLLECTION_REQUESTS) {
        val uid = requireUid()
        require(isValidDocId(donationId)) { "Invalid donation ID" }
        require(isValidDocId(donorUserId)) { "Invalid donor user ID" }

        val docRef = db.collection(COLLECTION_REQUESTS).document()
        val entity = FoodRequestEntity(
            id = docRef.id,
            donationId = donationId,
            donationTitle = donationTitle.ifBlank { "Surplus Food Pack" },
            recipientUserId = uid,
            recipientOrgName = recipientOrgName.ifBlank { "Community Kitchen" },
            donorUserId = donorUserId,
            district = district.ifBlank { "Colombo" },
            requestedPortions = requestedPortions.coerceIn(1, 5000),
            dietaryNotes = dietaryNotes.trim().take(240),
            status = FoodRequestStatus.PENDING.name,
            participantIds = listOf(uid, donorUserId).distinct()
        )
        docRef.set(entity.toCreateMap()).await()
        entity
    }

    suspend fun assignVolunteerToRequest(
        requestId: String,
        volunteerUserId: String,
        volunteerName: String,
        existingParticipantIds: List<String>
    ): Result<Unit> = runCatchingFirestore(OperationType.UPDATE, "$COLLECTION_REQUESTS/$requestId") {
        requireUid()
        require(isValidDocId(requestId)) { "Invalid request ID" }
        require(isValidDocId(volunteerUserId)) { "Invalid volunteer user ID" }
        val updatedParticipants = (existingParticipantIds + volunteerUserId).distinct().take(3)
        db.collection(COLLECTION_REQUESTS).document(requestId).update(
            mapOf(
                "status" to FoodRequestStatus.APPROVED.name,
                "volunteerUserId" to volunteerUserId,
                "volunteerName" to volunteerName.trim().ifBlank { "Community Rider" }.take(120),
                "participantIds" to updatedParticipants,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun updateFoodRequestStatus(
        requestId: String,
        newStatus: FoodRequestStatus
    ): Result<Unit> = runCatchingFirestore(OperationType.UPDATE, "$COLLECTION_REQUESTS/$requestId") {
        requireUid()
        require(isValidDocId(requestId)) { "Invalid request ID" }
        db.collection(COLLECTION_REQUESTS).document(requestId).update(
            mapOf(
                "status" to newStatus.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun deleteFoodRequest(requestId: String): Result<Unit> =
        runCatchingFirestore(OperationType.DELETE, "$COLLECTION_REQUESTS/$requestId") {
            requireUid()
            require(isValidDocId(requestId)) { "Invalid request ID" }
            db.collection(COLLECTION_REQUESTS).document(requestId).delete().await()
        }

    // =========================================================================
    // Error & Cancellation Helpers
    // =========================================================================

    private suspend fun <T> runCatchingFirestore(
        operationType: OperationType,
        path: String,
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, operationType, path)
            Result.failure(e)
        }
    }

    private suspend fun <T> kotlinx.coroutines.flow.FlowCollector<RedistributionUiState<T>>.emitFirestoreStreamError(
        error: Throwable,
        operationType: OperationType,
        path: String
    ) {
        val firestoreException = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreException != null) {
            FirestoreErrorHandler.handleFirestoreError(firestoreException, operationType, path)
            emit(RedistributionUiState.Error(firestoreException.message ?: "Cloud sync unavailable"))
        } else if (error is CancellationException) {
            throw error
        } else {
            val wrapped = error as? Exception ?: Exception(error.message, error)
            FirestoreErrorHandler.handleFirestoreError(wrapped, operationType, path)
            emit(RedistributionUiState.Error(error.message ?: "Unexpected sync error"))
        }
    }

    private fun FoodRequestEntity.getEstimatedCreatedMillis(snapshot: DocumentSnapshot?): Long {
        val estimated = snapshot?.getTimestamp(
            "createdAt",
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
        return estimated?.toDate()?.time ?: createdAt?.toDate()?.time ?: 0L
    }
}
