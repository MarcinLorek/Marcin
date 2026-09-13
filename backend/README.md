# Opcjonalny backend: codzienny snapshot rejestru KNF jako JSON

Ten katalog **nie jest wymagany**, żeby aplikacja Android działała – domyślnie
aplikacja pobiera i parsuje `https://rss.knf.gov.pl/rss_pub/rssH.html`
bezpośrednio na urządzeniu (`KnfHtmlParser` / `KnfXlsxParser`).

## Po co w ogóle backend, skoro apka działa bez niego?

Rozważ to jako ulepszenie na przyszłość, nie wymóg startowy:

1. **Odporność na zmianę strony KNF w jednym miejscu.** Jeśli KNF przebuduje
   HTML, wystarczy zaktualizować jeden skrypt Pythona i wypchnąć commit –
   zamiast wymuszać aktualizację apki na wszystkich telefonach użytkowników
   (Google Play review, wolniejsza propagacja).
2. **Mniejsze zużycie danych/baterii na telefonie.** Parsowanie HTML/XLSX
   (Jsoup, SAX) na starszych/słabszych telefonach to niepotrzebny koszt,
   jeśli można to zrobić raz dziennie na serwerze i rozesłać gotowy,
   maleńki JSON.
3. **Historia niezależna od cyklu życia aplikacji.** Nawet jeśli użytkownik
   odinstaluje appkę na miesiąc, backend i tak zbiera codzienne snapshoty –
   po ponownej instalacji można od razu zaimportować pełną historię
   zamiast zaczynać od zera (obecnie w apce historia zaczyna się od
   pierwszego uruchomienia na danym urządzeniu, bo to jedyne dostępne źródło).
4. **Jedno źródło prawdy dla wielu klientów** (Android, ewentualnie iOS/web
   w przyszłości) zamiast duplikowania logiki parsowania w każdym z nich.

## Jak to działa

- `fetch_knf.py` – pobiera `rss_pub/rssH.html`, parsuje te same dwie tabele
  (znaczące pozycje ≥0,5% i sumaryczne ≥0,1%) co `KnfHtmlParser` w aplikacji
  (ta sama, header-based logika dopasowania kolumn – żeby ewentualna zmiana
  strony była naprawiana w analogiczny sposób w obu miejscach), i zapisuje:
  - `docs/data/latest.json` – bieżący stan (nadpisywany),
  - `docs/data/history/YYYY-MM-DD.json` – snapshot danego dnia (dopisywany,
    nigdy nie nadpisywany, więc historia rośnie z czasem).
- `.github/workflows/fetch-knf-data.yml` – uruchamia skrypt w GitHub Actions
  codziennie w dni robocze po sesji GPW i commituje zmiany do repo. Jeśli
  w tym samym repo włączysz GitHub Pages z katalogu `docs/`, `latest.json`
  będzie dostępny pod publicznym URL-em, który Android mógłby odpytywać
  Retrofitem zamiast (albo obok) parsowania KNF bezpośrednio.
- **Ryzyko/ograniczenie**: podobnie jak `KnfHtmlParser`, ten skrypt nie mógł
  zostać zweryfikowany na żywo (domena `knf.gov.pl` była zablokowana w
  środowisku, w którym powstał ten kod) – patrz komentarz w `KnfHtmlParser.kt`.
  Struktura HTML/kolumn jest rozpoznawana po słowach kluczowych w nagłówkach,
  więc drobne zmiany układu strony nie powinny go wywrócić, ale wymaga to
  potwierdzenia na żywo przed wdrożeniem produkcyjnym.

## Uruchomienie lokalne

```bash
cd backend
pip install -r requirements.txt
python fetch_knf.py
```
