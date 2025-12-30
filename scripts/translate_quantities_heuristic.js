// Heuristic translation of ingredient quantities to French and optional Firestore update.
// Usage:
//   node scripts/translate_quantities_heuristic.js <cocktails.json> <output.json> [--update <serviceAccount.json> <projectId>]
// Example (JSON only):
//   node scripts/translate_quantities_heuristic.js public/cocktails.json public/cocktails_quant_fr.json
// Example (JSON + Firestore update):
//   node scripts/translate_quantities_heuristic.js public/cocktails.json public/cocktails_quant_fr.json --update scripts/firebase-service-account.json cocktail-caa91

const fs = require('fs');
const path = require('path');
let admin = null;

function die(msg) { console.error(msg); process.exit(1); }

const args = process.argv.slice(2);
if (args.length < 2) die('Args: <input.json> <output.json> [--update <serviceAccount.json> <projectId>]');

const inputPath = args[0];
const outputPath = args[1];
let updateDb = false;
let saPath, projectId;
if (args[2] === '--update') {
  updateDb = true;
  saPath = args[3];
  projectId = args[4];
  if (!saPath || !projectId) die('With --update provide <serviceAccount.json> <projectId>');
}

if (!fs.existsSync(inputPath)) die(`File not found: ${inputPath}`);
if (updateDb && !fs.existsSync(saPath)) die(`Service account not found: ${saPath}`);

const data = JSON.parse(fs.readFileSync(inputPath, 'utf8'));
if (!Array.isArray(data)) die('Input JSON must be an array of cocktails');

const unitMap = new Map([
  ['dash', 'trait'],
  ['dashes', 'traits'],
  ['drop', 'goutte'],
  ['drops', 'gouttes'],
  ['barspoon', 'cuillère de bar'],
  ['barspoons', 'cuillères de bar'],
  ['teaspoon', 'cuillère à café'],
  ['teaspoons', 'cuillères à café'],
  ['tablespoon', 'cuillère à soupe'],
  ['tablespoons', 'cuillères à soupe'],
  ['cup', 'tasse'],
  ['cupful', 'tasse'],
  ['cups', 'tasses'],
  ['bottle', 'bouteille'],
  ['bottles', 'bouteilles'],
  ['scoop', 'boule'],
  ['scoops', 'boules'],
  ['sprig', 'brin'],
  ['sprigs', 'brins'],
  ['wedge', 'quartier'],
  ['wedges', 'quartiers'],
  ['slice', 'tranche'],
  ['slices', 'tranches'],
  ['segment', 'segment'],
  ['segments', 'segments'],
  ['peel', 'zeste'],
  ['twist', 'zeste'],
  ['swath', 'zeste'],
  ['knob', 'morceau'],
  ['ring', 'anneau'],
  ['pinch', 'pincée'],
  ['pinches', 'pincées'],
  ['grind', 'tournée de moulin'],
  ['grinds', 'tournées de moulin'],
  ['grain', 'grain'],
  ['segment', 'segment'],
  ['whole', 'entier'],
  ['unit', 'pièce'],
  ['pea', 'pois'],
  ['stick', 'bâton'],
  ['sticks', 'bâtons'],
  ['dropper', 'pipette'],
  ['cube', 'cube'],
  ['cubes', 'cubes'],
]);

function translateQuantity(q) {
  let s = (q || '').trim();
  if (!s) return s;

  // Preserve numbers and ml/oz/cl/l
  // Attempt to split leading number/quantity token from a trailing unit word.
  const m = s.match(/^([0-9]+(?:[.,][0-9]+)?(?:\s*[+/]\s*[0-9]+)?)\s*(.*)$/);
  if (m) {
    const qty = m[1].replace(',', '.');
    const rest = m[2].trim();
    if (!rest) return qty;
    const lower = rest.toLowerCase();
    // If it already contains ml, cl, oz, dl, l, keep as-is
    if (/(ml|cl|dl|l|oz|brix|%)/i.test(rest)) {
      return `${qty} ${rest}`.trim();
    }
    const mapped = unitMap.get(lower) || unitMap.get(lower.replace(/s$/, ''));
    if (mapped) return `${qty} ${mapped}`.trim();
    return `${qty} ${rest}`.trim();
  }

  // Single word unit
  const lower = s.toLowerCase();
  const mapped = unitMap.get(lower) || unitMap.get(lower.replace(/s$/, ''));
  if (mapped) return mapped;

  // Leave untouched if no rule
  return s;
}

// Translate JSON
const translated = data.map(c => {
  if (!Array.isArray(c.ingredients)) return c;
  const newIngs = c.ingredients.map(ing => {
    const translatedQty = translateQuantity(ing.quantity);
    return { ...ing, quantity: translatedQty };
  });
  return { ...c, ingredients: newIngs };
});

fs.writeFileSync(outputPath, JSON.stringify(translated, null, 2), 'utf8');
console.log(`Wrote translated quantities JSON to ${outputPath}`);

if (updateDb) {
  admin = require('firebase-admin');
  admin.initializeApp({
    credential: admin.credential.cert(require(path.resolve(saPath))),
    projectId,
  });
  const db = admin.firestore();

  (async () => {
    let touched = 0;
    for (const c of translated) {
      const id = c.id || c.documentId || c.cocktailId;
      if (!id) continue;
      await db.collection('cocktails').doc(String(id)).set({ ingredients: c.ingredients }, { merge: true });
      touched++;
      if (touched % 50 === 0) console.log(`Updated ${touched} docs...`);
    }
    console.log(`Firestore updated: ${touched} documents.`);
    process.exit(0);
  })().catch(err => { console.error('Error updating Firestore:', err); process.exit(1); });
}
