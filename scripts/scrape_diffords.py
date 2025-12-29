import argparse
import time
from pathlib import Path

import requests
from bs4 import BeautifulSoup

try:
    from googletrans import Translator
except ImportError:
    Translator = None

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0 Safari/537.36"
)


def translate_text(text: str, translator: "Translator | None") -> str:
    if not text or not translator:
        return text
    try:
        return translator.translate(text, dest="fr").text
    except Exception:
        return text


def extract_section(soup: BeautifulSoup, needle: str) -> str:
    """Grab text after an h2 that contains `needle` until the next h2/h1."""
    for h2 in soup.find_all("h2"):
        if needle in h2.get_text(strip=True).lower():
            parts = []
            sib = h2.find_next_sibling()
            while sib and sib.name not in ["h1", "h2"]:
                txt = sib.get_text(" ", strip=True)
                if txt:
                    parts.append(txt)
                sib = sib.find_next_sibling()
            return "\n".join(parts)
    return ""


def normalize_fraction_html(node) -> str:
    """Convert HTML fractions like <sup>1</sup>&frasl;<sub>2</sub> to 1/2 and strip tags."""
    html = str(node).replace("&frasl;", "/")
    frag = BeautifulSoup(html, "html.parser")
    return frag.get_text(" ", strip=True)


def extract_ingredients(soup: BeautifulSoup) -> list[tuple[str, str]]:
    """Return list of (quantity, name). Annotation tags are appended to quantity."""

    def parse_li(li):
        annotations = [t.get_text(" ", strip=True) for t in li.select(".tag--light")]
        qty_node = li.find(class_=lambda c: c and any(k in c for k in ["measure", "quantity", "amount"]))
        name_node = li.find(class_=lambda c: c and any(k in c for k in ["ingredient", "name"]))
        if qty_node and name_node:
            qty = normalize_fraction_html(qty_node)
            if annotations:
                qty = f"{qty} {' '.join(annotations)}".strip()
            name = name_node.get_text(" ", strip=True)
            return qty, name

        raw = li.get_text(" ", strip=True)
        if annotations:
            raw = raw.replace(" ".join(annotations), "").strip()
        import re
        m = re.match(r"^([0-9\u00bc\u00bd\u00be\u2153\u2154\u215b\u215c\u215d\u215e.,/\s]+[^a-zA-Z]*)\s+(.*)$", raw)
        if m:
            qty = m.group(1).strip()
            name = m.group(2).strip()
        else:
            qty = raw
            name = ""
        if annotations:
            qty = f"{qty} {' '.join(annotations)}".strip()
        return qty, name

    ul = soup.find("ul", class_=lambda c: c and "ingredients" in c)
    if ul:
        return [parse_li(li) for li in ul.find_all("li")]

    blk = soup.find(class_=lambda c: c and "ingredients" in c)
    if blk:
        items = [parse_li(li) for li in blk.find_all("li")]
        if items:
            return items
        text = blk.get_text("\n", strip=True).splitlines()
        cleaned = []
        for line in text:
            if line.lower().startswith("ingredients"):
                continue
            if line.lower().startswith("[edit"):
                continue
            if line:
                cleaned.append((line.strip(), ""))
        return cleaned
    return []


def extract_name(soup: BeautifulSoup) -> str:
    for sel in ["h1.recipe__title", "h1.legacy-strip__heading", "h1"]:
        node = soup.select_one(sel)
        if node:
            return node.get_text(strip=True)
    return ""


def strip_noise(text: str) -> str:
    if not text:
        return ""
    footer = (
        "Diffordƒ?Ts Guide remains free-to-use thanks to the support of the brands in green above . "
        "Values stated for alcohol and calorie content, and number of drinks an ingredient makes should be considered approximate."
    )
    text = text.replace(footer, "")
    lines = []
    for ln in text.splitlines():
        ln = ln.strip()
        if not ln:
            continue
        if ln.startswith("More "):
            continue
        if ln.lower().startswith("view readers"):
            continue
        lines.append(ln)
    return "\n".join(lines)


def scrape_recipe(recipe_id: int, translate: bool) -> dict | None:
    url = f"https://www.diffordsguide.com/cocktails/recipe/{recipe_id}/dummy"
    headers = {"User-Agent": USER_AGENT}
    resp = requests.get(url, headers=headers, timeout=20, allow_redirects=True)
    if resp.status_code == 404:
        print(f"[{recipe_id}] 404 not found")
        return None
    if resp.status_code != 200:
        print(f"[{recipe_id}] HTTP {resp.status_code} at {resp.url}, skipping")
        return None

    final_url = resp.url
    if final_url != url:
        print(f"[{recipe_id}] redirected to {final_url}")

    if "community recipe" in resp.text.lower():
        print(f"[{recipe_id}] skipped community recipe")
        return None

    soup = BeautifulSoup(resp.text, "html.parser")
    name = extract_name(soup)
    if not name:
        print(f"[{recipe_id}] no name found, skipping")
        return None

    ingredients = extract_ingredients(soup)
    howto = extract_section(soup, "how to make")
    strength = extract_section(soup, "strength")
    history = extract_section(soup, "history")
    review = ""
    if "review:" in strength.lower():
        for line in strength.splitlines():
            if line.lower().startswith("review"):
                review = line
                break
    taste = strength

    nutrition = strip_noise(extract_section(soup, "nutrition"))
    alcohol_content = strip_noise(extract_section(soup, "alcohol content"))

    strength_score = None
    taste_score = None
    for h2 in soup.find_all("h2"):
        if "strength" in h2.get_text(strip=True).lower():
            sib = h2.find_next_sibling()
            while sib and sib.name not in ["h1", "h2"]:
                for img in sib.find_all("img"):
                    alt = img.get("alt", "")
                    if "/10" in alt:
                        try:
                            num = float(alt.split("/10")[0].split()[-1])
                        except Exception:
                            num = None
                        if num is not None:
                            if strength_score is None:
                                strength_score = num
                            elif taste_score is None:
                                taste_score = num
                sib = sib.find_next_sibling()
            break

    image_url = ""
    target_alt = f"{name.lower()} image"
    for tag in soup.find_all("img"):
        alt = tag.get("alt", "").lower().strip()
        if alt == target_alt:
            src = tag.get("src") or tag.get("data-src") or ""
            if src.startswith("//"):
                src = "https:" + src
            elif src.startswith("/"):
                src = f"https://www.diffordsguide.com{src}"
            image_url = src
            break

    translator = Translator() if translate and Translator else None

    def tr(text: str) -> str:
        return translate_text(text, translator) if translate else text

    expert_rating = None
    member_rating = None

    def parse_width(style_val: str) -> float | None:
        try:
            for part in style_val.split(";"):
                part = part.strip()
                if part.startswith("width"):
                    pct = part.split(":")[1].replace("%", "").strip()
                    return round(float(pct) / 20, 1)
        except Exception:
            return None
        return None

    for tag in soup.find_all():
        for key, val in tag.attrs.items():
            if not isinstance(val, str):
                continue
            lowkey = key.lower()
            if "rating" in lowkey or "score" in lowkey or "rate" in lowkey:
                try:
                    num = float(val)
                except Exception:
                    continue
                if expert_rating is None:
                    expert_rating = num
                elif member_rating is None:
                    member_rating = num
        if tag.has_attr("style") and ("width" in tag["style"]):
            stars = parse_width(tag["style"])
            if stars:
                if expert_rating is None:
                    expert_rating = stars
                elif member_rating is None:
                    member_rating = stars

    def parse_star_block(block: BeautifulSoup) -> float | None:
        if not block:
            return None
        full = len(block.select(".svg-icon--star"))
        half = len(block.select(".svg-icon--star-half"))
        empty = len(block.select(".svg-icon--star-empty"))
        if full + half + empty == 0:
            return None
        return full + 0.5 * half

    expert_block = soup.select_one(".legacy-rating-with-title .rating")
    member_block = soup.select_one(".legacy-rating-with-title.switch .rating")
    ex_val = parse_star_block(expert_block)
    mem_val = parse_star_block(member_block)
    if ex_val is not None:
        expert_rating = ex_val
    if mem_val is not None:
        member_rating = mem_val

    data = {
        "id": recipe_id,
        "name": name,
        "ingredients": ingredients,
        "howto": tr(howto),
        "strength_taste": "",
        "strength_score": strength_score,
        "taste_score": taste_score,
        "review": tr(review),
        "history": tr(history),
        "nutrition": nutrition,
        "alcohol_content": alcohol_content,
        "image_url": image_url,
        "source_url": final_url,
        "expert_rating": expert_rating,
        "member_rating": member_rating,
    }
    return data


def save_recipe(data: dict, out_root: Path):
    rid = data["id"]
    folder = out_root / str(rid)
    folder.mkdir(parents=True, exist_ok=True)
    content_path = folder / "content.txt"
    lines = [
        f"ID: {rid}",
        f"Nom: {data['name']}",
        f"Source: {data['source_url']}",
        f"Image: {data['image_url']}",
        "",
        "Ingrédients:",
    ]
    for qty, name in data["ingredients"]:
        lines.append(f"- {qty}")
        lines.append(f"- {name}")
    lines += [
        "",
        "Recette / Préparation:",
        data["howto"],
        "",
        "Profil (force/goût):",
        f"Score force (0-10): {data.get('strength_score')}",
        f"Score douceur/acidité (0-10): {data.get('taste_score')}",
        f"Note expert (1-5): {data.get('expert_rating')}",
        f"Note membres (1-5): {data.get('member_rating')}",
        "",
        "Avis:",
        data["review"],
        "",
        "Histoire:",
        data["history"],
        "",
        "Nutrition:",
        data["nutrition"],
        "",
        "Teneur en alcool:",
        data["alcohol_content"],
    ]
    content_path.write_text("\n".join(lines), encoding="utf-8")

    if data["image_url"]:
        try:
            img_resp = requests.get(data["image_url"], headers={"User-Agent": USER_AGENT}, timeout=20)
            if img_resp.ok:
                ext = Path(data["image_url"]).suffix.split("?")[0] or ".jpg"
                (folder / f"image{ext}").write_bytes(img_resp.content)
        except Exception:
            pass


def main():
    parser = argparse.ArgumentParser(description="Scrape Difford's Guide cocktails by recipe ID.")
    parser.add_argument("--start", type=int, default=1, help="Starting recipe ID (inclusive)")
    parser.add_argument("--end", type=int, default=1000, help="Ending recipe ID (inclusive)")
    parser.add_argument("--output", type=Path, default=Path("recipes"), help="Output folder")
    parser.add_argument("--translate", action="store_true", help="Translate text to French (uses googletrans)")
    parser.add_argument("--delay", type=float, default=1.0, help="Delay between requests (seconds)")
    args = parser.parse_args()

    args.output.mkdir(parents=True, exist_ok=True)
    for rid in range(args.start, args.end + 1):
        print(f"=== Processing {rid} ===")
        data = scrape_recipe(rid, translate=args.translate)
        if not data:
            continue
        save_recipe(data, args.output)
        print(f"[{rid}] saved: {data['name']}")
        time.sleep(args.delay)


if __name__ == "__main__":
    main()
