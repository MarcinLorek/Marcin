package pl.mlorek.gpwshort.data.remote.parser

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Wspólne, tolerancyjne parsowanie wartości liczbowych/dat spotykanych w publikacjach KNF. */
object KnfValueParsing {

    private val dateFormatters = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
    )

    fun parsePercent(raw: String): Double? {
        val cleaned = raw
            .trim()
            .replace("%", "")
            .replace(" ", "")
            .replace(" ", "")
            .replace(",", ".")
        return cleaned.toDoubleOrNull()
    }

    fun parseDate(raw: String): LocalDate? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        for (formatter in dateFormatters) {
            try {
                return LocalDate.parse(trimmed, formatter)
            } catch (_: DateTimeParseException) {
                // próbuj kolejnego formatu
            }
        }
        return null
    }

    fun cleanText(raw: String): String = raw.trim().replace(Regex("\\s+"), " ")

    fun cleanIsin(raw: String?): String? {
        val cleaned = raw?.let { cleanText(it) }
        return cleaned?.takeIf { it.isNotBlank() && !it.equals("-", ignoreCase = true) }
    }
}
