/**
 * InterviewTrail - Firestore Seed Import Script
 * ------------------------------------------------
 * Imports all master data collections from firestore_seed_data.json
 * into your Firebase project's Firestore database.
 *
 * Compatible with firebase-admin v10+ through v14+.
 */

const { initializeApp, cert } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const seedData = require("./firestore_seed_data.json");
const serviceAccount = require("./serviceAccountKey.json");

// Initialize Firebase Admin with credentials
initializeApp({
  credential: cert(serviceAccount),
});

const db = getFirestore();

async function run() {
  console.log("Starting InterviewTrail Firestore seed import...\n");

  // Track generated doc IDs so State can reference Country,
  // and City can reference State.
  const countryIdByName = {};
  const stateIdByName = {};

  // --- 1. Simple collections: insert as-is ---
  const simpleCollections = [
    "TechnologyStack",
    "Domain",
    "PostType",
    "ExperienceLevel",
    "Designation",
    "Gender",
    "InterviewRoundType",
    "JobType",
    "NotificationCategory",
  ];

  for (const collectionName of simpleCollections) {
    const records = seedData[collectionName] || [];
    const batch = db.batch();
    records.forEach((record) => {
      const docRef = db.collection(collectionName).doc();
      batch.set(docRef, {
        ...record,
        created_at: FieldValue.serverTimestamp(),
      });
    });
    await batch.commit();
    console.log(`Inserted ${records.length} documents into ${collectionName}`);
  }

  // --- 2. Country (needed before State) ---
  for (const record of seedData.Country) {
    const docRef = await db.collection("Country").add({
      ...record,
      created_at: FieldValue.serverTimestamp(),
    });
    countryIdByName[record.name] = docRef.id;
  }
  console.log(`Inserted ${seedData.Country.length} documents into Country`);

  // --- 3. State (references Country) ---
  for (const record of seedData.State) {
    const countryId = countryIdByName[record.country_name];
    const docRef = await db.collection("State").add({
      name: record.name,
      country_id: countryId,
      created_at: FieldValue.serverTimestamp(),
    });
    stateIdByName[record.name] = docRef.id;
  }
  console.log(`Inserted ${seedData.State.length} documents into State`);

  // --- 4. City (references State) ---
  const cityBatch = db.batch();
  seedData.City.forEach((record) => {
    const stateId = stateIdByName[record.state_name];
    const docRef = db.collection("City").doc();
    cityBatch.set(docRef, {
      name: record.name,
      state_id: stateId,
      created_at: FieldValue.serverTimestamp(),
    });
  });
  await cityBatch.commit();
  console.log(`Inserted ${seedData.City.length} documents into City`);

  console.log("\nAll master data imported successfully.");
  process.exit(0);
}

run().catch((err) => {
  console.error("Import failed:", err);
  process.exit(1);
});
