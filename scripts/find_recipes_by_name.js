const fs = require('fs');
const path = require('path');

// Liste des noms à rechercher (casse ignorée)
const TARGET_NAMES = [
  "Without Borders",
  "Million Dollar Margarita",
  "Pisco Sour (Difford's recipe)",
  "Chrysanthemum",
  "Honolulu Cocktail No. 2",
  "Very Rusty Daiquiri",
  "Cameron's Kick",
  "Santa Maria Daiquiri",
  "Eight Foot Fizz",
  "Yellow Submarine",
  "Mercado Roma",
  "Friend of Jack",
  "The Usual",
  "Jack Rising",
  "Clever Jasper's Tea",
  "The Sunday Session",
  "Heather Crowe",
  "Soirée Cup",
  "Mr Masso",
  "Ron de Olla",
  "Playa Fortuna",
  "Apple-Jacked",
  "Leave A Message After The Beep...",
  "Equilibrium",
  "Golden Eight",
  "Superhero",
  "Skyline Margarita",
  "8verlast",
  "Dyevitchka",
  "French Daiquiri"
].map(s => s.toLowerCase());

const recipesRoot = path.join(__dirname, '..', 'recipes');
const outFile = path.join(__dirname, 'found_recipes.txt');

const matches = [];
for (const dirent of fs.readdirSync(recipesRoot, { withFileTypes: true })) {
  if (!dirent.isDirectory()) continue;
  const id = dirent.name;
  const contentPath = path.join(recipesRoot, id, 'content.txt');
  if (!fs.existsSync(contentPath)) continue;
  const text = fs.readFileSync(contentPath, 'utf8');
  const line = text.split(/\r?\n/).find(l => l.startsWith('Nom:'));
  const name = line ? line.replace(/^Nom:\s*/, '').trim() : '';
  if (name && TARGET_NAMES.includes(name.toLowerCase())) {
    matches.push({ id, name });
  }
}

const output = matches.map(m => `${m.id} | ${m.name}`).join('\n');
fs.writeFileSync(outFile, output, 'utf8');
console.log(`Trouvé ${matches.length} recettes. Écrit dans ${outFile}`);
