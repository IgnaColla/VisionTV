import requests
import json
import os
import time

# Configuration
ARCHIVE_SEARCH_URL = "https://archive.org/advancedsearch.php"
TMDB_SEARCH_MOVIE_URL = "https://api.themoviedb.org/3/search/movie"
TMDB_SEARCH_TV_URL = "https://api.themoviedb.org/3/search/tv"
CATALOG_PATH = "catalog/public_domain.json"
BLOCKLIST_PATH = "scripts/blocklist.txt"

# Collections to search
COLLECTIONS = ["feature_films", "classic_tv", "prelinger", "sci_fi_movies", "silent_films"]
MAX_ITEMS_PER_COLLECTION = 20

def get_blocklist():
    if not os.path.exists(BLOCKLIST_PATH):
        return set()
    with open(BLOCKLIST_PATH, "r") as f:
        return {line.strip() for line in f if line.strip() and not line.startswith("#")}

def search_archive():
    query = f"mediatype:movies AND ({' OR '.join([f'collection:{c}' for c in COLLECTIONS])}) AND licenseurl:*publicdomain*"
    params = {
        "q": query,
        "fl": "identifier,title,year,mediatype,collection",
        "sort[]": "downloads desc",
        "rows": MAX_ITEMS_PER_COLLECTION * len(COLLECTIONS),
        "output": "json"
    }

    response = requests.get(ARCHIVE_SEARCH_URL, params=params)
    response.raise_for_status()
    return response.json().get("response", {}).get("docs", [])

def find_tmdb_id(title, year, mediatype, api_key):
    if not api_key:
        return None

    url = TMDB_SEARCH_MOVIE_URL if mediatype == "movies" else TMDB_SEARCH_TV_URL
    headers = {"Authorization": f"Bearer {api_key}"} if api_key.startswith("eyJ") else {}
    params = {"query": title}
    if not api_key.startswith("eyJ"):
        params["api_key"] = api_key

    try:
        res = requests.get(url, params=params, headers=headers)
        res.raise_for_status()
        results = res.json().get("results", [])

        if not results:
            return None

        # Simple matching: try to find one with the same year, or just take the first
        for r in results:
            res_year = r.get("release_date", r.get("first_air_date", ""))[:4]
            if str(year) == res_year:
                return r["id"]

        return results[0]["id"]
    except Exception as e:
        print(f"TMDB search failed for {title}: {e}")
        return None

def main():
    tmdb_key = os.environ.get("TMDB_TOKEN")
    blocklist = get_blocklist()

    print("Searching Archive.org...")
    items = search_archive()
    print(f"Found {len(items)} potential items.")

    catalog_items = []
    seen_ids = set()

    for entry in items:
        identifier = entry["identifier"]
        if identifier in blocklist or identifier in seen_ids:
            continue

        title = entry.get("title", identifier)
        year = entry.get("year")
        # Archive.org mediatype 'movies' maps to both our movies and series
        # We'll use collection as a hint or just default to movie
        item_type = "series" if "classic_tv" in entry.get("collection", []) else "movie"

        print(f"Processing: {title} ({year})...")
        tmdb_id = find_tmdb_id(title, year, "movies" if item_type == "movie" else "tv", tmdb_key)

        if tmdb_id:
            catalog_items.append({
                "title": title,
                "type": item_type,
                "archive_org_id": identifier,
                "year": int(year) if year and str(year).isdigit() else None,
                "tmdb_id": tmdb_id
            })
            seen_ids.add(identifier)
            time.sleep(0.2) # Respect TMDB rate limits
        else:
            print(f"Skipping {title}: Could not validate on TMDB.")

    catalog = {
        "catalog_version": int(time.time()),
        "items": catalog_items
    }

    with open(CATALOG_PATH, "w") as f:
        json.dump(catalog, f, indent=2)

    print(f"Done! Catalog updated with {len(catalog_items)} items.")

if __name__ == "__main__":
    main()
