// Usage: node scripts/export_unique_quantities.js <cocktails.json> <output.txt>
// Ex:    node scripts/export_unique_quantities.js public/cocktails.json scripts/all_quantities.txt
const fs = require('fs');

const [jsonPath, outPath] = process.argv.slice(2);
if (!jsonPath || !outPath) {
  console.error('Args: <cocktails.json> <output.txt>');
  process.exit(1);
}
if (!fs.existsSync(jsonPath)) {
  console.error('File not found:', jsonPath);
  process.exit(1);
}
const data = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
if (!Array.isArray(data)) {
  console.error('cocktails.json must be an array');
  process.exit(1);
}
const set = new Set();
for (const c of data) {
  if (!Array.isArray(c.ingredients)) continue;
  for (const ing of c.ingredients) {
    const q = (ing.quantity || '').trim();
    if (q) set.add(q);
  }
}
const list = Array.from(set).sort((a,b)=>a.localeCompare(b));
fs.writeFileSync(outPath, list.join('\n'), 'utf8');
console.log(`Wrote ${list.length} unique quantities to ${outPath}`);
