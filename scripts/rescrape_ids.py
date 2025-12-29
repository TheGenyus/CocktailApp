import argparse
import re
import time
from pathlib import Path

from scrape_diffords import scrape_recipe, save_recipe


def load_ids(path: Path) -> list[int]:
    text = path.read_text(encoding="utf-8", errors="ignore") if path.exists() else ""
    ids = set(int(x) for x in re.findall(r"\d+", text))
    return sorted(ids)


def main():
    parser = argparse.ArgumentParser(description="Re-scrape specific recipe IDs from a file")
    parser.add_argument("--file", type=Path, default=Path("scripts/bad_ingredients.txt"), help="File containing IDs")
    parser.add_argument("--output", type=Path, default=Path("recipes"), help="Output folder")
    parser.add_argument("--translate", action="store_true", help="Translate text to French")
    parser.add_argument("--delay", type=float, default=1.0, help="Delay between requests")
    args = parser.parse_args()

    ids = load_ids(args.file)
    if not ids:
        print("No IDs found in", args.file)
        return

    args.output.mkdir(parents=True, exist_ok=True)
    for rid in ids:
        print(f"=== Re-scraping {rid} ===")
        data = scrape_recipe(rid, translate=args.translate)
        if not data:
            continue
        save_recipe(data, args.output)
        print(f"[{rid}] saved: {data['name']}")
        time.sleep(args.delay)


if __name__ == "__main__":
    main()
