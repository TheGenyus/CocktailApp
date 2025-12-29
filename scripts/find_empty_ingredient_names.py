import json
from pathlib import Path

# Chemin du JSON (modifie si besoin)
json_path = Path('public/cocktails.json')
if not json_path.exists():
    json_path = Path('cocktails.json')

with json_path.open('r', encoding='utf-8') as f:
    data = json.load(f)

matches = []
for item in data:
    ing_list = item.get('ingredients') or []
    for ing in ing_list:
        name = (ing.get('name') or '').strip()
        if name == '':
            matches.append((str(item.get('id')), item.get('name', '(sans nom)')))
            break

out = '\n'.join(f"{i} | {n}" for i, n in matches)
out_path = Path('scripts/empty_ingredient_names.txt')
out_path.write_text(out, encoding='utf-8')
print(f"Trouvé {len(matches)} recettes avec un nom d'ingrédient vide. Résultat dans {out_path}")
