const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DONOR_UID = "donor_1";
const VOLUNTEER_UID = "vol_1";
const RECIPIENT_UID = "rec_1";
const OTHER_UID = "other_1";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 10_000) {
  return new Date(Date.now() - msOffset);
}

function futureDate(msOffset = 600_000) {
  return new Date(Date.now() + msOffset);
}

function validUser(uid, ts = pastDate()) {
  return {
    uid,
    fullName: "Amaya Wickramasinghe",
    organization: "Upali's Kitchen",
    district: "Colombo",
    email: `${uid}@example.lk`,
    phone: "+94 77 412 8930",
    activeRole: "DONOR",
    vehicleOrDietaryNote: "Restaurant & Catering Surplus",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validDonor(uid, ts = pastDate()) {
  return {
    userId: uid,
    organizationName: "Upali's Kitchen",
    donorType: "Restaurant Partner",
    district: "Colombo",
    locationName: "65 Ward Place, Cinnamon Gardens",
    typicalSurplus: "Cooked Meals & Short Eats",
    verified: true,
    totalHandovers: 4,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validVolunteer(uid, ts = pastDate()) {
  return {
    userId: uid,
    displayName: "Nadeesha Fernando",
    communityGroup: "Colombo Community Riders",
    district: "Colombo",
    preferredDistrict: "Kandy",
    interestAreas: ["Hot Meal Rescue", "Urgent Emergency Dispatch"],
    vehicleType: "E-Bike (Insulated Box)",
    carrierCapacityKg: 15.0,
    availabilityWindow: "Flexible (Anytime)",
    available: true,
    deliveriesCompleted: 6,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validDonation(donorUid, ts = pastDate()) {
  return {
    donorUserId: donorUid,
    donorName: "Upali's Kitchen",
    donorType: "Restaurant Partner",
    title: "Turmeric Rice & Vegetable Curry Packs",
    category: "COOKED_MEALS",
    quantityKg: 9.6,
    servingsEstimate: 24,
    locationName: "65 Ward Place, Cinnamon Gardens",
    district: "Colombo",
    pickupWindow: "Today, 7:00 PM - 8:30 PM",
    expiryNote: "Best before 10:30 PM",
    dietaryTags: ["Vegetarian", "Halal-friendly"],
    safetyConfirmed: true,
    status: "MATCHING",
    handoverPin: "4829",
    pickupVerified: false,
    deliveryVerified: false,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validRequest(donorUid, recipientUid, donationId = "don-1", ts = pastDate()) {
  return {
    donationId,
    donationTitle: "Turmeric Rice & Vegetable Curry Packs",
    recipientUserId: recipientUid,
    recipientOrgName: "Sarana Community Kitchen",
    donorUserId: donorUid,
    district: "Colombo",
    requestedPortions: 12,
    dietaryNotes: "Vegetarian packs preferred",
    status: "PENDING",
    participantIds: [recipientUid, donorUid],
    createdAt: ts,
    updatedAt: ts,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated requests are denied across all collections", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("users").doc(DONOR_UID).get());
  await assertFails(db.collection("donors").doc(DONOR_UID).get());
  await assertFails(db.collection("volunteers").doc(VOLUNTEER_UID).get());
  await assertFails(db.collection("donations").doc("don-1").get());
  await assertFails(db.collection("requests").doc("req-1").get());
  await assertFails(db.collection("donors").doc(DONOR_UID).set(validDonor(DONOR_UID)));
});

test("Private /users profile enforces owner isolation, strict schema, and timestamp immutability", async () => {
  const donorDb = testEnv.authenticatedContext(DONOR_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const created = pastDate(20_000);

  await assertSucceeds(donorDb.collection("users").doc(DONOR_UID).set(validUser(DONOR_UID, created)));
  await assertSucceeds(donorDb.collection("users").doc(DONOR_UID).get());

  // Cross-user access & collection listing blocked
  await assertFails(otherDb.collection("users").doc(DONOR_UID).get());
  await assertFails(otherDb.collection("users").limit(10).get());
  await assertFails(otherDb.collection("users").doc(DONOR_UID).set(validUser(DONOR_UID, created)));

  // Reject extra keys & future timestamps
  await assertFails(
    donorDb.collection("users").doc("extra_user").set({
      ...validUser("extra_user", created),
      isAdmin: true,
    })
  );
  await assertFails(
    donorDb.collection("users").doc(DONOR_UID).update({
      createdAt: futureDate(),
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    donorDb.collection("users").doc(DONOR_UID).update({
      createdAt: pastDate(5000),
      updatedAt: pastDate(1000),
    })
  );
});

test("/donors and /volunteers allow owner CRUD, bounded signed-in listing, and reject non-owner writes", async () => {
  const donorDb = testEnv.authenticatedContext(DONOR_UID).firestore();
  const volunteerDb = testEnv.authenticatedContext(VOLUNTEER_UID).firestore();

  await assertSucceeds(donorDb.collection("donors").doc(DONOR_UID).set(validDonor(DONOR_UID)));
  await assertSucceeds(
    donorDb.collection("donors").doc(DONOR_UID).update({
      totalHandovers: 5,
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(volunteerDb.collection("donors").limit(50).get());
  await assertFails(
    volunteerDb.collection("donors").doc(DONOR_UID).update({
      totalHandovers: 99,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(volunteerDb.collection("donors").doc(DONOR_UID).delete());

  await assertSucceeds(
    volunteerDb.collection("volunteers").doc(VOLUNTEER_UID).set(validVolunteer(VOLUNTEER_UID))
  );
  await assertSucceeds(
    volunteerDb.collection("volunteers").doc(VOLUNTEER_UID).update({
      preferredDistrict: "Galle",
      district: "Galle",
      interestAreas: ["Bakery & Evening Runs", "Community & Weekend Drives"],
      available: false,
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(donorDb.collection("volunteers").limit(50).get());
  await assertFails(
    donorDb.collection("volunteers").doc(VOLUNTEER_UID).update({
      available: true,
      updatedAt: pastDate(1000),
    })
  );
  // Empty interestAreas or missing preferredDistrict must fail
  await assertFails(
    volunteerDb.collection("volunteers").doc("bad_vol").set({
      ...validVolunteer("bad_vol"),
      interestAreas: [],
    })
  );
  await assertFails(
    volunteerDb.collection("volunteers").doc("bad_vol_2").set({
      ...validVolunteer("bad_vol_2"),
      preferredDistrict: "",
    })
  );

  // @DocumentId `id` must never appear in written fields
  await assertFails(
    donorDb.collection("donors").doc("donor_with_id").set({
      id: "donor_with_id",
      ...validDonor("donor_with_id"),
    })
  );
});

test("/donations enforces donor ownership and valid volunteer/recipient relay transitions", async () => {
  const donorDb = testEnv.authenticatedContext(DONOR_UID).firestore();
  const volunteerDb = testEnv.authenticatedContext(VOLUNTEER_UID).firestore();
  const recipientDb = testEnv.authenticatedContext(RECIPIENT_UID).firestore();
  const intruderDb = testEnv.authenticatedContext(OTHER_UID).firestore();

  await assertSucceeds(donorDb.collection("donations").doc("don-1").set(validDonation(DONOR_UID)));
  await assertSucceeds(volunteerDb.collection("donations").limit(50).get());

  // Intruder cannot mutate donor fields
  await assertFails(
    intruderDb.collection("donations").doc("don-1").update({
      title: "Hijacked Title",
      updatedAt: pastDate(1000),
    })
  );

  // Volunteer claims matching donation
  await assertSucceeds(
    volunteerDb.collection("donations").doc("don-1").update({
      status: "VOLUNTEER_ASSIGNED",
      assignedVolunteerUserId: VOLUNTEER_UID,
      assignedVolunteerName: "Nadeesha Fernando",
      updatedAt: pastDate(1000),
    })
  );

  // Recipient claims open recipient slot
  await assertSucceeds(
    recipientDb.collection("donations").doc("don-1").update({
      recipientUserId: RECIPIENT_UID,
      recipientOrgName: "Sarana Community Kitchen",
      updatedAt: pastDate(1000),
    })
  );

  // Intruder cannot verify pickup
  await assertFails(
    intruderDb.collection("donations").doc("don-1").update({
      status: "PICKED_UP",
      pickupVerified: true,
      updatedAt: pastDate(1000),
    })
  );

  // Assigned volunteer progresses PICKED_UP -> DELIVERED_VERIFIED
  await assertSucceeds(
    volunteerDb.collection("donations").doc("don-1").update({
      status: "PICKED_UP",
      pickupVerified: true,
      updatedAt: pastDate(900),
    })
  );
  await assertSucceeds(
    volunteerDb.collection("donations").doc("don-1").update({
      status: "DELIVERED_VERIFIED",
      pickupVerified: true,
      deliveryVerified: true,
      updatedAt: pastDate(800),
    })
  );
});

test("/requests enforces participant isolation, filtered queries, and role-scoped updates", async () => {
  const recipientDb = testEnv.authenticatedContext(RECIPIENT_UID).firestore();
  const donorDb = testEnv.authenticatedContext(DONOR_UID).firestore();
  const volunteerDb = testEnv.authenticatedContext(VOLUNTEER_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(OTHER_UID).firestore();

  await assertSucceeds(
    recipientDb.collection("requests").doc("req-1").set(validRequest(DONOR_UID, RECIPIENT_UID, "don-1"))
  );

  // Participant-scoped query matching Kotlin repository succeeds
  await assertSucceeds(
    donorDb
      .collection("requests")
      .where("participantIds", "array-contains", DONOR_UID)
      .limit(50)
      .get()
  );

  // Stranger cannot read doc or execute unfiltered query
  await assertFails(strangerDb.collection("requests").doc("req-1").get());
  await assertFails(strangerDb.collection("requests").limit(20).get());

  // Donor approves and adds volunteer to participantIds
  await assertSucceeds(
    donorDb.collection("requests").doc("req-1").update({
      status: "APPROVED",
      volunteerUserId: VOLUNTEER_UID,
      volunteerName: "Nadeesha Fernando",
      participantIds: [RECIPIENT_UID, DONOR_UID, VOLUNTEER_UID],
      updatedAt: pastDate(1000),
    })
  );

  // Assigned volunteer updates IN_TRANSIT, recipient confirms FULFILLED
  await assertSucceeds(volunteerDb.collection("requests").doc("req-1").get());
  await assertSucceeds(
    volunteerDb.collection("requests").doc("req-1").update({
      status: "IN_TRANSIT",
      updatedAt: pastDate(900),
    })
  );
  await assertSucceeds(
    recipientDb.collection("requests").doc("req-1").update({
      status: "FULFILLED",
      updatedAt: pastDate(800),
    })
  );

  // Donor cannot delete recipient request; recipient can delete own request
  await assertFails(donorDb.collection("requests").doc("req-1").delete());
  await assertSucceeds(recipientDb.collection("requests").doc("req-1").delete());
});
