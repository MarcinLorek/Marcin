package pl.mlorek.gpwshort.data.remote.parser

/**
 * Rola kolumny w tabeli rejestru KNF, rozpoznawana po treści nagłówka (patrz [HeaderMatcher]),
 * a nie po stałej pozycji – dzięki temu parser przeżywa przestawienie kolejności kolumn.
 */
enum class ColumnRole {
    HOLDER_NAME,
    ISSUER_NAME,
    ISIN,
    PERCENT,
    POSITION_DATE,
}
