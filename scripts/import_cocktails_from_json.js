// Import cocktails from a JSON array and overwrite (set merge:false) each document in Firestore.
// Usage:
//   node scripts/import_cocktails_from_json.js <cocktails.json> <serviceAccount.json> <projectId>
// Example:
//   node scripts/import_cocktails_from_json.js public/cocktails.json scripts/firebase-service-account.json cocktail-caa91

const fs = require('fs');
const path = require('path');
const admin = require('firebase-admin');

function die(msg) { console.error(msg); process.exit(1); }

const [jsonPath, saPath, projectId] = process.argv.slice(2);
if (!jsonPath || !saPath || !projectId) {
  die('Args: <cocktails.json> <serviceAccount.json> <projectId>');
}
if (!fs.existsSync(jsonPath)) die(`File not found: ${jsonPath}`);
if (!fs.existsSync(saPath)) die(`Service account not found: ${saPath}`);

const cocktails = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
if (!Array.isArray(cocktails)) die('Input JSON must be an array');

admin.initializeApp({
  credential: admin.credential.cert(require(path.resolve(saPath))),
  projectId,
});
const db = admin.firestore();

(async () => {
  let count = 0;
  for (const c of cocktails) {
    const id = c.id || c.documentId || c.cocktailId;
    if (!id) { console.warn('Skip cocktail without id:', c.name || ''); continue; }
    const data = { ...c };
    await db.collection('cocktails').doc(String(id)).set(data, { merge: false });
    count++;
    if (count % 50 === 0) console.log(`Imported ${count} cocktails...`);
  }
  console.log(`Done. Imported ${count} cocktails.`);
  process.exit(0);
})().catch(err => { console.error('Error:', err); process.exit(1); });
