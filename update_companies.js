const { initializeApp, cert } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const serviceAccount = require("./serviceAccountKey.json");

initializeApp({ credential: cert(serviceAccount) });
const db = getFirestore();

const companiesData = [
  { name: "Google", logoUrl: "https://t0.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://www.google.com&size=128", isActive: true },
  { name: "Microsoft", logoUrl: "https://t0.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://www.microsoft.com&size=128", isActive: true },
  { name: "Amazon", logoUrl: "https://t0.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://www.amazon.com&size=128", isActive: true },
  { name: "Meta", logoUrl: "https://t0.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://www.meta.com&size=128", isActive: true },
  { name: "Apple", logoUrl: "https://t0.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://www.apple.com&size=128", isActive: true },
  { name: "Infosys", logoUrl: "https://t0.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://www.infosys.com&size=128", isActive: true },
  { name: "TCS", logoUrl: "https://t0.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://www.tcs.com&size=128", isActive: true },
  { name: "Wipro", logoUrl: "https://t0.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://www.wipro.com&size=128", isActive: true },
  { name: "Accenture", logoUrl: "https://t0.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://www.accenture.com&size=128", isActive: true },
  { name: "Cognizant", logoUrl: "https://t0.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://www.cognizant.com&size=128", isActive: true },
  { name: "Flipkart", logoUrl: "https://t0.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://www.flipkart.com&size=128", isActive: true },
  { name: "Paytm", logoUrl: "https://t0.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://www.paytm.com&size=128", isActive: true }
];

async function updateCompanies() {
  const collectionRef = db.collection("companies");
  const existingDocs = await collectionRef.get();
  
  // If collection exists, we update. Otherwise, we add.
  const batch = db.batch();
  
  companiesData.forEach(company => {
    let docToUpdate = null;
    existingDocs.forEach(doc => {
      if (doc.data().name === company.name) {
        docToUpdate = doc;
      }
    });

    if (docToUpdate) {
      batch.update(docToUpdate.ref, { logoUrl: company.logoUrl, isActive: company.isActive });
    } else {
      const newDocRef = collectionRef.doc();
      batch.set(newDocRef, company);
    }
  });

  await batch.commit();
  console.log("Companies updated in Firebase with logoUrl and isActive status.");
  process.exit(0);
}

updateCompanies().catch(console.error);
