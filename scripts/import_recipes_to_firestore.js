const fs = require('fs');
const path = require('path');
const admin = require('firebase-admin');

// Usage: node scripts/import_recipes_to_firestore.js <recipesFolder> <serviceAccountPath> [projectId]
const recipesRoot = process.argv[2] || path.join(process.cwd(), 'recipes');
const serviceAccountPath = process.argv[3] || process.env.GOOGLE_APPLICATION_CREDENTIALS;
const projectId = process.argv[4];

if (!serviceAccountPath || !fs.existsSync(serviceAccountPath)) {
  console.error('Missing service account path. Pass as arg or set GOOGLE_APPLICATION_CREDENTIALS.');
  process.exit(1);
}

const serviceAccount = require(serviceAccountPath);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: projectId || serviceAccount.project_id,
});
const db = admin.firestore();

function normalizeLabel(str) {
  return str
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim();
}

function parseContent(text) {
  const lines = text.split(/\r?\n/);
  const data = {
    id: null,
    name: null,
    source: null,
    imageUrl: null,
    ingredients: [],
    recipe: '',
    strengthScore: null,
    tasteScore: null,
    expertRating: null,
    memberRating: null,
    review: '',
    history: '',
    nutrition: '',
    alcoholContent: '',
  };

  const ingredientLines = [];
  let section = '';

  for (const raw of lines) {
    const line = raw.trim();
    if (!line) continue;

    const normalized = normalizeLabel(line.replace(/\s+/g, ' '));
    if (line.startsWith('ID:')) {
      data.id = line.replace('ID:', '').trim();
      continue;
    }
    if (line.startsWith('Nom:')) {
      data.name = line.replace('Nom:', '').trim();
      continue;
    }
    if (line.startsWith('Source:')) {
      data.source = line.replace('Source:', '').trim();
      continue;
    }
    if (line.startsWith('Image:')) {
      data.imageUrl = line.replace('Image:', '').trim();
      continue;
    }

    if (['ingredients:'].includes(normalized)) {
      section = 'ingredients';
      continue;
    }
    if (['recette / preparation:', 'recette/preparation:', 'recette :', 'recette:'].includes(normalized)) {
      section = 'recipe';
      continue;
    }
    if (['profil (force/gout):', 'profil:', 'profil (force/gout)'].includes(normalized)) {
      section = 'profile';
      continue;
    }
    if (['avis:', 'avis'].includes(normalized)) {
      section = 'review';
      continue;
    }
    if (['histoire:', 'histoire'].includes(normalized)) {
      section = 'history';
      continue;
    }
    if (['nutrition:', 'nutrition'].includes(normalized)) {
      section = 'nutrition';
      continue;
    }
    if (['teneur en alcool:', 'alcool:', 'teneur en alcool'].includes(normalized)) {
      section = 'alcohol';
      continue;
    }

    if (section === 'ingredients') {
      if (line.startsWith('-')) {
        ingredientLines.push(line.slice(1).trim());
      }
    } else if (section === 'recipe') {
      data.recipe += (data.recipe ? '\n' : '') + line;
    } else if (section === 'profile') {
      if (normalized.startsWith('score force')) {
        const num = parseFloat(line.split(':')[1]);
        if (!isNaN(num)) data.strengthScore = num;
      } else if (normalized.startsWith('score douceur')) {
        const num = parseFloat(line.split(':')[1]);
        if (!isNaN(num)) data.tasteScore = num;
      } else if (normalized.startsWith('note expert')) {
        const num = parseFloat(line.split(':')[1]);
        if (!isNaN(num)) data.expertRating = num;
      } else if (normalized.startsWith('note membres')) {
        const num = parseFloat(line.split(':')[1]);
        if (!isNaN(num)) data.memberRating = num;
      }
    } else if (section === 'review') {
      data.review += (data.review ? '\n' : '') + line;
    } else if (section === 'history') {
      data.history += (data.history ? '\n' : '') + line;
    } else if (section === 'nutrition') {
      data.nutrition += (data.nutrition ? '\n' : '') + line;
    } else if (section === 'alcohol') {
      data.alcoholContent += (data.alcoholContent ? '\n' : '') + line;
    }
  }

  // Build ingredients as (quantity, name) pairs from alternating lines
  for (let i = 0; i < ingredientLines.length; i += 2) {
    const quantity = ingredientLines[i] || '';
    const name = ingredientLines[i + 1] || '';
    if (quantity || name) {
      data.ingredients.push({ quantity: quantity.trim(), name: name.trim() });
    }
  }
  return data;
}

async function run() {
  const entries = fs.readdirSync(recipesRoot, { withFileTypes: true }).filter((d) => d.isDirectory());
  for (const dir of entries) {
    const id = dir.name;
    const contentPath = path.join(recipesRoot, id, 'content.txt');
    if (!fs.existsSync(contentPath)) continue;
    const text = fs.readFileSync(contentPath, 'utf-8');
    const parsed = parseContent(text);
    const docId = parsed.id || id;
    const payload = {
      name: parsed.name,
      imageUrl: parsed.imageUrl,
      ingredients: parsed.ingredients,
      recipe: parsed.recipe,
      strengthScore: parsed.strengthScore,
      tasteScore: parsed.tasteScore,
      expertRating: parsed.expertRating,
      memberRating: parsed.memberRating,
      review: parsed.review,
      history: parsed.history,
      nutrition: parsed.nutrition,
      alcoholContent: parsed.alcoholContent,
      flavourDescription: parsed.review || '',
    };
    // remove undefined/null-only lists
    if (!payload.ingredients || payload.ingredients.length === 0) delete payload.ingredients;
    Object.keys(payload).forEach((k) => {
      if (payload[k] === null || payload[k] === undefined || payload[k] === '') delete payload[k];
    });

    await db.collection('cocktails').doc(String(docId)).set(payload, { merge: true });
    console.log(`Imported ${docId}: ${parsed.name}`);
  }
  console.log('Import complete');
  process.exit(0);
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});
