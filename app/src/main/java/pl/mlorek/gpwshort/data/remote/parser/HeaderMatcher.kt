package pl.mlorek.gpwshort.data.remote.parser

import java.text.Normalizer

/**
 * Dopasowuje nagłówki tabel KNF (po polskich słowach kluczowych, bez wielkości liter
 * i znaków diakrytycznych) do [ColumnRole]. Utrzymywany osobno od parserów HTML/XLSX,
 * żeby ewentualną zmianę nazewnictwa kolumn na stronie KNF poprawić w jednym miejscu.
 */
object HeaderMatcher {

    private val isinKeywords = listOf("isin")
    private val percentKeywords = listOf("proc", "%", "udzia")
    private val dateKeywords = listOf("data")
    private val holderKeywords = listOf("podmiot", "posiadacz", "fundusz", "oglasza")
    private val issuerKeywords = listOf("emitent", "spolk", "nazwa waloru", "papier")

    fun matchRole(headerText: String): ColumnRole? {
        val normalized = normalize(headerText)
        return when {
            isinKeywords.any { normalized.contains(it) } -> ColumnRole.ISIN
            percentKeywords.any { normalized.contains(it) } -> ColumnRole.PERCENT
            dateKeywords.any { normalized.contains(it) } -> ColumnRole.POSITION_DATE
            holderKeywords.any { normalized.contains(it) } -> ColumnRole.HOLDER_NAME
            issuerKeywords.any { normalized.contains(it) } -> ColumnRole.ISSUER_NAME
            else -> null
        }
    }

    /** Mapuje wiersz nagłówka na indeksy kolumn; brak wpisu oznacza brak danej roli w tabeli. */
    fun mapHeader(headerCells: List<String>): Map<ColumnRole, Int> {
        val result = mutableMapOf<ColumnRole, Int>()
        headerCells.forEachIndexed { index, cell ->
            val role = matchRole(cell) ?: return@forEachIndexed
            // Pierwsze trafienie wygrywa - kolumny KNF nie powtarzają tej samej roli.
            result.putIfAbsent(role, index)
        }
        return result
    }

    private fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text.lowercase().trim(), Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{M}"), "")
    }
}
