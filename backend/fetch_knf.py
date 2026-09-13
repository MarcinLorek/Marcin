#!/usr/bin/env python3
"""
Pobiera rejestr krotkiej sprzedazy KNF (https://rss.knf.gov.pl/rss_pub/rssH.html),
parsuje tabele "znaczace pozycje krotkie netto" i "sumaryczne pozycje krotkie netto"
i zapisuje znormalizowany JSON: biezacy stan (docs/data/latest.json) oraz snapshot
danego dnia w historii (docs/data/history/YYYY-MM-DD.json).

Logika rozpoznawania kolumn (po slowach kluczowych w naglowku, nie po stalej
pozycji) jest celowo analogiczna do KnfHtmlParser/HeaderMatcher w aplikacji
Android - patrz backend/README.md.
"""
from __future__ import annotations

import datetime
import json
import os
import re
import time
import unicodedata

import requests
from bs4 import BeautifulSoup

REGISTER_URL = "https://rss.knf.gov.pl/rss_pub/rssH.html"

ROLE_KEYWORDS = {
    "isin": ["isin"],
    "percent": ["proc", "%", "udzia"],
    "date": ["data"],
    "holder": ["podmiot", "posiadacz", "fundusz", "oglasza"],
    "issuer": ["emitent", "spolk", "nazwa waloru", "papier"],
}

DATE_FORMATS = ["%Y-%m-%d", "%d-%m-%Y", "%d.%m.%Y", "%d/%m/%Y"]


def normalize(text: str) -> str:
    decomposed = unicodedata.normalize("NFD", text.strip().lower())
    return "".join(ch for ch in decomposed if not unicodedata.combining(ch))


def match_role(header_text: str) -> str | None:
    normalized = normalize(header_text)
    for role, keywords in ROLE_KEYWORDS.items():
        if any(keyword in normalized for keyword in keywords):
            return role
    return None


def map_header(header_cells: list[str]) -> dict[str, int]:
    mapping: dict[str, int] = {}
    for index, cell in enumerate(header_cells):
        role = match_role(cell)
        if role and role not in mapping:
            mapping[role] = index
    return mapping


def parse_percent(raw: str) -> float | None:
    cleaned = raw.strip().replace("%", "").replace(" ", "").replace(" ", "").replace(",", ".")
    try:
        return float(cleaned)
    except ValueError:
        return None


def parse_date(raw: str) -> datetime.date | None:
    raw = raw.strip()
    for fmt in DATE_FORMATS:
        try:
            return datetime.datetime.strptime(raw, fmt).date()
        except ValueError:
            continue
    return None


def clean_text(raw: str) -> str:
    return re.sub(r"\s+", " ", raw.strip())


def clean_isin(raw: str | None) -> str | None:
    if raw is None:
        return None
    cleaned = clean_text(raw)
    return cleaned if cleaned and cleaned != "-" else None


def interpret_table(header: list[str], rows: list[list[str]]):
    mapping = map_header(header)
    if "percent" not in mapping or "date" not in mapping or "issuer" not in mapping:
        return None, []

    percent_col = mapping["percent"]
    date_col = mapping["date"]
    issuer_col = mapping["issuer"]
    isin_col = mapping.get("isin")
    holder_col = mapping.get("holder")

    def cell(row: list[str], idx: int | None) -> str | None:
        if idx is None or idx >= len(row):
            return None
        return row[idx]

    parsed_rows = []
    for row in rows:
        issuer = clean_text(cell(row, issuer_col) or "")
        percent = parse_percent(cell(row, percent_col) or "")
        date = parse_date(cell(row, date_col) or "")
        if not issuer or percent is None or date is None:
            continue

        entry = {
            "issuerName": issuer,
            "isin": clean_isin(cell(row, isin_col)),
            "percent": percent,
            "positionDate": date.isoformat(),
        }

        if holder_col is not None:
            holder = clean_text(cell(row, holder_col) or "")
            if not holder:
                continue
            entry["holderName"] = holder

        parsed_rows.append(entry)

    kind = "significant" if holder_col is not None else "summary"
    return kind, parsed_rows


def fetch_html(url: str, retries: int = 3, backoff_seconds: float = 1.0) -> str:
    last_error: Exception | None = None
    for attempt in range(retries):
        try:
            response = requests.get(url, timeout=30, headers={"User-Agent": "gpw-short-tracker/1.0"})
            response.raise_for_status()
            return response.text
        except requests.RequestException as exc:  # noqa: PERF203 - retry z narastajacym opoznieniem
            last_error = exc
            if attempt < retries - 1:
                time.sleep(backoff_seconds * (2**attempt))
    raise RuntimeError(f"Nie udalo sie pobrac {url}: {last_error}")


def parse_register(html: str):
    soup = BeautifulSoup(html, "lxml")
    significant: list[dict] = []
    summary: list[dict] = []

    for table in soup.find_all("table"):
        rows = table.find_all("tr")
        if not rows:
            continue
        header_cells = [c.get_text(strip=True) for c in rows[0].find_all(["th", "td"])]
        data_rows = [
            [c.get_text(strip=True) for c in r.find_all(["td", "th"])]
            for r in rows[1:]
        ]
        data_rows = [r for r in data_rows if r]
        if not header_cells or not data_rows:
            continue

        kind, parsed = interpret_table(header_cells, data_rows)
        if kind == "significant":
            significant.extend(parsed)
        elif kind == "summary":
            summary.extend(parsed)

    return significant, summary


def build_snapshot(significant: list[dict], summary: list[dict]) -> dict:
    by_issuer_key: dict[str, list[dict]] = {}
    for row in significant:
        key = row["isin"] or row["issuerName"].strip().lower()
        by_issuer_key.setdefault(key, []).append(row)

    issuers = []
    handled_keys = set()

    for row in summary:
        key = row["isin"] or row["issuerName"].strip().lower()
        handled_keys.add(key)
        holders = by_issuer_key.get(key, [])
        issuers.append(_issuer_entry(row["issuerName"], row["isin"], row["percent"], row["positionDate"], holders))

    for key, holders in by_issuer_key.items():
        if key in handled_keys or not holders:
            continue
        first = holders[0]
        total = round(sum(h["percent"] for h in holders), 4)
        issuers.append(_issuer_entry(first["issuerName"], first["isin"], total, first["positionDate"], holders))

    publication_date = max(
        (issuer["positionDate"] for issuer in issuers),
        default=datetime.date.today().isoformat(),
    )

    return {
        "publicationDate": publication_date,
        "generatedAt": datetime.datetime.utcnow().isoformat() + "Z",
        "issuers": sorted(issuers, key=lambda issuer: -issuer["totalShortPercent"]),
    }


def _issuer_entry(name: str, isin: str | None, percent: float, position_date: str, holders: list[dict]) -> dict:
    return {
        "name": name,
        "isin": isin,
        "totalShortPercent": percent,
        "positionDate": position_date,
        "holderCount": len({h["holderName"] for h in holders}),
        "holders": [
            {"name": h["holderName"], "percent": h["percent"], "positionDate": h["positionDate"]}
            for h in holders
        ],
    }


def main() -> None:
    html = fetch_html(REGISTER_URL)
    significant, summary = parse_register(html)

    if not significant and not summary:
        raise RuntimeError(
            "Zadna tabela nie pasuje do znanego ukladu kolumn - struktura strony KNF mogla sie zmienic",
        )

    snapshot = build_snapshot(significant, summary)

    out_dir = os.path.join(os.path.dirname(__file__), "..", "docs", "data")
    history_dir = os.path.join(out_dir, "history")
    os.makedirs(history_dir, exist_ok=True)

    with open(os.path.join(out_dir, "latest.json"), "w", encoding="utf-8") as handle:
        json.dump(snapshot, handle, ensure_ascii=False, indent=2)

    history_path = os.path.join(history_dir, f"{snapshot['publicationDate']}.json")
    with open(history_path, "w", encoding="utf-8") as handle:
        json.dump(snapshot, handle, ensure_ascii=False, indent=2)

    print(f"Zapisano snapshot z {snapshot['publicationDate']}: {len(snapshot['issuers'])} emitentow")


if __name__ == "__main__":
    main()
