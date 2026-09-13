# GPW Short – krótkie pozycje netto na GPW (Android)

Aplikacja Android (Kotlin + Jetpack Compose) pokazująca aktualne krótkie
pozycje netto na spółkach z GPW oraz historię ich zmian, na podstawie
Rejestru Krótkiej Sprzedaży KNF.

Pełny opis architektury, ryzyk i decyzji projektowych – patrz odpowiedź, w
której powstał ten projekt, oraz komentarze w kodzie (w szczególności
`KnfHtmlParser.kt`, `KnfXlsxParser.kt`, `ui/components/PositionHistoryChart.kt`).

## Struktura repo

- `app/` – aplikacja Android (Kotlin, Compose, Hilt, Room, WorkManager).
- `backend/` – **opcjonalny** skrypt Pythona + workflow GitHub Actions, który
  może budować dzienne snapshoty rejestru KNF jako statyczny JSON (patrz
  `backend/README.md`). Aplikacja Android go nie wymaga.
- `.github/workflows/fetch-knf-data.yml` – harmonogram dla powyższego skryptu.

## Uruchomienie

1. Otwórz katalog repo w Android Studio (Koala/Ladybug lub nowszym,
   wspierającym Kotlin 2.0 / AGP 8.7) – "Open" → wskaż ten folder.
   Android Studio samo wygeneruje `gradlew`/`gradle-wrapper.jar`, jeśli
   ich brakuje (Gradle Wrapper properties są już w repo).
2. Poczekaj na Gradle sync (pobierze zależności z `gradle/libs.versions.toml`).
3. Uruchom konfigurację `app` na emulatorze z API 26+ (Android 8.0+) lub
   fizycznym urządzeniu.
4. Przy pierwszym uruchomieniu aplikacja od razu spróbuje pobrać dane z
   `https://rss.knf.gov.pl/rss_pub/rssH.html` – wymagany dostęp do internetu.

### Build z linii poleceń (gdy masz zainstalowany Android SDK)

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Ważne zastrzeżenie

Środowisko, w którym powstał ten kod, miało zablokowany dostęp sieciowy do
`knf.gov.pl`, więc **parser HTML/XLSX nie mógł zostać zweryfikowany na
żywo** względem aktualnej struktury strony KNF. Przed pierwszym
uruchomieniem produkcyjnym koniecznie sprawdź ręcznie punkty z sekcji
"Do ręcznej weryfikacji" w opisie zadania / commit message.
