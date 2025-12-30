// Usage:
// node scripts/bulk_update_ingredients_from_csv.js <cocktails.json> <serviceAccount.json> <projectId> <csv1> [csv2 ...]
// Example:
// node scripts/bulk_update_ingredients_from_csv.js public/cocktails.json scripts/firebase-service-account.json cocktail-caa91 \
//   scripts/ingredient_translation_1_100.csv scripts/ingredient_translation_101_200.csv ... scripts/ingredient_translation_601_658.csv

const fs = require('fs');
const path = require('path');
const admin = require('firebase-admin');

function die(msg) {
  console.error(msg);
  process.exit(1);
}

const args = process.argv.slice(2);
if (args.length < 4) {
  die('Args: <cocktails.json> <serviceAccount.json> <projectId> <csv1> [csv2 ...]');
}

const [jsonPath, saPath, projectId, ...csvFiles] = args;
if (!fs.existsSync(jsonPath)) die(`File not found: ${jsonPath}`);
if (!fs.existsSync(saPath)) die(`Service account not found: ${saPath}`);
if (csvFiles.length === 0) die('Provide at least one CSV mapping file');

const cocktails = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
if (!Array.isArray(cocktails)) die('cocktails.json must be an array');

// Parse CSV mapping files: expected header "Original;French_normalise"
const mapping = new Map();
for (const file of csvFiles) {
  if (!fs.existsSync(file)) die(`CSV not found: ${file}`);
  const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/);
  for (const line of lines) {
    if (!line.trim()) continue;
    if (line.toLowerCase().startsWith('original;')) continue;
    const parts = line.split(';');
    if (parts.length < 2) continue;
    const orig = (parts[0] || '').trim();
    const norm = (parts[1] || '').trim();
    if (!orig || !norm) continue;
    mapping.set(orig.toLowerCase(), norm);
  }
}

console.log(`Loaded ${mapping.size} ingredient mappings from ${csvFiles.length} CSV file(s).`);

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
    const newIngredients = c.ingredients.map((ing) => {
      const name = (ing.name || '').trim();
      if (!name) return ing;
      const key = name.toLowerCase();
      if (mapping.has(key)) {
        changed = true;
        replacements += 1;
        return { ...ing, name: mapping.get(key) };
      }
      return ing;
    });

    if (!changed) continue;
    cocktailsTouched += 1;
    const id = c.id || c.documentId || c.cocktailId;
    if (!id) {
      console.warn('Skip cocktail without id:', c.name || 'unknown');
      continue;
    }
    const data = { ...c, ingredients: newIngredients };
    await db.collection('cocktails').doc(String(id)).set(data, { merge: true });
    console.log(`Updated ${id} (${c.name || ''})`);
  }

  console.log(`Done. Cocktails touched: ${cocktailsTouched}, ingredient name replacements: ${replacements}`);
  process.exit(0);
})().catch((err) => {
  console.error('Error:', err);
  process.exit(1);
});
