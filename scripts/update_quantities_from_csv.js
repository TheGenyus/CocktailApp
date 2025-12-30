// Usage:
// node scripts/update_quantities_from_csv.js <cocktails.json> <serviceAccount.json> <projectId> <csv>
// CSV format: header "Original;French_normalise" then rows quantity_en;quantity_fr
// Example:
// node scripts/update_quantities_from_csv.js public/cocktails.json scripts/firebase-service-account.json cocktail-caa91 scripts/quantity_translation.csv

const fs = require('fs');
const path = require('path');
const admin = require('firebase-admin');

function die(msg) { console.error(msg); process.exit(1); }

const [jsonPath, saPath, projectId, csvPath] = process.argv.slice(2);
if (!jsonPath || !saPath || !projectId || !csvPath) die('Args: <cocktails.json> <serviceAccount.json> <projectId> <csv>');
if (!fs.existsSync(jsonPath)) die(`File not found: ${jsonPath}`);
if (!fs.existsSync(saPath)) die(`Service account not found: ${saPath}`);
if (!fs.existsSync(csvPath)) die(`CSV not found: ${csvPath}`);

const cocktails = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
if (!Array.isArray(cocktails)) die('cocktails.json must be an array');

const mapping = new Map();
fs.readFileSync(csvPath, 'utf8').split(/\r?\n/).forEach(line => {
  if (!line.trim()) return;
  if (line.toLowerCase().startsWith('original;')) return;
  const [orig, fr] = line.split(';');
  if (!orig || !fr) return;
  mapping.set(orig.trim(), fr.trim());
});
console.log(`Loaded ${mapping.size} quantity mappings.`);

admin.initializeApp({
  credential: admin.credential.cert(require(path.resolve(saPath))),
  projectId,
});
const db = admin.firestore();

(async () => {
  let cocktailsTouched = 0;
  let replacements = 0;

  for (const c of cocktails) {
    if (!Array.isArray(c.ingredients) || c.ingredients.length === 0) continue;
    let changed = false;
    const newIngredients = c.ingredients.map(ing => {
      const q = (ing.quantity || '').trim();
      if (mapping.has(q)) {
        changed = true;
        replacements++;
        return { ...ing, quantity: mapping.get(q) };
      }
      return ing;
    });

    if (!changed) continue;
    cocktailsTouched++;
    const id = c.id || c.documentId || c.cocktailId;
    if (!id) { console.warn('Skip cocktail without id:', c.name || ''); continue; }
    const data = { ...c, ingredients: newIngredients };
    await db.collection('cocktails').doc(String(id)).set(data, { merge: true });
    console.log(`Updated ${id} (${c.name || ''})`);
  }
  console.log(`Done. Cocktails touched: ${cocktailsTouched}, quantity replacements: ${replacements}`);
  process.exit(0);
})().catch(err => { console.error('Error:', err); process.exit(1); });
