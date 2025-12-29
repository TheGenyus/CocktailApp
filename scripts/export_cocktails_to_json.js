const fs = require('fs');
const admin = require('firebase-admin');

// Chemins
const outPath = process.argv[2] || 'cocktails.json';
const serviceAccount = require('./firebase-service-account.json');

admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = admin.firestore();

(async () => {
  const snap = await db.collection('cocktails').get();
  const data = [];
  snap.forEach(doc => {
    const d = doc.data();
    data.push({
      id: doc.id,
      name: d.name,
      image: d.imageUrl || d.image,
      ingredients: d.ingredients || [],
      instructions: d.recipe || d.instructions,
      profile: d.profile || { strength: d.strengthScore, sweetness: d.tasteScore },
      expertRating: d.expertRating,
      memberRating: d.memberRating,
      history: d.history,
      nutrition: d.nutrition,
      alcohol: d.alcoholContent,
      review: d.review,
    });
  });
  fs.writeFileSync(outPath, JSON.stringify(data, null, 2), 'utf8');
  console.log(`Exporté ${data.length} cocktails -> ${outPath}`);
  process.exit(0);
})();
