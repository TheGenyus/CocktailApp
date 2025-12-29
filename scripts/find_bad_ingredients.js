// scripts/find_bad_ingredients.js
const admin = require('firebase-admin');

const BAD_ING = new Set([
  "special ingredient #2 (see above)",
  "special ingredient #4 (see above)",
  "1 dash","1 grind","1 pinch","1 swath","1.25 ml","10 ml","105 ml","12 dried","12.5 ml","14 fresh",
  "2","2 dash","2 grind","2 sprig","2.5 ml","22.5 ml","3","3 dash","3 fresh","30 ml","4","4 drop",
  "4 grind","450 ml","5 fresh","5 sprig","6 drop","60 ml","7.5 ml","8 drop","90 ml","barspoon","chilled",
  "chopped wedges","cupful","dash","fill glasses with","from freezer","grated","infused with earl grey tea",
  "optional","pinch","ring","slice","thai","/"
].map(s => s.toLowerCase()));

if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  console.error('Set GOOGLE_APPLICATION_CREDENTIALS to your service account JSON.');
  process.exit(1);
}

admin.initializeApp({credential: admin.credential.applicationDefault()});
const db = admin.firestore();

(async () => {
  const snap = await db.collection('cocktails').get();
  snap.forEach(doc => {
    const data = doc.data() || {};
    const ings = (data.ingredients || []).map(i => (i.name || '').toLowerCase());
    const hasBad = ings.some(n => BAD_ING.has(n.trim()));
    if (hasBad) {
      console.log(`${doc.id} | ${data.name || '(sans nom)'}`);
    }
  });
  process.exit(0);
})();
