// Usage:
// node scripts/update_ingredient_name.js <cocktails.json> <serviceAccount.json> <projectId> <oldName> <newName>
// Example:
// node scripts/update_ingredient_name.js public/cocktails.json scripts/firebase-service-account.json cocktail-caa91 "Pineapple (fresh)" "Pineapple"

const fs = require('fs');
const path = require('path');
const admin = require('firebase-admin');

function die(msg) {
  console.error(msg);
  process.exit(1);
}

const [jsonPath, saPath, projectId, oldName, newName] = process.argv.slice(2);
if (!jsonPath || !saPath || !projectId || !oldName || !newName) {
  die('Args: <cocktails.json> <serviceAccount.json> <projectId> <oldName> <newName>');
}

if (!fs.existsSync(jsonPath)) die(`File not found: ${jsonPath}`);
if (!fs.existsSync(saPath)) die(`Service account not found: ${saPath}`);

const cocktails = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
if (!Array.isArray(cocktails)) die('cocktails.json must be an array');

admin.initializeApp({
  credential: admin.credential.cert(require(path.resolve(saPath))),
  projectId,
});
const db = admin.firestore();

const target = oldName.trim().toLowerCase();
const replacement = newName;

const hits = cocktails.filter(
  (c) => Array.isArray(c.ingredients) && c.ingredients.some((ing) => (ing.name || '').trim().toLowerCase() === target)
);
console.log(`Found ${hits.length} cocktails with ingredient "${oldName}"`);

(async () => {
  let updated = 0;
  for (const c of hits) {
    const newIngredients = c.ingredients.map((ing) => {
      const name = (ing.name || '').trim();
      if (name.toLowerCase() === target) {
        return { ...ing, name: replacement };
      }
      return ing;
    });

    const data = { ...c, ingredients: newIngredients };
    const id = c.id || c.documentId || c.cocktailId;
    if (!id) {
      console.warn('Skip cocktail without id:', c.name || 'unknown');
      continue;
    }
    await db.collection('cocktails').doc(String(id)).set(data, { merge: true });
    updated++;
    console.log(`Updated ${id} (${c.name || ''})`);
  }
  console.log(`Done. Updated ${updated} documents.`);
  process.exit(0);
})();
